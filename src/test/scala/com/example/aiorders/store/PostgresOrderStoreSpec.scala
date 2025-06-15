package com.example.aiorders.store

import cats.effect.IO
import cats.syntax.all._
import com.example.aiorders.DatabaseSpec
import com.example.aiorders.models.{Order, OrderId, ProductId, User, UserId}
import com.example.aiorders.utils.TimeUtils

import java.util.UUID

class PostgresOrderStoreSpec extends DatabaseSpec {

  test("create and find order by ID") {
    (userStoreResource, orderStoreResource).tupled.use { case (userStore, orderStore) =>
      val user = User(
        id = UserId(UUID.randomUUID()),
        email = "test@example.com",
        name = "Test User",
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      val order = Order(
        id = OrderId(UUID.randomUUID()),
        userId = user.id,
        productId = ProductId("test-product"),
        quantity = 2,
        totalAmount = BigDecimal("29.99"),
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      for {
        _     <- userStore.commit(userStore.create(user))
        _     <- orderStore.commit(orderStore.create(order))
        found <- orderStore.commit(orderStore.findById(order.id))
      } yield assertEquals(found, Some(order))
    }
  }

  test("find orders by user ID") {
    (userStoreResource, orderStoreResource).tupled.use { case (userStore, orderStore) =>
      val user = User(
        id = UserId(UUID.randomUUID()),
        email = "orders@example.com",
        name = "Orders User",
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      val order1 = Order(
        id = OrderId(UUID.randomUUID()),
        userId = user.id,
        productId = ProductId("product-1"),
        quantity = 1,
        totalAmount = BigDecimal("19.99"),
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      val order2 = Order(
        id = OrderId(UUID.randomUUID()),
        userId = user.id,
        productId = ProductId("product-2"),
        quantity = 3,
        totalAmount = BigDecimal("59.97"),
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      for {
        _      <- userStore.commit(userStore.create(user))
        _      <- orderStore.commit(orderStore.create(order1))
        _      <- orderStore.commit(orderStore.create(order2))
        orders <- orderStore.commit(orderStore.findByUserId(user.id))
      } yield {
        assertEquals(orders.length, 2)
        assert(orders.contains(order1))
        assert(orders.contains(order2))
        // Orders should be sorted by created_at DESC
        assertEquals(orders.head.createdAt.compareTo(orders.last.createdAt) >= 0, true)
      }
    }
  }

  test("order exists") {
    (userStoreResource, orderStoreResource).tupled.use { case (userStore, orderStore) =>
      val user = User(
        id = UserId(UUID.randomUUID()),
        email = "exists@example.com",
        name = "Exists User",
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      val order = Order(
        id = OrderId(UUID.randomUUID()),
        userId = user.id,
        productId = ProductId("exists-product"),
        quantity = 1,
        totalAmount = BigDecimal("9.99"),
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      for {
        existsBefore <- orderStore.commit(orderStore.exists(order.id))
        _            <- userStore.commit(userStore.create(user))
        _            <- orderStore.commit(orderStore.create(order))
        existsAfter  <- orderStore.commit(orderStore.exists(order.id))
      } yield {
        assertEquals(existsBefore, false)
        assertEquals(existsAfter, true)
      }
    }
  }

  test("update order") {
    (userStoreResource, orderStoreResource).tupled.use { case (userStore, orderStore) =>
      val user = User(
        id = UserId(UUID.randomUUID()),
        email = "update@example.com",
        name = "Update User",
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      val order = Order(
        id = OrderId(UUID.randomUUID()),
        userId = user.id,
        productId = ProductId("update-product"),
        quantity = 1,
        totalAmount = BigDecimal("19.99"),
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      val updatedOrder = order.copy(
        productId = ProductId("updated-product"),
        quantity = 5,
        totalAmount = BigDecimal("99.95")
      )

      for {
        _     <- userStore.commit(userStore.create(user))
        _     <- orderStore.commit(orderStore.create(order))
        _     <- orderStore.commit(orderStore.update(updatedOrder))
        found <- orderStore.commit(orderStore.findById(order.id))
      } yield assertEquals(found, Some(updatedOrder))
    }
  }

  test("delete order") {
    (userStoreResource, orderStoreResource).tupled.use { case (userStore, orderStore) =>
      val user = User(
        id = UserId(UUID.randomUUID()),
        email = "delete@example.com",
        name = "Delete User",
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      val order = Order(
        id = OrderId(UUID.randomUUID()),
        userId = user.id,
        productId = ProductId("delete-product"),
        quantity = 1,
        totalAmount = BigDecimal("14.99"),
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      for {
        _           <- userStore.commit(userStore.create(user))
        _           <- orderStore.commit(orderStore.create(order))
        foundBefore <- orderStore.commit(orderStore.findById(order.id))
        _           <- orderStore.commit(orderStore.delete(order.id))
        foundAfter  <- orderStore.commit(orderStore.findById(order.id))
      } yield {
        assertEquals(foundBefore, Some(order))
        assertEquals(foundAfter, None)
      }
    }
  }

  test("find orders by user ID returns empty list for user with no orders") {
    (userStoreResource, orderStoreResource).tupled.use { case (userStore, orderStore) =>
      val user = User(
        id = UserId(UUID.randomUUID()),
        email = "noorders@example.com",
        name = "No Orders User",
        createdAt = TimeUtils.nowWithSecondPrecision
      )

      for {
        _      <- userStore.commit(userStore.create(user))
        orders <- orderStore.commit(orderStore.findByUserId(user.id))
      } yield assertEquals(orders, List.empty)
    }
  }
}
