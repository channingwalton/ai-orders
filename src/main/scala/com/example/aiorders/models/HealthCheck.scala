package com.example.aiorders.models

import io.circe.syntax._
import io.circe.{Encoder, Json}

import java.time.Instant

final case class ApplicationInfo(
  name: String,
  version: String
)

final case class HealthCheck(
  status: String,
  timestamp: Instant,
  application: ApplicationInfo
)

object ApplicationInfo {
  implicit val encoder: Encoder[ApplicationInfo] = new Encoder[ApplicationInfo] {
    final def apply(a: ApplicationInfo): Json = Json.obj(
      "name"    -> a.name.asJson,
      "version" -> a.version.asJson
    )
  }
}

object HealthCheck {
  implicit val encoder: Encoder[HealthCheck] = new Encoder[HealthCheck] {
    final def apply(h: HealthCheck): Json = Json.obj(
      "status"      -> h.status.asJson,
      "timestamp"   -> h.timestamp.toString.asJson,
      "application" -> h.application.asJson
    )
  }
}
