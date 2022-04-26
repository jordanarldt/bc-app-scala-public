package com.example.routes

import com.example.ApiSchema._
import com.example.{AppConfig, UserRoles}
import com.example.lib.{Database, BcUser, BcVerifiedUser, BigCommerce}

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.{ScalatestRouteTest, RouteTestTimeout}

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ApiSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import spray.json.DefaultJsonProtocol._
  import com.example.JsonFormats._
  
  lazy val testKit = ActorTestKit()
  implicit def typedSystem = testKit.system
  override protected def createActorSystem(): ActorSystem = 
    testKit.system.classicSystem

  implicit val timeout = RouteTestTimeout(5.seconds)
  implicit val db = Database(testKit.system.executionContext)

  // Get the configuration variables
  implicit val (bigCommerce, bcClientBypass) = AppConfig()

  /*
   * In order for the API routes to be tested, 
   * the 'bc-auth-token' must be set in the 'application.conf' file.
   * 
   * If 'bc-auth-bypass' parameter is set to false, the tests will automatically
   * create a new token database entry using the 'bc-auth-token' value in order to complete the tests.
   * 
   * If neither are set, the route tests requiring authentication will be cancelled.
   */

  val configBypass = system.settings.config.getBoolean("my-app.bigcommerce.bypass-auth")
  val configBypassHash = system.settings.config.getString("my-app.bigcommerce.bypass-auth-hash")
  val configBypassToken = system.settings.config.getString("my-app.bigcommerce.bypass-auth-token")
  val configBypassUserId = system.settings.config.getInt("my-app.bigcommerce.bypass-auth-userid")
  val apiConfigValid: Boolean = {
    if (bcClientBypass.isEmpty) {
      configBypassToken.nonEmpty && configBypassHash.nonEmpty && configBypassUserId.isValidInt
    } else {
      true
    }
  }

  lazy val routes = new Api().routes

  // Mimic the context JWT used on the client for authentication
  val testUser = BcUser(1, "apitest@test.com")
  val timestamp = (System.currentTimeMillis() / 1000).toInt
  val testContext = bigCommerce.encodeJwt(BcVerifiedUser(testUser, testUser, "stores/apitest", timestamp))
  
  // For /api/variants tests, a valid store hash is required for the API requests
  // Create a context JWT using the store hash set by the bypass-auth-hash parameter
  val bypassUser = BcUser(configBypassUserId, "apitest@test.com")
  val bypassContext = bigCommerce.encodeJwt(BcVerifiedUser(bypassUser, bypassUser, s"stores/$configBypassHash", timestamp))

  // If bypass-auth is false and the bypass-auth-token is set, create a temporary
  // database entry for the api route tests. For /api/variant endpoint tests, a valid
  // store hash and access token is required. The store hash/access token will be set
  // to the bypass-auth-token value while tests are being ran, and then be reverted
  // to the original value after the tests complete (if one existed).
  // This is so unit tests can run regardless of if the app has been installed on BigCommerce.
  val originalToken = Await.result(db.getStoreToken(configBypassHash), 1.second)
  val originalScopes = Await.result(db.getTokenScopes(configBypassHash), 1.second)

  override def beforeAll(): Unit = {
    if (apiConfigValid && bcClientBypass.isEmpty) {
      db.saveStoreToken("apitest", configBypassToken, "testingscopes")
      db.upsertStoreUser(1, "apitest", testUser.email, UserRoles.Owner)

      // Temporarily use the bypass token for the variant route tests
      db.saveStoreToken(configBypassHash, configBypassToken, originalScopes.getOrElse("testingscopes"))
      
      // Create alot of DB users to test pagination API responses
      for (i <- 2 to 100) {
        db.upsertStoreUser(i, "apitest", s"apitestuser$i@test.com", UserRoles.Viewer)
      }
    }
  }

  // Delete the test token after the test suite completes
  override def afterAll(): Unit = {
    db.deleteStoreToken("apitest")
    db.deleteStoreUsers("apitest")

    // If the bypass-auth-hash already had a token in the DB,
    // set it back to the original value - otherwise remove it
    if (originalToken.isDefined && originalScopes.isDefined) {
      db.saveStoreToken(configBypassHash, originalToken.get, originalScopes.get)
    } else {
      db.deleteStoreToken(configBypassHash)
    }
  }

  if (!apiConfigValid) cancel("Cannot test API routes unless bypass-auth configs are set in application.conf")

  "The /api route" should {
    "respond with 404 on an invalid route" in {
      Get("/api/invalid") ~> routes ~> check {
        status should === (StatusCodes.NotFound)
      }
    }
  }

  "The /api/users route" should {
    "respond with user role on GET /api/users/role" in {
      if (configBypass) cancel("This test will always fail if bypass-auth is true")

      Get(s"/api/users/role?context=$testContext") ~> routes ~> check {
        status should === (StatusCodes.OK)
        responseAs[String] should === ("""{"role":"owner"}""")
      }
    }

    "respond with 401 on GET /api/users/role with invalid context" in {
      if (configBypass) cancel("This test will always pass if bypass-auth is true")

      Get(s"/api/users/role?context=invalid") ~> routes ~> check {
        status should === (StatusCodes.Unauthorized)
      }
    }

    "respond with Method Not Allowed on invalid method at /api/users/role" in {
      Post(s"/api/users/role?context=$testContext") ~> routes ~> check {
        status should === (StatusCodes.MethodNotAllowed)
      }
    }

    "respond with Not Found on invalid path at /api/users/invalid" in {
      if (configBypass) cancel("This test will always fail if bypass-auth is true")

      Get(s"/api/users/invalid?context=$testContext") ~> routes ~> check {
        status should === (StatusCodes.NotFound)
      }
    }

    "respond with all users for a store on GET /api/users" in {
      if (configBypass) cancel("This test will always fail if bypass-auth is true")

      Get(s"/api/users?context=$testContext&limit=100") ~> routes ~> check {
        status should === (StatusCodes.OK)

        val res = responseAs[PaginatedUsersResponse]
        res.data.length should === (100)
      }
    }

    "respond with Bad Request on invalid query params" in {
      if (configBypass) cancel("This test will always fail if bypass-auth is true")

      Get(s"/api/users?context=$testContext&page=invalid&limit=invalid") ~> routes ~> check {
        status should === (StatusCodes.BadRequest)
      }
    }

    "be able to update a user role PUT /api/users" in {
      if (configBypass) cancel("This test will always fail if bypass-auth is true")

      Put(s"/api/users?context=$testContext", UpdateUserRoleBody(2, UserRoles.Admin)) ~> routes ~> check {
        val dbResult = Await.result(db.getStoreUser("apitest", 2), 5.seconds)
        dbResult.isDefined should === (true)
        dbResult.get.role should === (UserRoles.Admin)
        status should === (StatusCodes.OK)
      }
    }

    "respond with Bad Request on invalid body" in {
      if (configBypass) cancel("This test will always fail if bypass-auth is true")

      val reqBody = """{"userId":2,"role":"invalid"}"""
      Put(s"/api/users?context=$testContext", reqBody) ~> routes ~> check {
        status should === (StatusCodes.BadRequest)
      }
    }

    "respond with Unauthorized if requester does not have permissions on PUT /api/users" in {
      if (configBypass) cancel("This test will not be accurate if bypass-auth is true")

      val testViewer = BcUser(5, "apitestuser5@test.com")
      val viewerContext = bigCommerce.encodeJwt(BcVerifiedUser(testViewer, testUser, "stores/apitest", timestamp))
      val reqBody = """{"userId":2,"role":"admin"}"""

      Put(s"/api/users?context=$viewerContext", reqBody) ~> routes ~> check {
        status should === (StatusCodes.Unauthorized)
      }
    }

    "respond with Forbidden if the userId in the request is an owner" in {
      if (configBypass) cancel("This test will always fail if bypass-auth is true")

      val testViewer = BcUser(2, "apitestuser2@test.com")
      val viewerContext = bigCommerce.encodeJwt(BcVerifiedUser(testViewer, testUser, "stores/apitest", timestamp))
      val reqBody = """{"userId":1,"role":"admin"}"""

      Put(s"/api/users?context=$viewerContext", reqBody) ~> routes ~> check {
        status should === (StatusCodes.Forbidden)
      }
    }

    "respond with Forbidden if the userId in the request is the same as the user making the request" in {
      if (configBypass) cancel("This test will always fail if bypass-auth is true")

      val reqBody = """{"userId":1,"role":"admin"}"""

      Put(s"/api/users?context=$testContext", reqBody) ~> routes ~> check {
        status should === (StatusCodes.Forbidden)
      }
    }
  }
  
  // These tests need a valid store/store hash to make the requests to BigCommerce
  "The /api/variants route" should {
    "respond with paginated variant data" in {
      Get(s"/api/variants?context=$bypassContext") ~> routes ~> check {
        status should === (StatusCodes.OK)
        entityAs[String] should (include("data") and include("pagination"))
      }
    }

    "be able to update a variants inventory level and tracking type at PUT /api/variants" in {
      Get(s"/api/variants?context=$bypassContext") ~> routes ~> check {
        val res = entityAs[AmendedVariantResponse]
        if (res.data.isEmpty) cancel("No variants found, so the test cannot run")

        val variant = res.data.head
        val body = UpdateVariantBody(variant.id, variant.product_id, 75, Some("product"))
        
        Put(s"/api/variants?context=$bypassContext", body) ~> routes ~> check {
          status should === (StatusCodes.OK)
          // After it's been updated successfully, set it back to the original values
          val original = UpdateVariantBody(variant.id, variant.product_id, variant.inventory_level, Some(variant.inventory_tracking))
          Put(s"/api/variants?context=$bypassContext", original) ~> routes ~> check {
            status should === (StatusCodes.OK)
          }
        }
      }
    }

    "reject an update request on a user with invalid permissions" in {
      if (configBypass) cancel("This test will not be accurate if bypass-auth is true")

      val testViewer = BcUser(3, "apitestuser3@test.com")
      val viewerContext = bigCommerce.encodeJwt(BcVerifiedUser(testViewer, testUser, "stores/apitest", timestamp))
      val body = UpdateVariantBody(1, 1, 5, Some("variant"))
      
      Put(s"/api/variants?context=$viewerContext", body) ~> routes ~> check {
        status should === (StatusCodes.Unauthorized)
      }
    }
  }
}
