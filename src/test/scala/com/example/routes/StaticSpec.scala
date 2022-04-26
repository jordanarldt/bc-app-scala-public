package com.example.routes

import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StaticSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  
  lazy val testKit = ActorTestKit()
  implicit def typedSystem = testKit.system
  override protected def createActorSystem(): ActorSystem = 
    testKit.system.classicSystem

  lazy val routes = Static.routes

  "Static Routes" should {
    
    "serve the index.html file successfully" in {
      Get("/") ~> routes ~> check {
        status should === (StatusCodes.OK)
        response.entity.contentType should === (ContentTypes.`text/html(UTF-8)`)
      }
    }

    "serve the index.html for an unmatched route" in {
      Get("/test") ~> routes ~> check {
        status should === (StatusCodes.OK)
        response.entity.contentType should === (ContentTypes.`text/html(UTF-8)`)
      }
    }

  }
}
