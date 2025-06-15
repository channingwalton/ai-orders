package com.example.aiorders.services

import cats.effect.IO
import com.example.aiorders.models.{CreateOrderRequest, OrderId, ProductId, UserId}
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

  test("createOrder creates a new order with correct details") {
    OrderService.inMemory[IO].flatMap { service =>
      service.createOrder(testRequest).map { order =>
        assertEquals(order.userId, testUserId)
        assertEquals(order.productId, testProductId)
        assertEquals(order.quantity, 2)
        assertEquals(order.totalAmount, BigDecimal("29.99"))
        assert(order.id.isInstanceOf[OrderId])
        assert(order.createdAt != null)
      }
    }
  }

  test("getOrdersForUser returns empty list when no orders exist") {
    OrderService.inMemory[IO].flatMap { service =>
      service.getOrdersForUser(testUserId).map { orders =>
        assertEquals(orders, List.empty)
      }
    }
  }

  test("getOrdersForUser returns orders for specific user") {
    OrderService.inMemory[IO].flatMap { service =>
      val otherUserId  = UserId(UUID.randomUUID())
      val otherRequest = testRequest.copy(userId = otherUserId)

      for {
        order1 <- service.createOrder(testRequest)
        order2 <- service.createOrder(otherRequest)
        order3 <- service.createOrder(testRequest)

        userOrders      <- service.getOrdersForUser(testUserId)
        otherUserOrders <- service.getOrdersForUser(otherUserId)
      } yield {
        assertEquals(userOrders.length, 2)
        assertEquals(otherUserOrders.length, 1)
        assert(userOrders.contains(order1))
        assert(userOrders.contains(order3))
        assert(otherUserOrders.contains(order2))
      }
    }
  }

  test("getOrdersForUser returns orders sorted by creation time (newest first)") {
    OrderService.inMemory[IO].flatMap { service =>
      for {
        order1 <- service.createOrder(testRequest)
        _      <- IO.sleep(scala.concurrent.duration.Duration.fromNanos(1000))
        order2 <- service.createOrder(testRequest)
        _      <- IO.sleep(scala.concurrent.duration.Duration.fromNanos(1000))
        order3 <- service.createOrder(testRequest)

        orders <- service.getOrdersForUser(testUserId)
      } yield {
        assertEquals(orders.length, 3)
        assertEquals(orders.head, order3)
        assertEquals(orders(1), order2)
        assertEquals(orders(2), order1)
      }
    }
  }
}
