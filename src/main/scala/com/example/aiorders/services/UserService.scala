package com.example.aiorders.services

import cats.effect.Ref
import cats.syntax.all._
import com.example.aiorders.models.{User, UserId}

import java.time.Instant
import java.util.UUID

trait UserService[F[_]] {
  def userExists(userId: UserId): F[Boolean]
  def createUser(email: String, name: String): F[User]
  def getUser(userId: UserId): F[Option[User]]
}

class InMemoryUserService[F[_]](storage: Ref[F, Map[UserId, User]])(implicit
  F: cats.effect.Sync[F]
) extends UserService[F] {

  def userExists(userId: UserId): F[Boolean] =
    storage.get.map(_.contains(userId))

  def createUser(email: String, name: String): F[User] = {
    val user = User(
      id = UserId(UUID.randomUUID()),
      email = email,
      name = name,
      createdAt = Instant.now()
    )

    storage.update(_.updated(user.id, user)) *> F.pure(user)
  }

  def getUser(userId: UserId): F[Option[User]] =
    storage.get.map(_.get(userId))
}

object UserService {
  def inMemory[F[_]: cats.effect.Sync]: F[UserService[F]] =
    Ref.of[F, Map[UserId, User]](Map.empty).map(new InMemoryUserService[F](_))
}
