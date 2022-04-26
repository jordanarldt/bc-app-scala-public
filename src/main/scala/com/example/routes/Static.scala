package com.example.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

object Static {
  val routes: Route = {
    (extractRequest & get) { req =>
      concat(
        pathPrefix("static") {
          concat(
            path(Segment / RemainingPath) { (assetType, file) =>
              getFromResource(s"client/static/$assetType/$file")
            },
            complete(StatusCodes.NotFound)
          )
        },
        path("favicon.ico") {
          getFromResource("client/favicon.ico")
        },
        path("manifest.json") {
          getFromResource("client/manifest.json")
        },
        path("asset-manifest.json") {
          getFromResource("client/asset-manifest.json")
        },
        path("robots.txt") {
          getFromResource("client/robots.txt")
        },
        // All other requests are routed to the React App
        // as long as the prefix isn't "api" or "static"
        path(Remaining) { route =>
          getFromResource("client/index.html")
        }
      )
    }
  }
}
