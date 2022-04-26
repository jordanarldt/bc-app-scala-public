package com.example.controllers

import com.example.ApiSchema._
import com.example.DbSchema
import com.example.UserRoles._
import com.example.JsonFormats._
import com.example.lib.{BcVerifiedUser, Database, ApiUtils}
import com.example.routes.JsonError

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

import spray.json.DefaultJsonProtocol._

// Controller for requests to the /api/users endpoint
object UsersController {
  def apply(payload: BcVerifiedUser, method: String)(implicit db: Database): Route = 
    new UsersController(payload, method, db).routes
}

class UsersController(payload: BcVerifiedUser, method: String, db: Database) {
  
  val storeHash = payload.context.split("/").last

  val routes: Route = method match {
    case "GET" =>
      concat(
        // /api/users/role endpoint
        pathPrefix("role") {
          onSuccess(db.getStoreUser(storeHash, payload.user.id)) {
            case Some(user) => complete(StatusCodes.OK, RoleResponse(user.role))
            case None => complete(StatusCodes.NotFound)
          }
        },
        // /api/users endpoint
        pathEndOrSingleSlash {
          parameters("page".as[Int].optional, "limit".as[Int].optional, "search".optional) { 
            (pageOpt, limitOpt, searchOpt) =>
              val page = pageOpt.getOrElse(1)
              val limit = limitOpt.getOrElse(10)
              val search = searchOpt.getOrElse("")
              
              onSuccess(db.getStoreUsers(storeHash, page, limit, search)) { (result, total) =>
                val data = result.map(u => UserResponse(u.userId, u.email, u.role, u.lastLogin))
                val pagination = PaginationInfo(page, Math.ceil(total.toDouble / limit).toInt, total)

                complete(StatusCodes.OK, PaginatedUsersResponse(data, pagination))
              }
          } ~ 
          // If the parameters aren't matched, they're invalid
          complete(StatusCodes.BadRequest)
        },
        // /api/users/??? unmatched (not found)
        complete(StatusCodes.NotFound)
      )
    case "PUT" =>
      // PUT /api/users endpoint
      pathEndOrSingleSlash {
        // Before updating the body, ensure the requesting user has proper permissions
        onSuccess(db.getStoreUser(storeHash, payload.user.id)) {
          case Some(currentUser) if currentUser.role != Viewer =>
            ApiUtils.validateBody[UpdateUserRoleBody] { body =>
              // Get the role of the target user
              onSuccess(db.getStoreUser(storeHash, body.userId)) {
                case Some(target) if target.role != Owner && target.userId != payload.user.id =>
                  // Ensure the user isn't an owner, and not the user making the request
                  onSuccess(db.updateStoreUserRole(storeHash, body.userId, body.role)) { _ =>
                    complete(StatusCodes.OK)
                  }
                case Some(_) =>
                  complete(JsonError(StatusCodes.Forbidden, "Cannot change the role of a store owner or yourself"))
                case None => 
                  complete(JsonError(StatusCodes.NotFound, "UserId does not exist"))
              }
            }
          case _ =>
            complete(JsonError(StatusCodes.Unauthorized, "You do not have the permissions to make this request"))
        }
      }
  }
}
