package com.example.routes

import com.example.lib.{BcVerifiedUser, BigCommerce, Database}
import com.example.UserRoles._

import scala.util.{Failure, Success}
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

// Routes for BigCommerce App events (install, uninstall, users, load)
class Auth(
  implicit system: ActorSystem[_], 
  db: Database,
  bigCommerce: BigCommerce
) {
  import system.executionContext

  // Route directive to handle payload verification to avoid code repetition
  private def withPayloadAuth(cb: BcVerifiedUser => Route): Route = {
    parameter("signed_payload_jwt") {
      bigCommerce.verifyPayload(_) match {
        case Some(user) => cb(user)
        case None =>
          complete(StatusCodes.Unauthorized, "Unable to authorize request. Please try again.")
      }
    } ~ complete(StatusCodes.BadRequest, "Invalid request. Missing required query parameters.")
  }

  // Declare the routes
  val routes: Route = (pathPrefix("auth") & get) {
    concat(
      // /install endpoint
      pathPrefix("install") {
        parameters("code", "context", "scope") { 
          (code, context, scope) =>
            // Install request from BC provides the above query params, which are used
            // to generate the access token and store it in the database
            onComplete(bigCommerce.generateAccessToken(code, context, scope)) {
              case Success(Some(auth)) => 
                val storeHash = auth.context.split("/").last
                // Store the access token in the database
                onSuccess(db.saveStoreToken(storeHash, auth.access_token, auth.scope)) { _ =>
                  // Create the owner user in the database
                  onSuccess(db.upsertStoreUser(auth.user.id, storeHash, auth.user.email, Owner)) { _ =>
                    println(s"Successful install for store ${storeHash} - owner user ${auth.user.email}")
                    val timestamp = (System.currentTimeMillis / 1000).toInt
                    val jwtContext = bigCommerce.encodeJwt(BcVerifiedUser(auth.user, auth.user, auth.context, timestamp))
                    redirect(s"/?context=$jwtContext", StatusCodes.Found)
                  }
                }
              case Success(None) =>
                complete(StatusCodes.Unauthorized, "Failed to authorize BigCommerce installation")
              case Failure(e) => 
                println(e.getMessage())
                complete(StatusCodes.InternalServerError, "Internal Server Eror when authenticating with BigCommerce")
            }
        } ~ 
        complete(StatusCodes.BadRequest, "Invalid request. Missing required query parameters.")
      },
      // /load endpoint
      pathPrefix("load") {
        withPayloadAuth { payload =>
          val storeHash = payload.context.split("/").last

          onSuccess(db.getStoreToken(storeHash)) {
            case Some(_) =>
              val timestamp = (System.currentTimeMillis / 1000).toInt
              val jwtContext = bigCommerce.encodeJwt(payload)

              // If somehow there isn't an owner user in the database
              // but the owner user is the same as the payload user,
              // then set the upsert role to Owner instead of Viewer.
              // (this will only likely happen during development)
              val upsertRole = if (payload.owner.id == payload.user.id) Owner else Viewer
              
              onSuccess(db.upsertStoreUser(payload.user.id, storeHash, payload.user.email, upsertRole)) { _ =>
                println(s"Successful load for store ${storeHash} - store user ${payload.user.email}")
                redirect(s"/?context=$jwtContext", StatusCodes.Found)
              }
            case None =>
              println(s"Error: failed to find store credentials for $storeHash on /load endpoint")
              complete(StatusCodes.Unauthorized, "Could not find store credentials. Please reinstall the app.")
          }
        }
      },
      // /uninstall endpoint
      pathPrefix("uninstall") {
        withPayloadAuth { payload =>
          // Perform uninstall actions
          val storeHash = payload.context.split("/").last
          
          onSuccess(db.deleteStoreToken(storeHash)) { _ =>
            db.deleteStoreUsers(storeHash).map { deleted => 
              println(s"Deleted $deleted users from store $storeHash on app uninstall")
            }
            complete(StatusCodes.OK)
          }
        }
      },
      // /removeUser endpoint
      pathPrefix("removeUser") {
        withPayloadAuth { payload =>
          val storeHash = payload.context.split("/").last
          val userId = payload.user.id
          
          onSuccess(db.deleteStoreUser(storeHash, userId)) { _ =>
            println(s"Deleted user $userId from store $storeHash on /removeUser endpoint")
            complete(StatusCodes.OK)
          }
        }
      },
      // If the prefix is /auth but unmatched, return a not found error
      complete(StatusCodes.NotFound)
    )
  }
}
