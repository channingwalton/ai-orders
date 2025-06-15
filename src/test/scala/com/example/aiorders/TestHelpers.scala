package com.example.aiorders

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import cats.{Id, ~>}
import com.example.aiorders.models.{Order, OrderId, User, UserId}
import com.example.aiorders.services.{OrderService, UserService}
import com.example.aiorders.store.OrderStore
import com.example.aiorders.utils.TimeUtils

import java.util.UUID

object TestHelpers {

  def createInMemoryUserService: IO[UserService[IO]] =
    for {
      ref <- Ref.of[IO, Map[UserId, User]](Map.empty)
    } yield new UserService[IO] {
      def userExists(userId: UserId): IO[Boolean] =
        ref.get.map(_.contains(userId))

      def createUser(email: String, name: String): IO[User] = {
        val user = User(
          id = UserId(UUID.randomUUID()),
          email = email,
          name = name,
          createdAt = TimeUtils.nowWithSecondPrecision
        )
        ref.update(_.updated(user.id, user)) *> IO.pure(user)
      }

      def getUser(userId: UserId): IO[Option[User]] =
        ref.get.map(_.get(userId))
    }

  def createInMemoryOrderService(userService: UserService[IO]): IO[OrderService[IO]] =
    for {
      ref <- Ref.of[IO, Map[OrderId, Order]](Map.empty)
    } yield {
      val orderStore = new OrderStore[IO, Id] {
        def create(order: Order): Id[Unit] = {
          ref.update(_.updated(order.id, order)).unsafeRunSync()
          ()
        }
        def findById(orderId: OrderId): Id[Option[Order]] =
          ref.get.map(_.get(orderId)).unsafeRunSync()
        def findByUserId(userId: UserId): Id[List[Order]] =
          ref.get
            .map(
              _.values.filter(_.userId == userId).toList.sortBy(_.createdAt.toEpochMilli).reverse
            )
            .unsafeRunSync()
        def update(order: Order): Id[Unit] = {
          ref.update(_.updated(order.id, order)).unsafeRunSync()
          ()
        }
        def delete(orderId: OrderId): Id[Unit] = {
          ref.update(_ - orderId).unsafeRunSync()
          ()
        }
        def exists(orderId: OrderId): Id[Boolean] =
          ref.get.map(_.contains(orderId)).unsafeRunSync()
        def commit[A](f: Id[A]): IO[A] = IO.pure(f)
        def lift: cats.arrow.FunctionK[IO, Id] = new (IO ~> Id) {
          def apply[A](fa: IO[A]): Id[A] = fa.unsafeRunSync()(cats.effect.unsafe.implicits.global)
        }
      }
      OrderService.withStore(orderStore, userService)
    }
}
