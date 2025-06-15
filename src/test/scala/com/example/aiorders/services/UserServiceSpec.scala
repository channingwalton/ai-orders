package com.example.aiorders.services

import cats.effect.IO
import com.example.aiorders.models.UserId
import munit.CatsEffectSuite

import java.util.UUID

class UserServiceSpec extends CatsEffectSuite {

  private val testUserId = UserId(UUID.randomUUID())

  test("userExists returns false for non-existent user") {
    UserService.inMemory[IO].flatMap { service =>
      service.userExists(testUserId).map { exists =>
        assertEquals(exists, false)
      }
    }
  }

  test("createUser creates a new user") {
    UserService.inMemory[IO].flatMap { service =>
      service.createUser("test@example.com", "Test User").map { user =>
        assertEquals(user.email, "test@example.com")
        assertEquals(user.name, "Test User")
        assert(user.id != null)
        assert(user.createdAt != null)
      }
    }
  }

  test("userExists returns true after creating user") {
    UserService.inMemory[IO].flatMap { service =>
      for {
        user   <- service.createUser("test@example.com", "Test User")
        exists <- service.userExists(user.id)
      } yield assertEquals(exists, true)
    }
  }

  test("getUser returns None for non-existent user") {
    UserService.inMemory[IO].flatMap { service =>
      service.getUser(testUserId).map { userOpt =>
        assertEquals(userOpt, None)
      }
    }
  }

  test("getUser returns Some(user) for existing user") {
    UserService.inMemory[IO].flatMap { service =>
      for {
        createdUser   <- service.createUser("test@example.com", "Test User")
        retrievedUser <- service.getUser(createdUser.id)
      } yield assertEquals(retrievedUser, Some(createdUser))
    }
  }
}
