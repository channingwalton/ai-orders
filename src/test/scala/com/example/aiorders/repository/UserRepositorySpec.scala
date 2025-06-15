package com.example.aiorders.repository

import cats.effect.IO
import com.example.aiorders.models.User

import java.time.Instant
import java.util.UUID

class UserRepositorySpec extends DatabaseTestSuite {

  test("UserRepository.create should insert a new user") {
    withDatabase { xa =>
      val repository = UserRepository[IO](xa)
      val userId     = UUID.randomUUID()
      val timestamp  = Instant.now()
      val user       = User(userId, "John Doe", "john.doe@example.com", timestamp)

      for {
        createdUser <- repository.create(user)
        foundUser   <- repository.findById(userId)
      } yield {
        assertEquals(createdUser, user)
        assertEquals(foundUser, Some(user))
      }
    }
  }

  test("UserRepository.findById should return None for non-existent user") {
    withDatabase { xa =>
      val repository    = UserRepository[IO](xa)
      val nonExistentId = UUID.randomUUID()

      repository.findById(nonExistentId).map { result =>
        assertEquals(result, None)
      }
    }
  }

  test("UserRepository.findByEmail should find user by email") {
    withDatabase { xa =>
      val repository = UserRepository[IO](xa)
      val userId     = UUID.randomUUID()
      val timestamp  = Instant.now()
      val user       = User(userId, "Jane Smith", "jane.smith@example.com", timestamp)

      for {
        _         <- repository.create(user)
        foundUser <- repository.findByEmail("jane.smith@example.com")
      } yield assertEquals(foundUser, Some(user))
    }
  }

  test("UserRepository.findByEmail should return None for non-existent email") {
    withDatabase { xa =>
      val repository = UserRepository[IO](xa)

      repository.findByEmail("nonexistent@example.com").map { result =>
        assertEquals(result, None)
      }
    }
  }

  test("UserRepository.findByEmail should be case sensitive") {
    withDatabase { xa =>
      val repository = UserRepository[IO](xa)
      val userId     = UUID.randomUUID()
      val timestamp  = Instant.now()
      val user       = User(userId, "Test User", "test@example.com", timestamp)

      for {
        _         <- repository.create(user)
        foundUser <- repository.findByEmail("TEST@EXAMPLE.COM")
      } yield assertEquals(foundUser, None)
    }
  }

  test("UserRepository should handle multiple users") {
    withDatabase { xa =>
      val repository = UserRepository[IO](xa)
      val timestamp  = Instant.now()
      val user1      = User(UUID.randomUUID(), "User One", "user1@example.com", timestamp)
      val user2      = User(UUID.randomUUID(), "User Two", "user2@example.com", timestamp)

      for {
        _          <- repository.create(user1)
        _          <- repository.create(user2)
        foundUser1 <- repository.findById(user1.id)
        foundUser2 <- repository.findByEmail("user2@example.com")
      } yield {
        assertEquals(foundUser1, Some(user1))
        assertEquals(foundUser2, Some(user2))
      }
    }
  }

  test("UserRepository should enforce unique email constraint") {
    withDatabase { xa =>
      val repository = UserRepository[IO](xa)
      val timestamp  = Instant.now()
      val user1      = User(UUID.randomUUID(), "User One", "duplicate@example.com", timestamp)
      val user2      = User(UUID.randomUUID(), "User Two", "duplicate@example.com", timestamp)

      for {
        _      <- repository.create(user1)
        result <- repository.create(user2).attempt
      } yield assert(result.isLeft, "Should fail due to unique email constraint")
    }
  }

  test("UserRepository should handle special characters in names and emails") {
    withDatabase { xa =>
      val repository = UserRepository[IO](xa)
      val userId     = UUID.randomUUID()
      val timestamp  = Instant.now()
      val user = User(
        userId,
        "José María O'Connor-Smith",
        "josé.maría@example-domain.co.uk",
        timestamp
      )

      for {
        createdUser <- repository.create(user)
        foundUser   <- repository.findById(userId)
      } yield {
        assertEquals(createdUser, user)
        assertEquals(foundUser, Some(user))
      }
    }
  }
}
