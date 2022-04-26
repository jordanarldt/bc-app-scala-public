package com.example.routes

import com.example.lib.{Database, BcUser}
import com.example.AppConfig

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.{ScalatestRouteTest, RouteTestTimeout}

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AuthSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  
  lazy val testKit = ActorTestKit()
  implicit def typedSystem = testKit.system
  override protected def createActorSystem(): ActorSystem = 
    testKit.system.classicSystem

  implicit val timeout = RouteTestTimeout(5.seconds)

  implicit val db = Database(testKit.system.executionContext)
  
  // Set up configuration variables
  implicit val bigCommerce = AppConfig()._1

  lazy val routes = new Auth().routes
  
  val testUser1 = BcUser(1, "test1@test.com")
  val testUser2 = BcUser(2, "test2@test.com")
  val user1Jwt = bigCommerce.mimicSignedPayloadJwt(testUser1, "stores/test")
  val user2Jwt = bigCommerce.mimicSignedPayloadJwt(testUser2, "stores/test")

  // Create an access token entry in the DB for the test user when the test suite starts.
  // Cleanup will be done automatically in the /uninstall test
  override def beforeAll() = {
    db.saveStoreToken("test", "accesstokenfortesting", "nothing")
  }

  // Only test the /load, /uninstall, and /removeUser routes.
  // Cannot test /install because it requires a temporary code generated 
  // by BigCommerce which cannot be mimicked.
  "Auth Routes" should {

    "respond with a 404 on an invalid route" in {
      Get("/auth/invalid") ~> routes ~> check {
        status should === (StatusCodes.NotFound)
      }
    }

    "respond with redirect on valid request (GET /auth/load)" in {
      Get(s"/auth/load?signed_payload_jwt=$user1Jwt") ~> routes ~> check {
        status should === (StatusCodes.Found)
      }
    }

    "create a user in the database on valid request (GET /auth/load)" in {
      Get(s"/auth/load?signed_payload_jwt=$user2Jwt") ~> routes ~> check {
        status should === (StatusCodes.Found)
        db.getStoreUser("test", testUser2.email).map { dbResult =>
          dbResult.isDefined should === (true)
        }
      }
    }

    "remove a user from the database on valid removeUser request (GET /auth/removeUser)" in {
      Get(s"/auth/removeUser?signed_payload_jwt=$user2Jwt") ~> routes ~> check {
        status should === (StatusCodes.OK)
        db.getStoreUser("test", testUser2.email).map { dbResult =>
          dbResult.isDefined should === (false)
        }
      }
    }

    "remove all users and access token from the database on valid uninstall request (GET /auth/uninstall)" in {
      Get(s"/auth/uninstall?signed_payload_jwt=$user1Jwt") ~> routes ~> check {
        status should === (StatusCodes.OK)
        db.getStoreUser("test", testUser1.email).map { dbResult =>
          dbResult.isDefined should === (false)
        }
        db.getStoreToken("test").map { dbResult =>
          dbResult.isDefined should === (false)
        }
      }
    }
    
  }
}
