package com.example.aiorders.models

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.time.Instant
import java.util.UUID

final case class Order(
  id: UUID,
  userId: UUID,
  productId: UUID,
  quantity: Int,
  totalPriceCents: Int,
  createdAt: Instant
)

final case class CreateOrderRequest(
  userId: UUID,
  productId: UUID,
  quantity: Int
)

final case class OrderWithProduct(
  orderId: UUID,
  product: Product,
  quantity: Int,
  totalPriceCents: Int,
  createdAt: Instant
)

final case class UserOrdersResponse(
  userId: UUID,
  orders: List[OrderWithProduct]
)

object Order {
  implicit val encoder: Encoder[Order] = deriveEncoder
  implicit val decoder: Decoder[Order] = deriveDecoder
}

object CreateOrderRequest {
  implicit val encoder: Encoder[CreateOrderRequest] = deriveEncoder
  implicit val decoder: Decoder[CreateOrderRequest] = deriveDecoder
}

object OrderWithProduct {
  implicit val encoder: Encoder[OrderWithProduct] = deriveEncoder
  implicit val decoder: Decoder[OrderWithProduct] = deriveDecoder
}

object UserOrdersResponse {
  implicit val encoder: Encoder[UserOrdersResponse] = deriveEncoder
  implicit val decoder: Decoder[UserOrdersResponse] = deriveDecoder
}
