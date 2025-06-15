package com.example.aiorders.routes

import cats.effect.Concurrent
import cats.syntax.all._
import com.example.aiorders.models.{CreateOrderRequest, OrderListResponse, ServiceError, UserId}
import com.example.aiorders.services.OrderService
import io.circe.syntax._
import io.circe.{DecodingFailure, Json}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, MalformedMessageBodyFailure, Response}

class OrderRoutes[F[_]: Concurrent](orderService: OrderService[F]) extends Http4sDsl[F] {

  implicit val createOrderRequestDecoder: EntityDecoder[F, CreateOrderRequest] =
    jsonOf[F, CreateOrderRequest]
  implicit val orderEncoder: EntityEncoder[F, com.example.aiorders.models.Order] = jsonEncoderOf
  implicit val orderListEncoder: EntityEncoder[F, OrderListResponse]             = jsonEncoderOf

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    case req @ POST -> Root / "orders" =>
      (for {
        createRequest <- req.as[CreateOrderRequest]
        order         <- orderService.createOrder(createRequest)
        response      <- Created(order)
      } yield response).handleErrorWith(handleError)

    case GET -> Root / "orders" / "user" / UUIDVar(userUuid) =>
      val userId = UserId(userUuid)
      (for {
        orders   <- orderService.getOrdersForUser(userId)
        response <- Ok(OrderListResponse(orders))
      } yield response).handleErrorWith(handleError)
  }

  private def handleError(error: Throwable): F[Response[F]] =
    error match {
      case ServiceError.UserNotFound(_) =>
        NotFound(Json.obj("error" -> "User not found".asJson))
      case _: DecodingFailure | _: MalformedMessageBodyFailure =>
        BadRequest(Json.obj("error" -> "Invalid JSON request".asJson))
      case ServiceError.InvalidJsonRequest(reason) =>
        BadRequest(Json.obj("error" -> reason.asJson))
      case ServiceError.JsonEncodingFailure(_) =>
        InternalServerError(Json.obj("error" -> "Failed to encode response".asJson))
      case _ =>
        InternalServerError(Json.obj("error" -> "Internal server error".asJson))
    }

}

object OrderRoutes {
  def apply[F[_]: Concurrent](orderService: OrderService[F]): OrderRoutes[F] =
    new OrderRoutes[F](orderService)
}
