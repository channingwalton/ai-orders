package com.example.aiorders

import cats.effect.{IO, Resource}
import cats.syntax.semigroupk._
import com.example.aiorders.config.AppConfig
import com.example.aiorders.models.ApplicationInfo
import com.example.aiorders.repository.{OrderRepository, ProductRepository, UserRepository}
import com.example.aiorders.routes.{HealthRoutes, OrderRoutes}
import com.example.aiorders.services.{HealthService, OrderService}
import doobie.hikari.HikariTransactor
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource

object AiOrdersApp {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def createTransactor(config: AppConfig): Resource[IO, HikariTransactor[IO]] =
    for {
      connectEC <- Resource.eval(IO.executionContext)
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = config.database.driver,
        url = config.database.url,
        user = config.database.user,
        pass = config.database.password,
        connectEC = connectEC
      )
    } yield xa

  def server(config: AppConfig): Resource[IO, Server] =
    for {
      _  <- Resource.eval(logger.info("Initializing database connection"))
      xa <- createTransactor(config)
      _  <- Resource.eval(logger.info("Database connection established"))

      // Initialize repositories
      userRepository    = UserRepository[IO](xa)
      productRepository = ProductRepository[IO](xa)
      orderRepository   = OrderRepository[IO](xa)

      // Initialize services
      appInfo       = ApplicationInfo(config.application.name, config.application.version)
      healthService = HealthService[IO](appInfo)
      orderService  = OrderService[IO](userRepository, productRepository, orderRepository)

      // Initialize routes
      healthRoutes = HealthRoutes[IO](healthService)
      orderRoutes  = OrderRoutes[IO](orderService)

      // Combine routes
      httpApp: HttpApp[IO] = (healthRoutes <+> orderRoutes).orNotFound

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
