package com.example.aiorders.repository

import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.example.aiorders.models.User
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import munit.CatsEffectSuite
import org.flywaydb.core.Flyway
import org.testcontainers.utility.DockerImageName

import java.time.Instant
import java.util.UUID

class UserRepositoryContainerSpec extends CatsEffectSuite {

  val container = PostgreSQLContainer
    .Def(dockerImageName = DockerImageName.parse("postgres:16-alpine"))
    .createContainer()

  // Start container before tests
  override def beforeAll(): Unit = {
    super.beforeAll()
    container.start()
  }

  // Stop container after tests
  override def afterAll(): Unit = {
    super.afterAll()
    container.stop()
  }

  def transactorResource =
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

  override def beforeEach(context: BeforeEach): Unit = {
    super.beforeEach(context)

    // Run migration
    val flyway = Flyway
      .configure()
      .dataSource(container.jdbcUrl, container.username, container.password)
      .locations("classpath:db/migration")
      .load()
    flyway.migrate(): Unit
  }

  override def afterEach(context: AfterEach): Unit = {
    super.afterEach(context)

    // Clean database
    val flyway = Flyway
      .configure()
      .dataSource(container.jdbcUrl, container.username, container.password)
      .locations("classpath:db/migration")
      .load()
    flyway.clean(): Unit
  }

  test("UserRepository.create should insert a new user") {
    transactorResource.use { xa =>
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
    transactorResource.use { xa =>
      val repository    = UserRepository[IO](xa)
      val nonExistentId = UUID.randomUUID()

      repository.findById(nonExistentId).map { result =>
        assertEquals(result, None)
      }
    }
  }

  test("UserRepository should enforce unique email constraint") {
    transactorResource.use { xa =>
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
