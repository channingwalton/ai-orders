package com.example.aiorders.services

import com.example.aiorders.TestHelpers
import com.example.aiorders.models.{User, UserId}
import munit.CatsEffectSuite

import java.util.UUID

class UserServiceSpec extends CatsEffectSuite {

  private val testUserId = UserId(UUID.randomUUID())

  test("userExists returns false for non-existent user") {
    for {
      store <- TestHelpers.createInMemoryStore
      service = TestHelpers.createInMemoryUserService(store)
      exists <- store.commit(service.userExists(testUserId))
    } yield assertEquals(exists, false)
  }

  test("createUser creates a new user") {
    for {
      store <- TestHelpers.createInMemoryStore
      service = TestHelpers.createInMemoryUserService(store)
      user <- store.commit(service.createUser("test@example.com", "Test User"))
    } yield {
      assertEquals(user.email, "test@example.com")
      assertEquals(user.name, "Test User")
      assert(user.id != null)
      assert(user.createdAt != null)
    }
  }

  test("userExists returns true for existing user") {
    for {
      store <- TestHelpers.createInMemoryStore
      service = TestHelpers.createInMemoryUserService(store)
      createdUser <- store.commit(service.createUser("test@example.com", "Test User"))
      exists      <- store.commit(service.userExists(createdUser.id))
    } yield assertEquals(exists, true)
  }

  test("getUser returns None for non-existent user") {
    for {
      store <- TestHelpers.createInMemoryStore
      service = TestHelpers.createInMemoryUserService(store)
      userOpt <- store.commit(service.getUser(testUserId))
    } yield assertEquals(userOpt, None)
  }

  test("getUser returns Some(user) for existing user") {
    for {
      store <- TestHelpers.createInMemoryStore
      service = TestHelpers.createInMemoryUserService(store)
      createdUser <- store.commit(service.createUser("test@example.com", "Test User"))
      userOpt     <- store.commit(service.getUser(createdUser.id))
    } yield assertEquals(userOpt, Some(createdUser))
  }
}
