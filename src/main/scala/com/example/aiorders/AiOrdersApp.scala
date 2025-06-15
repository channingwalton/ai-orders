package com.example.aiorders

import cats.effect.{IO, Resource}
import cats.syntax.all._
import com.example.aiorders.config.AppConfig
import com.example.aiorders.db.DatabaseMigration
import com.example.aiorders.models.ApplicationInfo
import com.example.aiorders.routes.{HealthRoutes, OrderRoutes}
import com.example.aiorders.services.{HealthService, OrderService, UserService}
import com.example.aiorders.store.PostgresOrderStore
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, StructuredLogger}
import pureconfig.ConfigSource

object AiOrdersApp {

  implicit val logger: Logger[IO]                     = Slf4jLogger.getLogger[IO]
  implicit val structuredLogger: StructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def server(config: AppConfig): Resource[IO, Server] =
    server(config, runMigrations = true)

  def server(config: AppConfig, runMigrations: Boolean): Resource[IO, Server] = {
    val appInfo       = ApplicationInfo(config.application.name, config.application.version)
    val healthService = HealthService[IO](appInfo)
    val healthRoutes  = HealthRoutes[IO](healthService)

    doobie.WeakAsync.liftK[IO, doobie.ConnectionIO].flatMap { lift =>
      for {
        _ <-
          if (runMigrations) Resource.eval(DatabaseMigration.migrate[IO](config))
          else Resource.pure(())
        store <- PostgresOrderStore.resource[IO](
          config.database.url,
          config.database.username,
          config.database.password,
          lift
        )
        userService  = UserService.withStore(store)
        orderService = OrderService.withStore(store, userService)
        orderRoutes  = OrderRoutes[IO](orderService)

        allRoutes            = healthRoutes <+> orderRoutes.routes
        httpApp: HttpApp[IO] = allRoutes.orNotFound

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
