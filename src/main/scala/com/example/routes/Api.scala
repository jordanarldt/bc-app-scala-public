package com.example.routes

import com.example.ApiSchema._
import com.example.controllers._
import com.example.lib.{BigCommerce, Database, BcAuthBypass, BcVerifiedUser, BcUser}

import scala.util.{Success, Failure}
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{StatusCodes, StatusCode, HttpResponse, HttpEntity, ContentType, MediaTypes}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{Route, Directive0}
import akka.http.scaladsl.server.Directives._

object JsonError {
  def apply(status: StatusCode, error: String) = 
    HttpResponse(status, entity = HttpEntity(ContentType(MediaTypes.`application/json`), s"""{"message":"$error"}"""))
}

object JsonResponse {
  def apply(data: String) = 
    HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), data))
}

class Api(
  implicit system: ActorSystem[_], 
  db: Database, 
  bigCommerce: BigCommerce,
  bcClientBypass: Option[BcAuthBypass] = None
) {
  import system.executionContext

  // Import JSON support marshalling
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.example.JsonFormats._
  import spray.json._

  // Allow CORS requests unless in production
  // this is for easier development in the React client
  private val corsMode = sys.env.getOrElse("MODE", "DEV")

  private val corsHeaders = if (corsMode == "DEV") List(
    RawHeader("Access-Control-Allow-Origin", "*"),
    RawHeader("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS, DELETE"),
    RawHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization")
  ) else List.empty

  /** 
   * This method automatically validates the context query param
   * and also provide the BC Client for the store making the request.
   * The route provides a curried function callback/control structure containing the
   * BigCommerce credentials for the store that made the request,
   * as well as the HTTP Method for the request route - but this is only
   * needed if a route supports multiple methods.
   * 
   * @param prefix the path to be matched by the route
   * @param methods the HTTP methods supported by the route
   * @param allowNested determines if the route will allow additional paths/children - defaults to `false`
   * 
   * @example ```
   * val route = apiRoute("endpoint", List("GET", "POST")) { (bcClient, method) =>
   *   method match {
   *     case "GET" =>
   *       // do GET stuff
   *       complete("Get request received")
   *     case "POST" =>
   *       // do POST stuff
   *       complete("Post request received")
   *   }
   * }
   * ```
   */
  private def apiRoute(
    prefix: String,
    methods: List[String],
    allowNested: Boolean = false
  )(routeLogic: (BigCommerce, BcVerifiedUser, String) => Route): Route = {

    val rootDirective = if (allowNested) {
      pathPrefix(prefix)
    } else {
      (pathPrefix(prefix) & pathEndOrSingleSlash)
    }

    val methodsDirective = methods.map { 
      _.toLowerCase match {
        case "get" => get
        case "post" => post
        case "put" => put
        case "delete" => delete
        case _ =>
          throw new Exception(s"Invalid method on $prefix route")
      }
    }.reduce(_ | _)

    rootDirective {
      (methodsDirective & extractMethod) { method =>
        if (bcClientBypass.isEmpty) {
          // If bypass-auth is not enabled, then we need to validate the context
          parameter("context") { 
            bigCommerce.decodeJwt(_) match {
              case None => 
                complete(JsonError(StatusCodes.Unauthorized, "Unable to validate request context"))
              case Some(verified) =>
                val storeHash = verified.context.split("/").last
    
                onSuccess(db.getStoreToken(storeHash)) { 
                  case Some(accessToken) => 
                    val bcClient = new BigCommerce(
                      storeHash = Some(storeHash), 
                      accessToken = Some(accessToken)
                    )
        
                    routeLogic(bcClient, verified, method.value)
                  case None => 
                    complete(JsonError(StatusCodes.Unauthorized, "Could not find store credentials. Please try reinstalling the app."))
                }
            }
          } ~ 
          complete(JsonError(StatusCodes.Unauthorized, "Missing authorization parameter"))
        } else {
          // If bypass-auth is enabled, create a dummy user
          val bcClient = new BigCommerce(
            storeHash = Some(bcClientBypass.get.storeHash),
            accessToken = Some(bcClientBypass.get.accessToken)
          )

          val timestamp = (System.currentTimeMillis() / 1000).toInt
          val bypassUser = BcUser(bcClientBypass.get.userId, "bypassuser")
          val bypassPayload = BcVerifiedUser(bypassUser, bypassUser, s"stores/${bcClientBypass.get.storeHash}", timestamp)

          routeLogic(bcClient, bypassPayload, method.value)
        }
      } ~ complete(StatusCodes.MethodNotAllowed)
    }
  }

  // Declare all API endpoints here
  val routes: Route = pathPrefix("api") {
    respondWithHeaders(corsHeaders) {
      concat(
        // Add OPTIONS directive to ensure POST/PUT/DELETE requests pass pre-flight while in development
        options {
          if (corsMode == "DEV") complete(StatusCodes.OK) else complete(StatusCodes.MethodNotAllowed)
        },
        apiRoute("variants", List("GET", "PUT"), allowNested = true) { 
          (bcClient, userPayload, method) => VariantsController(bcClient, userPayload, method) 
        },
        apiRoute("users", List("GET", "PUT"), allowNested = true) {
          (_, userPayload, method) => UsersController(userPayload, method)
        },
        // Any unmatched routes with the api prefix should route to a 404
        complete(StatusCodes.NotFound)
      )
    }
  }
}
