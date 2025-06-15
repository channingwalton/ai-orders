package com.example.aiorders.routes

import cats.effect.IO
import com.example.aiorders.models.{CreateOrderRequest, OrderListResponse, ProductId, UserId}
import com.example.aiorders.services.{OrderService, UserService}
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
    for {
      userService  <- UserService.inMemory[IO]
      user         <- userService.createUser("test@example.com", "Test User")
      orderService <- OrderService.inMemory[IO](userService)

      routes       = OrderRoutes[IO](orderService).routes
      validRequest = testRequest.copy(userId = user.id)
      request = Request[IO](Method.POST, uri"/orders")
        .withEntity(validRequest.asJson)

      response <- routes.orNotFound(request)
      _ = assertEquals(response.status, Status.Created)
      order <- response.as[com.example.aiorders.models.Order]
    } yield {
      assertEquals(order.userId, user.id)
      assertEquals(order.productId, testProductId)
      assertEquals(order.quantity, 2)
      assertEquals(order.totalAmount, BigDecimal("29.99"))
    }
  }

  test("GET /orders/user/{userId} returns empty list when no orders exist") {
    for {
      userService  <- UserService.inMemory[IO]
      user         <- userService.createUser("test@example.com", "Test User")
      orderService <- OrderService.inMemory[IO](userService)

      routes  = OrderRoutes[IO](orderService).routes
      request = Request[IO](Method.GET, Uri.unsafeFromString(s"/orders/user/${user.id.value}"))

      response <- routes.orNotFound(request)
      _ = assertEquals(response.status, Status.Ok)
      orderList <- response.as[OrderListResponse]
    } yield assertEquals(orderList.orders, List.empty)
  }

  test("GET /orders/user/{userId} returns orders for user") {
    for {
      userService  <- UserService.inMemory[IO]
      user         <- userService.createUser("test@example.com", "Test User")
      orderService <- OrderService.inMemory[IO](userService)

      routes        = OrderRoutes[IO](orderService).routes
      validRequest1 = testRequest.copy(userId = user.id)
      validRequest2 = testRequest.copy(userId = user.id, quantity = 3)

      _ <- orderService.createOrder(validRequest1)
      _ <- orderService.createOrder(validRequest2)

      request = Request[IO](Method.GET, Uri.unsafeFromString(s"/orders/user/${user.id.value}"))
      response <- routes.orNotFound(request)

      _ = assertEquals(response.status, Status.Ok)
      orderList <- response.as[OrderListResponse]
    } yield {
      assertEquals(orderList.orders.length, 2)
      assert(orderList.orders.forall(_.userId == user.id))
    }
  }

  test("GET /orders/user/{userId} with invalid UUID returns 404") {
    for {
      userService  <- UserService.inMemory[IO]
      orderService <- OrderService.inMemory[IO](userService)

      routes  = OrderRoutes[IO](orderService).routes
      request = Request[IO](Method.GET, uri"/orders/user/invalid-uuid")

      response <- routes.orNotFound(request)
    } yield assertEquals(response.status, Status.NotFound)
  }
}
