package com.example.aiorders.store

import cats.arrow.FunctionK
import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.example.aiorders.models.{Order, OrderId, ServiceError, User, UserId}
import doobie.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.StructuredLogger

class PostgresOrderStore[F[_]: StructuredLogger: Async](
  transactor: Transactor[F],
  val lift: FunctionK[F, ConnectionIO]
) extends OrderStore[F, ConnectionIO] {

  private val cioUnit: ConnectionIO[Unit] = ().pure[ConnectionIO]

  override def create(order: Order): ConnectionIO[Unit] =
    OrderStatements.create(order).run.attempt.flatMap {
      case Right(1) => cioUnit
      case Right(n) => databaseError[Unit](s"Expected 1 row inserted but got $n instead")
      case Left(e)  => databaseError("Error creating order", e.some)
    }

  override def findById(orderId: OrderId): ConnectionIO[Option[Order]] =
    OrderStatements.findById(orderId).option.attempt.flatMap {
      case Right(o) => o.pure[ConnectionIO]
      case Left(e)  => databaseError("Error finding order by ID", e.some)
    }

  override def findByUserId(userId: UserId): ConnectionIO[List[Order]] =
    OrderStatements.findByUserId(userId).to[List].attempt.flatMap {
      case Right(orders) => orders.pure[ConnectionIO]
      case Left(e)       => databaseError("Error finding orders by user ID", e.some)
    }

  override def update(order: Order): ConnectionIO[Unit] =
    OrderStatements.update(order).run.attempt.flatMap {
      case Right(1) => cioUnit
      case Right(0) => ServiceError.OrderNotFound(order.id).raiseError[ConnectionIO, Unit]
      case Right(n) => databaseError[Unit](s"Expected 1 row updated but got $n instead")
      case Left(e)  => databaseError("Error updating order", e.some)
    }

  override def delete(orderId: OrderId): ConnectionIO[Unit] =
    OrderStatements.delete(orderId).run.attempt.flatMap {
      case Right(1) => cioUnit
      case Right(0) => ServiceError.OrderNotFound(orderId).raiseError[ConnectionIO, Unit]
      case Right(n) => databaseError[Unit](s"Expected 1 row deleted but got $n instead")
      case Left(e)  => databaseError("Error deleting order", e.some)
    }

  override def exists(orderId: OrderId): ConnectionIO[Boolean] =
    OrderStatements.exists(orderId).unique.attempt.flatMap {
      case Right(v) => v.pure[ConnectionIO]
      case Left(e)  => databaseError(s"Error checking if order exists: $orderId", e.some)
    }

  // User operations
  override def createUser(user: User): ConnectionIO[Unit] =
    OrderStatements.createUser(user).run.attempt.flatMap {
      case Right(1) => cioUnit
      case Right(n) => databaseError[Unit](s"Expected 1 row inserted but got $n instead")
      case Left(e)  => databaseError("Error creating user", e.some)
    }

  override def findUserById(userId: UserId): ConnectionIO[Option[User]] =
    OrderStatements.findUserById(userId).option.attempt.flatMap {
      case Right(o) => o.pure[ConnectionIO]
      case Left(e)  => databaseError("Error finding user by ID", e.some)
    }

  override def findUserByEmail(email: String): ConnectionIO[Option[User]] =
    OrderStatements.findUserByEmail(email).option.attempt.flatMap {
      case Right(o) => o.pure[ConnectionIO]
      case Left(e)  => databaseError("Error finding user by email", e.some)
    }

  override def updateUser(user: User): ConnectionIO[Unit] =
    OrderStatements.updateUser(user).run.attempt.flatMap {
      case Right(1) => cioUnit
      case Right(0) => ServiceError.UserNotFound(user.id).raiseError[ConnectionIO, Unit]
      case Right(n) => databaseError[Unit](s"Expected 1 row updated but got $n instead")
      case Left(e)  => databaseError("Error updating user", e.some)
    }

  override def deleteUser(userId: UserId): ConnectionIO[Unit] =
    OrderStatements.deleteUser(userId).run.attempt.flatMap {
      case Right(1) => cioUnit
      case Right(0) => ServiceError.UserNotFound(userId).raiseError[ConnectionIO, Unit]
      case Right(n) => databaseError[Unit](s"Expected 1 row deleted but got $n instead")
      case Left(e)  => databaseError("Error deleting user", e.some)
    }

  override def userExists(userId: UserId): ConnectionIO[Boolean] =
    OrderStatements.userExists(userId).unique.attempt.flatMap {
      case Right(v) => v.pure[ConnectionIO]
      case Left(e)  => databaseError(s"Error checking if user exists: $userId", e.some)
    }

  override def commit[A](f: ConnectionIO[A]): F[A] =
    f.transact(transactor)

  private def databaseError[A](msg: String, e: Option[Throwable] = None): ConnectionIO[A] =
    e.fold(lift(StructuredLogger[F].error(msg)))(t => lift(StructuredLogger[F].error(t)(msg))) >>
      ServiceError.DatabaseError(msg).raiseError[ConnectionIO, A]
}

object PostgresOrderStore {
  def resource[F[_]: Async: StructuredLogger](
    jdbcUrl: String,
    username: String,
    password: String,
    lift: FunctionK[F, ConnectionIO]
  ): Resource[F, OrderStore[F, ConnectionIO]] =
    for {
      ce <- doobie.util.ExecutionContexts.fixedThreadPool[F](32)
      transactor <- HikariTransactor.newHikariTransactor[F](
        "org.postgresql.Driver",
        jdbcUrl,
        username,
        password,
        ce
      )
    } yield new PostgresOrderStore(transactor, lift)
}
