package com.example.aiorders.routes

import cats.effect.Concurrent
import cats.syntax.all._
import com.example.aiorders.models.{CreateOrderRequest, OrderListResponse, UserId}
import com.example.aiorders.services.OrderService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Response}

class OrderRoutes[F[_]: Concurrent](orderService: OrderService[F]) extends Http4sDsl[F] {

  implicit val createOrderRequestDecoder: EntityDecoder[F, CreateOrderRequest] =
    jsonOf[F, CreateOrderRequest]
  implicit val orderEncoder: EntityEncoder[F, com.example.aiorders.models.Order] = jsonEncoderOf
  implicit val orderListEncoder: EntityEncoder[F, OrderListResponse]             = jsonEncoderOf

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    case req @ POST -> Root / "orders" =>
      for {
        createRequest <- req.as[CreateOrderRequest]
        order         <- orderService.createOrder(createRequest)
        response      <- Created(order)
      } yield response

    case GET -> Root / "orders" / "user" / UUIDVar(userUuid) =>
      val userId = UserId(userUuid)
      for {
        orders   <- orderService.getOrdersForUser(userId)
        response <- Ok(OrderListResponse(orders))
      } yield response
  }

}

object OrderRoutes {
  def apply[F[_]: Concurrent](orderService: OrderService[F]): OrderRoutes[F] =
    new OrderRoutes[F](orderService)
}
