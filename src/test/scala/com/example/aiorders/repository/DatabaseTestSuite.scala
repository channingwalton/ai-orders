package com.example.aiorders.repository

import cats.effect.{IO, Resource}
import cats.syntax.all._
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.munit.TestContainerForAll
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import munit.CatsEffectSuite
import org.flywaydb.core.Flyway
import org.testcontainers.utility.DockerImageName

abstract class DatabaseTestSuite extends CatsEffectSuite with TestContainerForAll {

  override val containerDef: PostgreSQLContainer.Def =
    PostgreSQLContainer.Def(dockerImageName = DockerImageName.parse("postgres:16-alpine"))

  def transactorResource(container: PostgreSQLContainer): Resource[IO, HikariTransactor[IO]] =
    for {
      connectEC <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = "org.postgresql.Driver",
        url = container.jdbcUrl,
        user = container.username,
        pass = container.password,
        connectEC = connectEC
      )
    } yield xa

  override def afterContainersStart(container: PostgreSQLContainer): Unit = {
    val flyway = Flyway
      .configure()
      .dataSource(container.jdbcUrl, container.username, container.password)
      .locations("classpath:db/migration")
      .load()

    flyway.migrate(): Unit
  }

  override def beforeEach(context: BeforeEach): Unit = {
    super.beforeEach(context)
    withContainers { container =>
      transactorResource(container).use { xa =>
        import doobie.implicits._
        (sql"DELETE FROM orders".update.run >>
          sql"DELETE FROM products".update.run >>
          sql"DELETE FROM users".update.run).transact(xa).void
      }
    }.unsafeRunSync()
  }

  def withDatabase[A](test: HikariTransactor[IO] => IO[A]): IO[A] =
    withContainers { container =>
      transactorResource(container).use(test)
    }
}
