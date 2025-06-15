package com.example.aiorders.services

import cats.effect.IO
import com.example.aiorders.TestHelpers
import com.example.aiorders.models.{CreateOrderRequest, OrderId, ProductId, ServiceError, UserId}
import munit.CatsEffectSuite

import java.util.UUID

class OrderServiceSpec extends CatsEffectSuite {

  private val testUserId    = UserId(UUID.randomUUID())
  private val testProductId = ProductId("test-product")
  private val testRequest = CreateOrderRequest(
    userId = testUserId,
    productId = testProductId,
    quantity = 2,
    totalAmount = BigDecimal("29.99")
  )

  private def setupServices: IO[(UserService[IO], OrderService[IO], UserId)] =
    for {
      userService  <- TestHelpers.createInMemoryUserService
      orderService <- OrderService.inMemory[IO](userService)
      user         <- userService.createUser("test@example.com", "Test User")
    } yield (userService, orderService, user.id)

  test("createOrder creates a new order with correct details for existing user") {
    setupServices.flatMap { case (_, orderService, validUserId) =>
      val request = testRequest.copy(userId = validUserId)
      orderService.createOrder(request).map { order =>
        assertEquals(order.userId, validUserId)
        assertEquals(order.productId, testProductId)
        assertEquals(order.quantity, 2)
        assertEquals(order.totalAmount, BigDecimal("29.99"))
        assert(order.id.isInstanceOf[OrderId])
        assert(order.createdAt != null)
      }
    }
  }

  test("createOrder fails for non-existent user") {
    setupServices.flatMap { case (_, orderService, _) =>
      orderService.createOrder(testRequest).attempt.map {
        case Left(ServiceError.UserNotFound(userId)) => assertEquals(userId, testUserId)
        case Left(other) => fail(s"Expected UserNotFound error, got: $other")
        case Right(_)    => fail("Expected error for non-existent user")
      }
    }
  }

  test("getOrdersForUser returns empty list when no orders exist for existing user") {
    setupServices.flatMap { case (_, orderService, validUserId) =>
      orderService.getOrdersForUser(validUserId).map { orders =>
        assertEquals(orders, List.empty)
      }
    }
  }

  test("getOrdersForUser fails for non-existent user") {
    setupServices.flatMap { case (_, orderService, _) =>
      orderService.getOrdersForUser(testUserId).attempt.map {
        case Left(ServiceError.UserNotFound(userId)) => assertEquals(userId, testUserId)
        case Left(other) => fail(s"Expected UserNotFound error, got: $other")
        case Right(_)    => fail("Expected error for non-existent user")
      }
    }
  }

  test("getOrdersForUser returns orders for specific user") {
    setupServices.flatMap { case (userService, orderService, user1Id) =>
      for {
        user2 <- userService.createUser("user2@example.com", "User 2")

        request1 = testRequest.copy(userId = user1Id)
        request2 = testRequest.copy(userId = user2.id)

        order1 <- orderService.createOrder(request1)
        order2 <- orderService.createOrder(request2)
        order3 <- orderService.createOrder(request1)

        user1Orders <- orderService.getOrdersForUser(user1Id)
        user2Orders <- orderService.getOrdersForUser(user2.id)
      } yield {
        assertEquals(user1Orders.length, 2)
        assertEquals(user2Orders.length, 1)
        assert(user1Orders.contains(order1))
        assert(user1Orders.contains(order3))
        assert(user2Orders.contains(order2))
      }
    }
  }

  test("getOrdersForUser returns orders sorted by creation time (newest first)") {
    setupServices.flatMap { case (_, orderService, validUserId) =>
      val request = testRequest.copy(userId = validUserId)
      for {
        order1 <- orderService.createOrder(request)
        _      <- IO.sleep(scala.concurrent.duration.Duration.fromNanos(1000))
        order2 <- orderService.createOrder(request)
        _      <- IO.sleep(scala.concurrent.duration.Duration.fromNanos(1000))
        order3 <- orderService.createOrder(request)

        orders <- orderService.getOrdersForUser(validUserId)
      } yield {
        assertEquals(orders.length, 3)
        assertEquals(orders.head, order3)
        assertEquals(orders(1), order2)
        assertEquals(orders(2), order1)
      }
    }
  }
}
