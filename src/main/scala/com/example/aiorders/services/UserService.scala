package com.example.aiorders.services

import cats.Monad
import cats.syntax.all._
import com.example.aiorders.models.{User, UserId}
import com.example.aiorders.store.OrderStore
import com.example.aiorders.utils.TimeUtils

import java.util.UUID

trait UserService[G[_]] {
  def userExists(userId: UserId): G[Boolean]
  def createUser(email: String, name: String): G[User]
  def getUser(userId: UserId): G[Option[User]]
}

class DatabaseUserService[F[_], G[_]](store: OrderStore[F, G])(implicit
  G: Monad[G]
) extends UserService[G] {

  def userExists(userId: UserId): G[Boolean] =
    store.userExists(userId)

  def createUser(email: String, name: String): G[User] = {
    val user = User(
      id = UserId(UUID.randomUUID()),
      email = email,
      name = name,
      createdAt = TimeUtils.nowWithSecondPrecision
    )

    store.createUser(user) *> G.pure(user)
  }

  def getUser(userId: UserId): G[Option[User]] =
    store.findUserById(userId)
}

object UserService {
  def withStore[F[_], G[_]: Monad](store: OrderStore[F, G]): UserService[G] =
    new DatabaseUserService[F, G](store)
}
