package com.example.aiorders.store

import cats.effect.IO
import com.example.aiorders.DatabaseSpec
import com.example.aiorders.models.{User, UserId}
import com.example.aiorders.utils.TimeUtils

import java.util.UUID

class PostgresUserStoreSpec extends DatabaseSpec {

  test("create and find user by ID") {
    userStoreResource.use { implicit store =>
      val user = User(
        id = UserId(UUID.randomUUID()),
        email = "test@example.com",
        name = "Test User",
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      for {
        _     <- store.create(user).commit
        found <- store.findById(user.id).commit
      } yield assertEquals(found, Some(user))
    }
  }

  test("find user by email") {
    userStoreResource.use { implicit store =>
      val user = User(
        id = UserId(UUID.randomUUID()),
        email = "email@example.com",
        name = "Email User",
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      for {
        _     <- store.create(user).commit
        found <- store.findByEmail(user.email).commit
      } yield assertEquals(found, Some(user))
    }
  }

  test("user exists") {
    userStoreResource.use { implicit store =>
      val user = User(
        id = UserId(UUID.randomUUID()),
        email = "exists@example.com",
        name = "Exists User",
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      for {
        existsBefore <- store.exists(user.id).commit
        _            <- store.create(user).commit
        existsAfter  <- store.exists(user.id).commit
      } yield {
        assertEquals(existsBefore, false)
        assertEquals(existsAfter, true)
      }
    }
  }

  test("update user") {
    userStoreResource.use { implicit store =>
      val user = User(
        id = UserId(UUID.randomUUID()),
        email = "update@example.com",
        name = "Update User",
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      val updatedUser = user.copy(name = "Updated Name", email = "updated@example.com")

      for {
        _     <- store.create(user).commit
        _     <- store.update(updatedUser).commit
        found <- store.findById(user.id).commit
      } yield assertEquals(found, Some(updatedUser))
    }
  }

  test("delete user") {
    userStoreResource.use { implicit store =>
      val user = User(
        id = UserId(UUID.randomUUID()),
        email = "delete@example.com",
        name = "Delete User",
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      for {
        _           <- store.create(user).commit
        foundBefore <- store.findById(user.id).commit
        _           <- store.delete(user.id).commit
        foundAfter  <- store.findById(user.id).commit
      } yield {
        assertEquals(foundBefore, Some(user))
        assertEquals(foundAfter, None)
      }
    }
  }
}
