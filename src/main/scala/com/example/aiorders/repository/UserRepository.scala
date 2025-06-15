package com.example.aiorders.repository

import cats.effect.Sync
import cats.syntax.functor._
import com.example.aiorders.models.User
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.{Read, Write}

import java.time.Instant
import java.util.UUID

trait UserRepository[F[_]] {
  def findById(id: UUID): F[Option[User]]
  def findByEmail(email: String): F[Option[User]]
  def create(user: User): F[User]
}

object UserRepository {

  implicit val userRead: Read[User] =
    Read[(UUID, String, String, Instant)].map { case (id, name, email, createdAt) =>
      User(id, name, email, createdAt)
    }

  implicit val userWrite: Write[User] =
    Write[(UUID, String, String, Instant)].contramap { user =>
      (user.id, user.name, user.email, user.createdAt)
    }

  def apply[F[_]: Sync](xa: doobie.Transactor[F]): UserRepository[F] =
    new UserRepository[F] {

      def findById(id: UUID): F[Option[User]] =
        sql"SELECT id, name, email, created_at FROM users WHERE id = $id"
          .query[User]
          .option
          .transact(xa)

      def findByEmail(email: String): F[Option[User]] =
        sql"SELECT id, name, email, created_at FROM users WHERE email = $email"
          .query[User]
          .option
          .transact(xa)

      def create(user: User): F[User] =
        sql"INSERT INTO users (id, name, email, created_at) VALUES (${user.id}, ${user.name}, ${user.email}, ${user.createdAt})".update.run
          .transact(xa)
          .as(user)
    }
}
