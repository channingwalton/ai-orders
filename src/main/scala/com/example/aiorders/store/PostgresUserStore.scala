package com.example.aiorders.store

import cats.arrow.FunctionK
import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.example.aiorders.models.{ServiceError, User, UserId}
import doobie.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.StructuredLogger

class PostgresUserStore[F[_]: StructuredLogger: Async](
  transactor: Transactor[F],
  val lift: FunctionK[F, ConnectionIO]
) extends UserStore[F, ConnectionIO] {

  private val cioUnit: ConnectionIO[Unit] = ().pure[ConnectionIO]

  override def create(user: User): ConnectionIO[Unit] =
    UserStatements.create(user).run.attempt.flatMap {
      case Right(1) => cioUnit
      case Right(n) => databaseError[Unit](s"Expected 1 row inserted but got $n instead")
      case Left(e)  => databaseError("Error creating user", e.some)
    }

  override def findById(userId: UserId): ConnectionIO[Option[User]] =
    UserStatements.findById(userId).option.attempt.flatMap {
      case Right(o) => o.pure[ConnectionIO]
      case Left(e)  => databaseError("Error finding user by ID", e.some)
    }

  override def findByEmail(email: String): ConnectionIO[Option[User]] =
    UserStatements.findByEmail(email).option.attempt.flatMap {
      case Right(o) => o.pure[ConnectionIO]
      case Left(e)  => databaseError("Error finding user by email", e.some)
    }

  override def update(user: User): ConnectionIO[Unit] =
    UserStatements.update(user).run.attempt.flatMap {
      case Right(1) => cioUnit
      case Right(0) => ServiceError.UserNotFound(user.id).raiseError[ConnectionIO, Unit]
      case Right(n) => databaseError[Unit](s"Expected 1 row updated but got $n instead")
      case Left(e)  => databaseError("Error updating user", e.some)
    }

  override def delete(userId: UserId): ConnectionIO[Unit] =
    UserStatements.delete(userId).run.attempt.flatMap {
      case Right(1) => cioUnit
      case Right(0) => ServiceError.UserNotFound(userId).raiseError[ConnectionIO, Unit]
      case Right(n) => databaseError[Unit](s"Expected 1 row deleted but got $n instead")
      case Left(e)  => databaseError("Error deleting user", e.some)
    }

  override def exists(userId: UserId): ConnectionIO[Boolean] =
    UserStatements.exists(userId).unique.attempt.flatMap {
      case Right(v) => v.pure[ConnectionIO]
      case Left(e)  => databaseError(s"Error checking if user exists: $userId", e.some)
    }

  override def commit[A](f: ConnectionIO[A]): F[A] =
    f.transact(transactor)

  private def databaseError[A](msg: String, e: Option[Throwable] = None): ConnectionIO[A] =
    e.fold(lift(StructuredLogger[F].error(msg)))(t => lift(StructuredLogger[F].error(t)(msg))) >>
      ServiceError.DatabaseError(msg).raiseError[ConnectionIO, A]
}

object PostgresUserStore {
  def resource[F[_]: Async: StructuredLogger](
    jdbcUrl: String,
    username: String,
    password: String,
    lift: FunctionK[F, ConnectionIO]
  ): Resource[F, UserStore[F, ConnectionIO]] =
    for {
      ce <- doobie.util.ExecutionContexts.fixedThreadPool[F](32)
      transactor <- HikariTransactor.newHikariTransactor[F](
        "org.postgresql.Driver",
        jdbcUrl,
        username,
        password,
        ce
      )
    } yield new PostgresUserStore(transactor, lift)
}
