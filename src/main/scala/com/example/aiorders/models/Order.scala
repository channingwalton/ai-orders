package com.example.aiorders.models

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

import java.time.Instant
import java.util.UUID

final case class OrderId(value: UUID)
object OrderId {
  implicit val encoder: Encoder[OrderId] = Encoder.instance(_.value.toString.asJson)
  implicit val decoder: Decoder[OrderId] =
    Decoder.instance(_.as[String].map(s => OrderId(UUID.fromString(s))))
}

final case class UserId(value: UUID)
object UserId {
  implicit val encoder: Encoder[UserId] = Encoder.instance(_.value.toString.asJson)
  implicit val decoder: Decoder[UserId] =
    Decoder.instance(_.as[String].map(s => UserId(UUID.fromString(s))))
}

final case class ProductId(value: String)
object ProductId {
  implicit val encoder: Encoder[ProductId] = deriveEncoder
  implicit val decoder: Decoder[ProductId] = deriveDecoder
}

final case class Order(
  id: OrderId,
  userId: UserId,
  productId: ProductId,
  quantity: Int,
  totalAmount: BigDecimal,
  createdAt: Instant
)

object Order {
  implicit val encoder: Encoder[Order] = deriveEncoder
  implicit val decoder: Decoder[Order] = deriveDecoder
}

final case class CreateOrderRequest(
  userId: UserId,
  productId: ProductId,
  quantity: Int,
  totalAmount: BigDecimal
)

object CreateOrderRequest {
  implicit val encoder: Encoder[CreateOrderRequest] = deriveEncoder
  implicit val decoder: Decoder[CreateOrderRequest] = deriveDecoder
}

final case class OrderListResponse(orders: List[Order])

object OrderListResponse {
  implicit val encoder: Encoder[OrderListResponse] = deriveEncoder
  implicit val decoder: Decoder[OrderListResponse] = deriveDecoder
}
