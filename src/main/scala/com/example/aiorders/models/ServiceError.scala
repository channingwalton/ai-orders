package com.example.aiorders.models

import cats.syntax.all._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

sealed trait ServiceError extends Throwable {
  def message: String
  override def getMessage: String = message
}

object ServiceError {
  case class UserNotFound(userId: UserId) extends ServiceError {
    def message: String = s"User with id ${userId.value} not found"
  }

  case class InvalidJsonRequest(reason: String) extends ServiceError {
    def message: String = s"Invalid JSON request: $reason"
  }

  case class JsonEncodingFailure(reason: String) extends ServiceError {
    def message: String = s"Failed to encode response to JSON: $reason"
  }

  implicit val encoder: Encoder[ServiceError] = Encoder.instance {
    case UserNotFound(userId) =>
      deriveEncoder[UserNotFound].apply(UserNotFound(userId))
    case InvalidJsonRequest(reason) =>
      deriveEncoder[InvalidJsonRequest].apply(InvalidJsonRequest(reason))
    case JsonEncodingFailure(reason) =>
      deriveEncoder[JsonEncodingFailure].apply(JsonEncodingFailure(reason))
  }

  implicit val decoder: Decoder[ServiceError] =
    deriveDecoder[UserNotFound].widen[ServiceError] or
      deriveDecoder[InvalidJsonRequest].widen[ServiceError] or
      deriveDecoder[JsonEncodingFailure].widen[ServiceError]
}
