package com.example.aiorders

import cats.effect.{IO, Resource}
import com.example.aiorders.config.AppConfig
import com.example.aiorders.models.ApplicationInfo
import com.example.aiorders.routes.HealthRoutes
import com.example.aiorders.services.HealthService
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource

object AiOrdersApp {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def server(config: AppConfig): Resource[IO, Server] = {
    val appInfo       = ApplicationInfo(config.application.name, config.application.version)
    val healthService = HealthService[IO](appInfo)
    val healthRoutes  = HealthRoutes[IO](healthService)

    val httpApp: HttpApp[IO] = healthRoutes.orNotFound

    for {
      _ <- Resource.eval(
        logger.info(s"Starting server on ${config.server.host}:${config.server.port}")
      )
      server <- EmberServerBuilder
        .default[IO]
        .withHost(config.server.host)
        .withPort(config.server.port)
        .withHttpApp(httpApp)
        .build
      _ <- Resource.eval(
        logger.info(s"Server started successfully on ${config.server.host}:${config.server.port}")
      )
    } yield server
  }

  def loadConfig: IO[AppConfig] =
    IO.fromEither(
      ConfigSource.default
        .load[AppConfig]
        .left
        .map(failures =>
          new RuntimeException(s"Failed to load configuration: ${failures.toList.mkString(", ")}")
        )
    )

  val serverResource: Resource[IO, Server] =
    for {
      config <- Resource.eval(loadConfig)
      server <- server(config)
    } yield server
}
