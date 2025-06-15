package com.example.aiorders.models

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.time.Instant
import java.util.UUID

final case class Product(
  id: UUID,
  name: String,
  subscriptionType: SubscriptionType,
  priceCents: Int,
  createdAt: Instant
)

object Product {
  implicit val encoder: Encoder[Product] = deriveEncoder
  implicit val decoder: Decoder[Product] = deriveDecoder
}
