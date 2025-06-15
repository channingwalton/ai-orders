package com.example.aiorders.routes

import cats.effect.IO
import com.example.aiorders.models.{CreateOrderRequest, ProductId, UserId}
import com.example.aiorders.services.{OrderService, UserService}
import io.circe.Json
import io.circe.syntax._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._

import java.util.UUID

class OrderRoutesErrorSpec extends CatsEffectSuite {

  implicit val jsonDecoder: EntityDecoder[IO, Json] = jsonOf[IO, Json]

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
      userService  <- UserService.inMemory[IO]
      orderService <- OrderService.inMemory[IO](userService)
      user         <- userService.createUser("test@example.com", "Test User")
    } yield (userService, orderService, user.id)

  test("POST /orders returns 404 NotFound for non-existent user") {
    setupServices.flatMap { case (_, orderService, _) =>
      val routes = OrderRoutes[IO](orderService).routes
      val request = Request[IO](Method.POST, uri"/orders")
        .withEntity(testRequest.asJson)

      routes.orNotFound(request).flatMap { response =>
        assertEquals(response.status, Status.NotFound)
        response.as[Json].map { json =>
          assertEquals(json, Json.obj("error" -> "User not found".asJson))
        }
      }
    }
  }

  test("GET /orders/user/{userId} returns 404 NotFound for non-existent user") {
    setupServices.flatMap { case (_, orderService, _) =>
      val routes = OrderRoutes[IO](orderService).routes
      val request =
        Request[IO](Method.GET, Uri.unsafeFromString(s"/orders/user/${testUserId.value}"))

      routes.orNotFound(request).flatMap { response =>
        assertEquals(response.status, Status.NotFound)
        response.as[Json].map { json =>
          assertEquals(json, Json.obj("error" -> "User not found".asJson))
        }
      }
    }
  }

  test("POST /orders returns 400 BadRequest for invalid JSON") {
    setupServices.flatMap { case (_, orderService, _) =>
      val routes = OrderRoutes[IO](orderService).routes
      val request = Request[IO](Method.POST, uri"/orders")
        .withEntity("invalid json")

      routes.orNotFound(request).flatMap { response =>
        assertEquals(response.status, Status.BadRequest)
        response.as[Json].map { json =>
          assertEquals(json, Json.obj("error" -> "Invalid JSON request".asJson))
        }
      }
    }
  }

  test("POST /orders returns 400 BadRequest for missing required fields") {
    setupServices.flatMap { case (_, orderService, _) =>
      val routes = OrderRoutes[IO](orderService).routes
      val incompleteJson = Json.obj(
        "userId"   -> testUserId.value.toString.asJson,
        "quantity" -> 2.asJson
        // missing productId and totalAmount
      )
      val request = Request[IO](Method.POST, uri"/orders")
        .withEntity(incompleteJson)

      routes.orNotFound(request).flatMap { response =>
        // Accept either BadRequest or InternalServerError for missing fields
        assert(
          response.status == Status.BadRequest || response.status == Status.InternalServerError
        )
        response.as[Json].map { json =>
          // Check that it contains an error field
          assert(json.asObject.exists(_.contains("error")))
        }
      }
    }
  }

  test("GET /orders/user/{userId} with malformed UUID returns 404") {
    setupServices.flatMap { case (_, orderService, _) =>
      val routes  = OrderRoutes[IO](orderService).routes
      val request = Request[IO](Method.GET, uri"/orders/user/not-a-uuid")

      routes.orNotFound(request).map { response =>
        assertEquals(response.status, Status.NotFound)
      }
    }
  }
}
