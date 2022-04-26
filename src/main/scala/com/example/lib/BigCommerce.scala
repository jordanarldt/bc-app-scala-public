package com.example.lib

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, MediaTypes, ContentType, ContentTypes}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal

import java.net.URLEncoder

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

final case class BcUser(id: Int, email: String)
final case class BcAuthResponse(
  access_token: String,
  scope: String,
  user: BcUser,
  context: String,
  account_uuid: String
)

final case class BcVerifiedUser(
  user: BcUser,
  owner: BcUser,
  context: String,
  timestamp: Int,
)

final case class BcJwtPayload(
  aud: String,
  jti: Option[String],
  sub: String,
  user: BcUser,
  owner: BcUser,
  url: String
)

final case class BcAuthBypass(accessToken: String, storeHash: String, userId: Int)

// BigCommerce API Client
class BigCommerce(
  val storeHash: Option[String] = None, // use val to allow the hash to be an accessible property
  accessToken: Option[String] = None,
  clientId: Option[String] = None,
  secret: Option[String] = None,
  callback: Option[String] = None
)(implicit system: ActorSystem[_]) {
  
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import spray.json._
  import spray.json.DefaultJsonProtocol._
  import com.example.JsonFormats._
  import system.executionContext

  private implicit val clock = java.time.Clock.systemUTC

  def generateAccessToken(
    code: String, 
    context: String, 
    scope: String
  ): Future[Option[BcAuthResponse]] = {
    if (clientId.isDefined && secret.isDefined && callback.isDefined) {
      val payload = Map(
        "client_id" -> clientId.get,
        "client_secret" -> secret.get,
        "redirect_uri" -> callback.get,
        "grant_type" -> "authorization_code",
        "code" -> code,
        "scope" -> URLEncoder.encode(scope, "UTF-8"),
        "context" -> URLEncoder.encode(context, "UTF-8")
      )

      val entity = HttpEntity(ContentType(MediaTypes.`application/json`), payload.toJson.toString)
      val request = Post("https://login.bigcommerce.com/oauth2/token", entity)
      val resFut = Http().singleRequest(request)

      resFut.flatMap { res => 
        // Try to parse the string to BcAuthResponse class, otherwise return nothing in the Option
        // it is best practice in Scala to not throw an Exception unless it's a critical error.
        // If the jsonString can't convert to BcAuthResponse, it will return None instead of a Throwable,
        // and the None value will be handled by the install endpoint as an Unauthorized error.
        Unmarshal(res).to[String].map { jsonString => 
          Try(jsonString.parseJson.convertTo[BcAuthResponse]) match {
            case Failure(e) => 
              println(s"BigCommerce Install request rejected - $jsonString")
              None
            case Success(value) =>
              Some(value)
          }
        }
      }
    } else {
      throw new Exception("Cannot create a token unless clientId, secret, and callback are set")
    }
  }

  // Verify requests to ensure they are coming from BigCommerce
  def verifyPayload(signedPayloadJwt: String): Option[BcVerifiedUser] = {
    if (secret.isDefined) {
      Jwt.decode(signedPayloadJwt, secret.get, Seq(JwtAlgorithm.HS256)) match {
        case Failure(e) => 
          None
        case Success(claim) => 
          val payload = claim.content.parseJson.convertTo[BcJwtPayload]
          Some(BcVerifiedUser(payload.user, payload.owner, payload.sub, (System.currentTimeMillis / 1000).toInt))
      }
    } else {
      throw new Exception("Cannot verify BigCommerce payload without secret")
    }
  }

  def encodeJwt(payload: BcVerifiedUser): String = {
    if (secret.isDefined) {
      Jwt.encode(JwtClaim(payload.toJson.toString).issuedNow.expiresIn(24 * 60 * 60), secret.get, JwtAlgorithm.HS256)
    } else {
      throw new Exception("Cannot create JWT unless the client secret is set")
    }
  }

  def decodeJwt(context: String): Option[BcVerifiedUser] = {
    if (secret.isDefined) {
      Jwt.decode(context, secret.get, Seq(JwtAlgorithm.HS256)) match {
        case Failure(e) => 
          println(s"Could not decode request context: ${e.getMessage}")
          None
        case Success(claim) =>
          Some(claim.content.parseJson.convertTo[BcVerifiedUser])
      }
    } else {
      throw new Exception("Cannot create JWT unless the client secret is set")
    }
  }

  // Mimic a signed_payload_jwt for route testing
  def mimicSignedPayloadJwt(user: BcUser, context: String, url: String = "/"): String = {
    if (clientId.isDefined && secret.isDefined) {
      val payload = BcJwtPayload(clientId.get, None, context, user, user, url)
  
      Jwt.encode(
        JwtClaim(payload.toJson.toString).issuedNow.expiresIn(24 * 60 * 60), 
        secret.get, 
        JwtAlgorithm.HS256
      )
    } else {
      throw new Exception("Cannot create a signed payload JWT unless clientId and secret are set")
    }
  }
  
  /**
    * Make a GET to a BigCommerce resource
    *
    * @param uri
    * @return Future[String]
    */
  def get(uri: String): Future[String] = {
    if (storeHash.isDefined && accessToken.isDefined) {
      val headers = List(
        RawHeader("X-Auth-Token", accessToken.get),
        RawHeader("Accept", "application/json")
      )

      val url = s"https://api.bigcommerce.com/stores/${storeHash.get}/$uri"
      val request = Get(url).withHeaders(headers)
      val resFut = Http().singleRequest(request)

      // Respond with JSON as a String to allow for manual type conversion later
      resFut.flatMap { res => 
        Unmarshal(res).to[String].flatMap { json =>
          if (res.status.isSuccess) {
            Future.successful(json)
          } else {
            println(s"Server error on GET $url: $json")
            Future.failed(new Exception(json))
          } 
        }
      }
    } else {
      throw new Exception("Cannot make a GET request if storeHash and accessToken are not set.")
    }
  }

  /**
    * Make a POST to a BigCommerce resource
    *
    * @param uri
    * @param body - Json string of the request body
    */
  def post(uri: String, body: String): Future[String] = {
    if (storeHash.isDefined && accessToken.isDefined) {
      val headers = List(
        RawHeader("X-Auth-Token", accessToken.get),
        RawHeader("Accept", "application/json")
      )

      val url = s"https://api.bigcommerce.com/stores/${storeHash.get}/$uri"
      val request = Post(url, HttpEntity(ContentTypes.`application/json`, body)).withHeaders(headers)
      val resFut = Http().singleRequest(request)

      // Respond with JSON as a String to allow for manual type conversion later
      resFut.flatMap { res => 
        Unmarshal(res).to[String].flatMap { json =>
          if (res.status.isSuccess) {
            Future.successful(json)
          } else {
            Future.failed(new Exception(s"Server error on POST $url: $json"))
          } 
        }
      }
    } else {
      throw new Exception("Cannot make a POST request if storeHash and accessToken are not set.")
    }
  }

  /**
    * Make a PUT to a BigCommerce resource
    *
    * @param uri
    * @param body - Json string of the request body
    */
  def put(uri: String, body: String): Future[String] = {
    if (storeHash.isDefined && accessToken.isDefined) {
      val headers = List(
        RawHeader("X-Auth-Token", accessToken.get),
        RawHeader("Accept", "application/json")
      )

      val url = s"https://api.bigcommerce.com/stores/${storeHash.get}/$uri"
      val request = Put(url, HttpEntity(ContentTypes.`application/json`, body)).withHeaders(headers)
      val resFut = Http().singleRequest(request)

      // Respond with JSON as a String to allow for manual type conversion later
      resFut.flatMap { res => 
        Unmarshal(res).to[String].flatMap { json =>
          if (res.status.isSuccess) {
            Future.successful(json)
          } else {
            Future.failed(new Exception(s"Server error on PUT $url: $json"))
          } 
        }
      }
    } else {
      throw new Exception("Cannot make a PUT request if storeHash and accessToken are not set.")
    }
  }

  /**
    * Make a DELETE to a BigCommerce resource
    *
    * @param uri
    * @return Future[String]
    */
  def delete(uri: String): Future[String] = {
    if (storeHash.isDefined && accessToken.isDefined) {
      val headers = List(
        RawHeader("X-Auth-Token", accessToken.get),
        RawHeader("Accept", "application/json")
      )

      val url = s"https://api.bigcommerce.com/stores/${storeHash.get}/$uri"
      val request = Delete(url).withHeaders(headers)
      val resFut = Http().singleRequest(request)

      // Respond with JSON as a String to allow for manual type conversion later
      resFut.flatMap { res => 
        Unmarshal(res).to[String].flatMap { json =>
          if (res.status.isSuccess) {
            Future.successful(json)
          } else {
            Future.failed(new Exception(s"Server error on DELETE $url: $json"))
          } 
        }
      }
    } else {
      throw new Exception("Cannot make a DELETE request if storeHash and accessToken are not set.")
    }
  }
}
