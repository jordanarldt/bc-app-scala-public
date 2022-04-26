package com.example

import com.example.lib.{Database, BigCommerce, BcAuthBypass}
import com.example.routes._

import scala.util.{Failure, Success}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteConcatenation.concat
import akka.http.scaladsl.server.Directives._

object HttpServer {
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    val port = sys.env.getOrElse("PORT", "8080").toInt
    val futureBinding = Http().newServerAt("0.0.0.0", port).bind(routes)

    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    val rootBehavior = Behaviors.setup[Nothing] { context =>

      implicit val system = context.system
      implicit val db = Database(system.executionContext)
      implicit val (bigCommerce, bcClientBypass) = AppConfig()
      
      // Join all of the app routes
      val routes = concat(
        new Auth().routes,
        new Api().routes,
        Static.routes,
      )

      startHttpServer(routes)

      Behaviors.empty
    }

    val system = ActorSystem[Nothing](rootBehavior, "AkkaHttpServer")
  }
}
