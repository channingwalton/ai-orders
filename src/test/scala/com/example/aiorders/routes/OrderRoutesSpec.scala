package com.example.aiorders.routes

import cats.effect.IO
import com.example.aiorders.models.{CreateOrderRequest, OrderListResponse, ProductId, UserId}
import com.example.aiorders.services.OrderService
import io.circe.syntax._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._

import java.util.UUID

class OrderRoutesSpec extends CatsEffectSuite {

  implicit val orderDecoder: EntityDecoder[IO, com.example.aiorders.models.Order] =
    jsonOf[IO, com.example.aiorders.models.Order]
  implicit val orderListDecoder: EntityDecoder[IO, OrderListResponse] =
    jsonOf[IO, OrderListResponse]

  private val testUserId    = UserId(UUID.randomUUID())
  private val testProductId = ProductId("test-product")
  private val testRequest = CreateOrderRequest(
    userId = testUserId,
    productId = testProductId,
    quantity = 2,
    totalAmount = BigDecimal("29.99")
  )

  test("POST /orders creates a new order") {
    OrderService.inMemory[IO].flatMap { service =>
      val routes = OrderRoutes[IO](service).routes
      val request = Request[IO](Method.POST, uri"/orders")
        .withEntity(testRequest.asJson)

      routes.orNotFound(request).flatMap { response =>
        assertEquals(response.status, Status.Created)
        response.as[com.example.aiorders.models.Order].map { order =>
          assertEquals(order.userId, testUserId)
          assertEquals(order.productId, testProductId)
          assertEquals(order.quantity, 2)
          assertEquals(order.totalAmount, BigDecimal("29.99"))
        }
      }
    }
  }

  test("GET /orders/user/{userId} returns empty list when no orders exist") {
    OrderService.inMemory[IO].flatMap { service =>
      val routes = OrderRoutes[IO](service).routes
      val request =
        Request[IO](Method.GET, Uri.unsafeFromString(s"/orders/user/${testUserId.value}"))

      routes.orNotFound(request).flatMap { response =>
        assertEquals(response.status, Status.Ok)
        response.as[OrderListResponse].map { orderList =>
          assertEquals(orderList.orders, List.empty)
        }
      }
    }
  }

  test("GET /orders/user/{userId} returns orders for user") {
    OrderService.inMemory[IO].flatMap { service =>
      val routes = OrderRoutes[IO](service).routes

      for {
        _ <- service.createOrder(testRequest)
        _ <- service.createOrder(testRequest.copy(quantity = 3))

        request = Request[IO](Method.GET, Uri.unsafeFromString(s"/orders/user/${testUserId.value}"))
        response <- routes.orNotFound(request)

        _ = assertEquals(response.status, Status.Ok)
        orderList <- response.as[OrderListResponse]
      } yield {
        assertEquals(orderList.orders.length, 2)
        assert(orderList.orders.forall(_.userId == testUserId))
      }
    }
  }

  test("GET /orders/user/{userId} with invalid UUID returns 400") {
    OrderService.inMemory[IO].flatMap { service =>
      val routes  = OrderRoutes[IO](service).routes
      val request = Request[IO](Method.GET, uri"/orders/user/invalid-uuid")

      routes.orNotFound(request).map { response =>
        assertEquals(response.status, Status.NotFound)
      }
    }
  }
}
