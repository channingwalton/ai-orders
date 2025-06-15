package com.example.aiorders.models

import io.circe.{Decoder, Encoder}

enum SubscriptionType:
  case MONTHLY, ANNUAL

object SubscriptionType {
  implicit val encoder: Encoder[SubscriptionType] =
    Encoder.encodeString.contramap(_.toString)

  implicit val decoder: Decoder[SubscriptionType] =
    Decoder.decodeString.emap { str =>
      SubscriptionType.values
        .find(_.toString == str)
        .toRight(s"Invalid subscription type: $str")
    }
}
