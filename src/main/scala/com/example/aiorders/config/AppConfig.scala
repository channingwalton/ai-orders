package com.example.aiorders.config

import com.comcast.ip4s.{Host, Port}
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

final case class ServerConfig(
  host: Host,
  port: Port
)

final case class ApplicationConfig(
  name: String,
  version: String
)

final case class AppConfig(
  server: ServerConfig,
  application: ApplicationConfig
) derives ConfigReader

object AppConfig {
  implicit val hostReader: ConfigReader[Host] =
    ConfigReader[String].emap(s =>
      Host.fromString(s).toRight(CannotConvert(s, "Host", s"Invalid host: $s"))
    )

  implicit val portReader: ConfigReader[Port] =
    ConfigReader[Int].emap(i =>
      Port.fromInt(i).toRight(CannotConvert(i.toString, "Port", s"Invalid port: $i"))
    )
}
