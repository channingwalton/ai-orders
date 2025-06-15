package com.example.aiorders.services

import cats.effect.IO
import cats.{Id, ~>}
import com.example.aiorders.models.{User, UserId}
import com.example.aiorders.store.UserStore
import com.example.aiorders.utils.TimeUtils
import munit.CatsEffectSuite

import java.util.UUID

class UserServiceSpec extends CatsEffectSuite {

  private val testUserId = UserId(UUID.randomUUID())
  private val testUser = User(
    id = testUserId,
    email = "test@example.com",
    name = "Test User",
    createdAt = TimeUtils.nowWithSecondPrecision
  )

  private def mockUserStore(users: Map[UserId, User] = Map.empty): UserStore[IO, Id] =
    new UserStore[IO, Id] {
      def create(user: User): Id[Unit]                 = ()
      def findById(userId: UserId): Id[Option[User]]   = users.get(userId)
      def findByEmail(email: String): Id[Option[User]] = users.values.find(_.email == email)
      def update(user: User): Id[Unit]                 = ()
      def delete(userId: UserId): Id[Unit]             = ()
      def exists(userId: UserId): Id[Boolean]          = users.contains(userId)
      def commit[A](f: Id[A]): IO[A]                   = IO.pure(f)
      def lift: cats.arrow.FunctionK[IO, Id] = new (IO ~> Id) {
        def apply[A](fa: IO[A]): Id[A] = fa.unsafeRunSync()(cats.effect.unsafe.implicits.global)
      }
    }

  test("userExists returns false for non-existent user") {
    val service = UserService.withStore(mockUserStore())
    service.userExists(testUserId).map { exists =>
      assertEquals(exists, false)
    }
  }

  test("createUser creates a new user") {
    val service = UserService.withStore(mockUserStore())
    service.createUser("test@example.com", "Test User").map { user =>
      assertEquals(user.email, "test@example.com")
      assertEquals(user.name, "Test User")
      assert(user.id != null)
      assert(user.createdAt != null)
    }
  }

  test("userExists returns true for existing user") {
    val service = UserService.withStore(mockUserStore(Map(testUserId -> testUser)))
    service.userExists(testUserId).map { exists =>
      assertEquals(exists, true)
    }
  }

  test("getUser returns None for non-existent user") {
    val service = UserService.withStore(mockUserStore())
    service.getUser(testUserId).map { userOpt =>
      assertEquals(userOpt, None)
    }
  }

  test("getUser returns Some(user) for existing user") {
    val service = UserService.withStore(mockUserStore(Map(testUserId -> testUser)))
    service.getUser(testUserId).map { userOpt =>
      assertEquals(userOpt, Some(testUser))
    }
  }
}
