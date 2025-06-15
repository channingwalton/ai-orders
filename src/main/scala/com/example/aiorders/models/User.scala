package com.example.aiorders.models

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.time.Instant
import java.util.UUID

final case class User(
  id: UUID,
  name: String,
  email: String,
  createdAt: Instant
)

object User {
  implicit val encoder: Encoder[User] = deriveEncoder
  implicit val decoder: Decoder[User] = deriveDecoder
}
