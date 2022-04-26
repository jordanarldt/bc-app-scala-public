package com.example.lib

import com.example.lib.Database
import com.example.DbSchema._
import com.example.UserRoles._

import scala.util.{Success, Failure}

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

// Simple testkit for the database
class DatabaseSpec extends AsyncWordSpecLike {
  val ec = scala.concurrent.ExecutionContext.global
  val db = Database(ec)

  "Database" should {

    "be able to save a store token" in {
      val query = db.saveStoreToken("testing", "accesstoken", "scopes")

      query.map { result => 
        assert(result === 1)
      }
    }

    "be able to fetch a token by store hash" in {
      val query = db.getStoreToken("testing")

      query.map { result =>
        assert(result === Some("accesstoken"))
      }
    }

    "return nothing if a store does not have a token" in {
      val query = db.getStoreToken("nonexistant")

      query.map { result => 
        assert(result === None)
      }
    }

    "be able to delete a token by store hash" in {
      val query = db.deleteStoreToken("testing")

      query.map { result =>
        assert(result === 1)
      }
    }

    "be able to add a new store user" in {
      val query = db.upsertStoreUser(1, "testing", "test@test.com", Owner)

      query.map { result =>
        assert(result === 1)
      }
    }

    "be able to get a store user" in {
      val query = db.getStoreUser("testing", "test@test.com")

      query.map { result =>
        assert(result.isDefined)

        val user = result.get
        assert(user.userId === 1)
        assert(user.storeHash === "testing")
        assert(user.email === "test@test.com")
      }
    }

    "return nothing when fetching a store user that does not exist" in {
      val query = db.getStoreUser("testing", "doesnotexist@email.com")
      
      query.map { result =>
        assert(result === None)
      }
    }

    "be able to update a store user role" in {
      val query = db.upsertStoreUser(2, "testing", "testadmin@test.com", Admin)

      query.flatMap { rows =>
        assert(rows === 1)
        db.getStoreUser("testing", "testadmin@test.com").flatMap(result => {
          assert(result.get.role === Admin)

          db.updateStoreUserRole("testing", 2, Viewer)
            .flatMap(_ => db.getStoreUser("testing", "testadmin@test.com"))
            .map(result => {
              assert(result.isDefined)
              val user = result.get
              assert(user.role === Viewer)
            })
        })
      }
    }

    "be able to get all users by store hash" in {
      val query = db.getStoreUsers("testing")
      query.map { case (data, total) => 
        assert(data.size === 2)
      }
    }

    "be able to delete a store user" in {
      // delete both users created by the tests
      val query = db.deleteStoreUser("testing", 1)
      query.map(result => assert(result === 1))
    }

    "be able to delete all store users" in {
      // create an additional user before testing
      val query = db.upsertStoreUser(3, "testing", "testing@test.com", Admin)
      query.flatMap { result =>
        assert(result === 1)
        // delete all users
        db.deleteStoreUsers("testing").map(result => assert(result === 2))
      }
    }

  }
}
