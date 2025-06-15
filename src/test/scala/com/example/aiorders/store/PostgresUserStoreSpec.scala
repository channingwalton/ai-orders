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
        _     <- store.commit(store.createUser(user))
        found <- store.commit(store.findUserById(user.id))
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
        _     <- store.commit(store.createUser(user))
        found <- store.commit(store.findUserByEmail(user.email))
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
        existsBefore <- store.commit(store.userExists(user.id))
        _            <- store.commit(store.createUser(user))
        existsAfter  <- store.commit(store.userExists(user.id))
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
        _     <- store.commit(store.createUser(user))
        _     <- store.commit(store.updateUser(updatedUser))
        found <- store.commit(store.findUserById(user.id))
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
        _           <- store.commit(store.createUser(user))
        foundBefore <- store.commit(store.findUserById(user.id))
        _           <- store.commit(store.deleteUser(user.id))
        foundAfter  <- store.commit(store.findUserById(user.id))
      } yield {
        assertEquals(foundBefore, Some(user))
        assertEquals(foundAfter, None)
      }
    }
  }
}
