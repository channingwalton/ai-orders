package com.example.aiorders

import cats.effect.{IO, Ref}
import com.example.aiorders.models.{User, UserId}
import com.example.aiorders.services.UserService
import com.example.aiorders.utils.TimeUtils

import java.util.UUID

object TestHelpers {

  def createInMemoryUserService: IO[UserService[IO]] =
    for {
      ref <- Ref.of[IO, Map[UserId, User]](Map.empty)
    } yield new UserService[IO] {
      def userExists(userId: UserId): IO[Boolean] =
        ref.get.map(_.contains(userId))

      def createUser(email: String, name: String): IO[User] = {
        val user = User(
          id = UserId(UUID.randomUUID()),
          email = email,
          name = name,
          createdAt = TimeUtils.nowWithSecondPrecision
        )
        ref.update(_.updated(user.id, user)) *> IO.pure(user)
      }

      def getUser(userId: UserId): IO[Option[User]] =
        ref.get.map(_.get(userId))
    }
}
