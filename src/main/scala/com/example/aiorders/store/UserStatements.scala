package com.example.aiorders.store

import com.example.aiorders.models.{User, UserId}
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

import java.time.Instant
import java.util.UUID

object UserStatements {

  implicit val userIdMeta: Meta[UserId] = Meta[UUID].timap(UserId.apply)(_.value)

  def create(user: User): Update0 =
    sql"""
      INSERT INTO users (id, email, name, created_at)
      VALUES (${user.id}, ${user.email}, ${user.name}, ${user.createdAt})
    """.update

  def findById(userId: UserId): Query0[User] =
    sql"""
      SELECT id, email, name, created_at
      FROM users
      WHERE id = $userId
    """.query[User]

  def findByEmail(email: String): Query0[User] =
    sql"""
      SELECT id, email, name, created_at
      FROM users
      WHERE email = $email
    """.query[User]

  def update(user: User): Update0 =
    sql"""
      UPDATE users
      SET email = ${user.email}, name = ${user.name}
      WHERE id = ${user.id}
    """.update

  def delete(userId: UserId): Update0 =
    sql"""
      DELETE FROM users
      WHERE id = $userId
    """.update

  def exists(userId: UserId): Query0[Boolean] =
    sql"""
      SELECT EXISTS(SELECT 1 FROM users WHERE id = $userId)
    """.query[Boolean]
}
