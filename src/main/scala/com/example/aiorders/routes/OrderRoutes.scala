package com.example.aiorders.routes

import cats.effect.Concurrent
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.flatMap._
import com.example.aiorders.models.{CreateOrderRequest, Order, UserOrdersResponse}
import com.example.aiorders.services.{OrderError, OrderService}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Response}
import org.typelevel.log4cats.Logger

object OrderRoutes {
  def apply[F[_]: Concurrent: Logger](orderService: OrderService[F]): HttpRoutes[F] = {
    object dsl extends Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case req @ POST -> Root / "api" / "orders" =>
        req
          .as[CreateOrderRequest]
          .flatMap { createRequest =>
            orderService.createOrder(createRequest).flatMap { order =>
              Created(order)
            }
          }
          .handleErrorWith(handleOrderError)

      case GET -> Root / "api" / "users" / UUIDVar(userId) / "orders" =>
        orderService
          .getUserOrders(userId)
          .flatMap { userOrders =>
            Ok(userOrders)
          }
          .handleErrorWith(handleOrderError)
    }
  }

  private def handleOrderError[F[_]: Concurrent: Logger](error: Throwable): F[Response[F]] = {
    object dsl extends Http4sDsl[F]
    import dsl.*

    error match {
      case OrderError.UserNotFound(_) =>
        Logger[F].warn(s"User not found: ${error.getMessage}") *>
          NotFound(s"""{"error": "${error.getMessage}"}""")

      case OrderError.ProductNotFound(_) =>
        Logger[F].warn(s"Product not found: ${error.getMessage}") *>
          NotFound(s"""{"error": "${error.getMessage}"}""")

      case OrderError.InvalidQuantity(_) =>
        Logger[F].warn(s"Invalid quantity: ${error.getMessage}") *>
          BadRequest(s"""{"error": "${error.getMessage}"}""")

      case _ =>
        Logger[F].error(error)("Unexpected error occurred") *>
          InternalServerError(s"""{"error": "Internal server error"}""")
    }
  }
}
