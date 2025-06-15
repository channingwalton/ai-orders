package com.example.aiorders

import cats.arrow.FunctionK
import cats.effect.{IO, Resource}
import com.comcast.ip4s.{host, port}
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.munit.TestContainerForAll
import com.example.aiorders.config.{AppConfig, ApplicationConfig, DatabaseConfig, ServerConfig}
import com.example.aiorders.db.DatabaseMigration
import com.example.aiorders.store.{OrderStore, PostgresOrderStore, PostgresUserStore, UserStore}
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import munit.CatsEffectSuite
import org.testcontainers.utility.DockerImageName
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

abstract class DatabaseSpec extends CatsEffectSuite with TestContainerForAll {

  override val containerDef = PostgreSQLContainer.Def(DockerImageName.parse("postgres:15"))

  implicit val logger: StructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def transactorResource: Resource[IO, Transactor[IO]] =
    withContainers { container =>
      val pgContainer = container.asInstanceOf[PostgreSQLContainer]
      val config = DatabaseConfig(
        url = pgContainer.jdbcUrl,
        username = pgContainer.username,
        password = pgContainer.password
      )

      for {
        _  <- Resource.eval(DatabaseMigration.migrate[IO](createAppConfig(config)))
        ce <- doobie.util.ExecutionContexts.fixedThreadPool[IO](32)
        transactor <- HikariTransactor.newHikariTransactor[IO](
          config.driver,
          config.url,
          config.username,
          config.password,
          ce
        )
      } yield transactor
    }

  private def createLift: FunctionK[IO, ConnectionIO] =
    new FunctionK[IO, ConnectionIO] {
      def apply[A](fa: IO[A]): ConnectionIO[A] =
        doobie.free.connection.delay(fa.unsafeRunSync())
    }

  def userStoreResource: Resource[IO, UserStore[IO, ConnectionIO]] =
    transactorResource.map { transactor =>
      new PostgresUserStore[IO](transactor, createLift)
    }

  def orderStoreResource: Resource[IO, OrderStore[IO, ConnectionIO]] =
    transactorResource.map { transactor =>
      new PostgresOrderStore[IO](transactor, createLift)
    }

  implicit class UserStoreOps[A](cio: ConnectionIO[A]) {
    def commit(implicit store: UserStore[IO, ConnectionIO]): IO[A] = store.commit(cio)
  }

  import doobie._
  import doobie.implicits._

  type ConnectionIO[A] = doobie.ConnectionIO[A]

  def cleanDatabase(implicit transactor: Transactor[IO]): IO[Unit] =
    sql"TRUNCATE TABLE orders, users RESTART IDENTITY CASCADE".update.run.transact(transactor).void

  private def createAppConfig(dbConfig: DatabaseConfig) = AppConfig(
    server = ServerConfig(host"localhost", port"5432"),
    application = ApplicationConfig("test", "1.0.0"),
    database = dbConfig
  )
}
