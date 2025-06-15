package com.example.aiorders.repository

import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.munit.fixtures.TestContainersFixtures
import com.example.aiorders.models.User
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import munit.CatsEffectSuite
import org.flywaydb.core.Flyway
import org.testcontainers.utility.DockerImageName

import java.time.Instant
import java.util.UUID

class UserRepositorySimpleSpec extends CatsEffectSuite with TestContainersFixtures {

  val postgresContainer = ForAllContainerFixture(
    PostgreSQLContainer
      .Def(dockerImageName = DockerImageName.parse("postgres:16-alpine"))
      .createContainer()
  )

  def withTransactor[A](test: HikariTransactor[IO] => IO[A]): IO[A] = {
    val container = postgresContainer()

    // Setup database
    val flyway = Flyway
      .configure()
      .dataSource(container.jdbcUrl, container.username, container.password)
      .locations("classpath:db/migration")
      .load()
    flyway.migrate()

    // Create transactor and run test
    val resource = for {
      connectEC <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = "org.postgresql.Driver",
        url = container.jdbcUrl,
        user = container.username,
        pass = container.password,
        connectEC = connectEC
      )
    } yield xa

    resource
      .use(test)
      .guarantee(IO {
        flyway.clean(): Unit
      })
  }

  test("UserRepository.create should insert a new user") {
    withTransactor { xa =>
      val repository = UserRepository[IO](xa)
      val userId     = UUID.randomUUID()
      val timestamp  = Instant.now()
      val user       = User(userId, "John Doe", "john.doe@example.com", timestamp)

      for {
        createdUser <- repository.create(user)
        foundUser   <- repository.findById(userId)
      } yield {
        assertEquals(createdUser, user)
        assertEquals(foundUser, Some(user))
      }
    }
  }

  test("UserRepository.findById should return None for non-existent user") {
    withTransactor { xa =>
      val repository    = UserRepository[IO](xa)
      val nonExistentId = UUID.randomUUID()

      repository.findById(nonExistentId).map { result =>
        assertEquals(result, None)
      }
    }
  }

  test("UserRepository.findByEmail should find user by email") {
    withTransactor { xa =>
      val repository = UserRepository[IO](xa)
      val userId     = UUID.randomUUID()
      val timestamp  = Instant.now()
      val user       = User(userId, "Jane Smith", "jane.smith@example.com", timestamp)

      for {
        _         <- repository.create(user)
        foundUser <- repository.findByEmail("jane.smith@example.com")
      } yield assertEquals(foundUser, Some(user))
    }
  }

  test("UserRepository should enforce unique email constraint") {
    withTransactor { xa =>
      val repository = UserRepository[IO](xa)
      val timestamp  = Instant.now()
      val user1      = User(UUID.randomUUID(), "User One", "duplicate@example.com", timestamp)
      val user2      = User(UUID.randomUUID(), "User Two", "duplicate@example.com", timestamp)

      for {
        _      <- repository.create(user1)
        result <- repository.create(user2).attempt
      } yield assert(result.isLeft, "Should fail due to unique email constraint")
    }
  }
}
