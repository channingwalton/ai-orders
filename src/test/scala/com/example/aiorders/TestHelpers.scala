package com.example.aiorders

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import cats.{Id, ~>}
import com.example.aiorders.models.{Order, OrderId, User, UserId}
import com.example.aiorders.services.{OrderService, UserService}
import com.example.aiorders.store.OrderStore

object TestHelpers {

  def createInMemoryStore: IO[OrderStore[IO, Id]] =
    for {
      userRef  <- Ref.of[IO, Map[UserId, User]](Map.empty)
      orderRef <- Ref.of[IO, Map[OrderId, Order]](Map.empty)
    } yield new OrderStore[IO, Id] {
      // Order operations
      def create(order: Order): Id[Unit] = {
        orderRef.update(_.updated(order.id, order)).unsafeRunSync()
        ()
      }
      def findById(orderId: OrderId): Id[Option[Order]] =
        orderRef.get.map(_.get(orderId)).unsafeRunSync()
      def findByUserId(userId: UserId): Id[List[Order]] =
        orderRef.get
          .map(
            _.values.filter(_.userId == userId).toList.sortBy(_.createdAt.toEpochMilli).reverse
          )
          .unsafeRunSync()
      def update(order: Order): Id[Unit] = {
        orderRef.update(_.updated(order.id, order)).unsafeRunSync()
        ()
      }
      def delete(orderId: OrderId): Id[Unit] = {
        orderRef.update(_ - orderId).unsafeRunSync()
        ()
      }
      def exists(orderId: OrderId): Id[Boolean] =
        orderRef.get.map(_.contains(orderId)).unsafeRunSync()

      // User operations
      def createUser(user: User): Id[Unit] = {
        userRef.update(_.updated(user.id, user)).unsafeRunSync()
        ()
      }
      def findUserById(userId: UserId): Id[Option[User]] =
        userRef.get.map(_.get(userId)).unsafeRunSync()
      def findUserByEmail(email: String): Id[Option[User]] =
        userRef.get.map(_.values.find(_.email == email)).unsafeRunSync()
      def updateUser(user: User): Id[Unit] = {
        userRef.update(_.updated(user.id, user)).unsafeRunSync()
        ()
      }
      def deleteUser(userId: UserId): Id[Unit] = {
        userRef.update(_ - userId).unsafeRunSync()
        ()
      }
      def userExists(userId: UserId): Id[Boolean] =
        userRef.get.map(_.contains(userId)).unsafeRunSync()

      // Transaction support
      def commit[A](f: Id[A]): IO[A] = IO.pure(f)
      def lift: cats.arrow.FunctionK[IO, Id] = new (IO ~> Id) {
        def apply[A](fa: IO[A]): Id[A] = fa.unsafeRunSync()(cats.effect.unsafe.implicits.global)
      }
    }

  def createInMemoryUserService: IO[UserService[IO]] =
    createInMemoryStore.map(UserService.withStore(_))

  def createInMemoryOrderService(userService: UserService[IO]): IO[OrderService[IO]] =
    createInMemoryStore.map(OrderService.withStore(_, userService))
}
