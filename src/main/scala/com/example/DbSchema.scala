package com.example

import slick.jdbc.PostgresProfile.api._

object UserRoles {
  sealed trait UserRole
  case object Viewer extends UserRole
  case object Admin extends UserRole
  case object Owner extends UserRole
}

// Object to hold the Database structured types
object DbSchema {
  import UserRoles._

  // Declare the row schema for the `tokens` table
  case class TokenRow(storeHash: String, accessToken: String, scopes: String)

  // Declare the schema for the `tokens` table
  class TokensTable(tag: Tag) extends Table[TokenRow](tag, None, "tokens") {
    val storeHash = column[String]("store_hash", O.PrimaryKey)
    val accessToken = column[String]("access_token")
    val scopes = column[String]("scopes")

    override def * = (storeHash, accessToken, scopes).mapTo[TokenRow]
  }

  // Map each permission type to a string and vice versa
  implicit def mapper: BaseColumnType[UserRole] = MappedColumnType.base[UserRole, String](
    {
      case Viewer => "viewer"
      case Admin => "admin"
      case Owner => "owner"
    },
    {
      case "viewer" => Viewer
      case "admin" => Admin
      case "owner" => Owner
    }
  )

  // Declare the row schema for the `users` table
  case class StoreUser(userId: Int, storeHash: String, email: String, role: UserRole, lastLogin: Int, id: Int = 0)

  // Declare the schema for the `users` table
  // Id is used as the primary key, but userId refers to the ID provided by BigCommerce
  class UsersTable(tag: Tag) extends Table[StoreUser](tag, None, "users") {
    val id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    val userId = column[Int]("user_id")
    val storeHash = column[String]("store_hash")
    val email = column[String]("email")
    val role = column[UserRole]("role")
    val lastLogin = column[Int]("last_login")
    val idx = index("idx_userid_hash_email", (userId, storeHash, email), unique = true)

    override def * = (userId, storeHash, email, role, lastLogin, id).mapTo[StoreUser]
  }
}
