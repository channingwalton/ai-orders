package com.example.aiorders.services

import cats.effect.MonadCancelThrow
import cats.syntax.all._
import com.example.aiorders.models.{User, UserId}
import com.example.aiorders.store.OrderStore
import com.example.aiorders.utils.TimeUtils

import java.util.UUID

trait UserService[F[_]] {
  def userExists(userId: UserId): F[Boolean]
  def createUser(email: String, name: String): F[User]
  def getUser(userId: UserId): F[Option[User]]
}

class DatabaseUserService[F[_], G[_]](store: OrderStore[F, G])(implicit
  F: MonadCancelThrow[F]
) extends UserService[F] {

  def userExists(userId: UserId): F[Boolean] =
    store.commit(store.userExists(userId))

  def createUser(email: String, name: String): F[User] = {
    val user = User(
      id = UserId(UUID.randomUUID()),
      email = email,
      name = name,
      createdAt = TimeUtils.nowWithSecondPrecision
    )

    store.commit(store.createUser(user)) *> F.pure(user)
  }

  def getUser(userId: UserId): F[Option[User]] =
    store.commit(store.findUserById(userId))
}

object UserService {
  def withStore[F[_]: MonadCancelThrow, G[_]](store: OrderStore[F, G]): UserService[F] =
    new DatabaseUserService[F, G](store)
}
