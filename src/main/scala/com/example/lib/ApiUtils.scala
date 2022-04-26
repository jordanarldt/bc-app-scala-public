package com.example.lib

import com.example.routes.JsonError

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

object ApiUtils {
  import spray.json._

  /**
  * This method validates the request body against the type
  * provided and automatically throws a BadRequest error if
  * the body is not valid. If it's successful, it will return
  * the expected type in a curried function callback.
  */
  def validateBody[T: JsonReader](routeLogic: (T) => Route): Route = {
    entity(as[String]) { body =>
      try {
        val parsed = body.parseJson.convertTo[T]
        routeLogic(parsed)
      } catch {
        case e: DeserializationException =>
          complete(JsonError(StatusCodes.BadRequest, "Invalid JSON in request body"))
      }
    }
  }
}
