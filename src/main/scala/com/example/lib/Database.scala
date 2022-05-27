package com.example.lib

import com.example.DbSchema._
import com.example.UserRoles.UserRole

import scala.concurrent.{Future, ExecutionContext}

import slick.jdbc.JdbcBackend.{Database => JdbcDatabase}
import slick.jdbc.PostgresProfile.api._

object Database {
  private val tokenTable = TableQuery[TokensTable]
  private val usersTable = TableQuery[UsersTable]

  // Require execution context for certain async operations
  def apply(ec: ExecutionContext) = new Database(ec)

  // Set up the database by creating the `tokens` table if it doesn't exist yet
  private def setup() = DBIO.seq(
    (tokenTable.schema ++ usersTable.schema).createIfNotExists,
  )
}

class Database(context: ExecutionContext) {
  implicit val ec = context
  import Database._

  val db = JdbcDatabase.forConfig("my-app.database")
  
  // Run the setup to ensure the schemas are created
  db.run(setup)

  // Save a token to the database
  def saveStoreToken(storeHash: String, accessToken: String, scopes: String): Future[Int] = 
    db.run(tokenTable.insertOrUpdate(TokenRow(storeHash, accessToken, scopes)))

  // Get a token from the database
  def getStoreToken(storeHash: String): Future[Option[String]] = 
    db.run(tokenTable.filter(_.storeHash === storeHash).map(_.accessToken).result.headOption)

  // Get token scopes from the database
  def getTokenScopes(storeHash: String): Future[Option[String]] =
    db.run(tokenTable.filter(_.storeHash === storeHash).map(_.scopes).result.headOption)

  // Delete a token from the database
  def deleteStoreToken(storeHash: String): Future[Int] = 
    db.run(tokenTable.filter(_.storeHash === storeHash).delete)

  // Insert or update a user
  def upsertStoreUser(userId: Int, storeHash: String, email: String, role: UserRole): Future[Int] = {
    // When this method is called, the user may or may not exist in the database.
    // If the user does exist, the timestamp will be updated, otherwise a new user will be inserted.
    val timestamp = (System.currentTimeMillis / 1000).toInt

    val existingRow = db.run(usersTable.filter(u => u.email === email && u.storeHash === storeHash).result.headOption)

    existingRow.flatMap { userOption => 
      if (userOption.isEmpty) {
        db.run(usersTable.insertOrUpdate(StoreUser(userId, storeHash, email, role, timestamp)))
      } else {
        db.run(usersTable.filter(u => u.email === email && u.storeHash === storeHash)
          .map(_.lastLogin).update(timestamp))
      }
    }
  }

  // Get all users by store hash
  def getStoreUsers(
    storeHash: String, 
    page: Int = 1, 
    limit: Int = 250, 
    search: String = ""
  ): Future[(Seq[StoreUser], Int)] = {
    val offset = (page - 1) * limit
    val queries = for {
      rows <- 
        usersTable.filter(r => r.storeHash === storeHash && r.email.like(s"%$search%"))
          .sortBy(_.id).drop(offset).take(limit).result
      total <- 
        usersTable.filter(r => r.storeHash === storeHash && r.email.like(s"%$search%")).length.result
    } yield (rows, total)

    db.run(queries)
  }
  
  // Get a user from the database
  def getStoreUser(storeHash: String, email: String): Future[Option[StoreUser]] = 
    db.run(usersTable.filter(u => u.email === email && u.storeHash === storeHash).result.headOption)

  def getStoreUser(storeHash: String, userId: Int): Future[Option[StoreUser]] = 
    db.run(usersTable.filter(u => u.userId === userId && u.storeHash === storeHash).result.headOption)

  // Update a user role in the database
  def updateStoreUserRole(id: Int, role: UserRole): Future[Int] =
    db.run(usersTable.filter(_.id === id).map(_.role).update(role))

  def updateStoreUserRole(storeHash: String, userId: Int, role: UserRole): Future[Int] =
    db.run(usersTable.filter(_.userId === userId).map(_.role).update(role))

  def updateStoreUserRole(storeHash: String, email: String, role: UserRole): Future[Int] = 
    db.run(usersTable.filter(u => u.email === email && u.storeHash === storeHash).map(_.role).update(role))

  // Delete a user from the database
  def deleteStoreUser(id: Int): Future[Int] = 
    db.run(usersTable.filter(_.id === id).delete)

  def deleteStoreUser(storeHash: String, userId: Int): Future[Int] = 
    db.run(usersTable.filter(u => u.userId === userId && u.storeHash === storeHash).delete)

  def deleteStoreUser(storeHash: String, email: String): Future[Int] = 
    db.run(usersTable.filter(u => u.email === email && u.storeHash === storeHash).delete)

  // Delete all users by store hash from the database
  def deleteStoreUsers(storeHash: String): Future[Int] = 
    db.run(usersTable.filter(_.storeHash === storeHash).delete)
}
