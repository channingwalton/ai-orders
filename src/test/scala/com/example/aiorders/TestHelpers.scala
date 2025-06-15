package com.example.aiorders

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import cats.~>
import com.example.aiorders.models.{Order, OrderId, User, UserId}
import com.example.aiorders.services.{OrderService, UserService}
import com.example.aiorders.store.OrderStore

object TestHelpers {

  type TestEither[A] = Either[Throwable, A]

  def createInMemoryStore: IO[OrderStore[IO, TestEither]] =
    for {
      userRef  <- Ref.of[IO, Map[UserId, User]](Map.empty)
      orderRef <- Ref.of[IO, Map[OrderId, Order]](Map.empty)
    } yield new OrderStore[IO, TestEither] {
      // Order operations
      def create(order: Order): TestEither[Unit] = {
        orderRef.update(_.updated(order.id, order)).unsafeRunSync()
        Right(())
      }
      def findById(orderId: OrderId): TestEither[Option[Order]] =
        Right(orderRef.get.map(_.get(orderId)).unsafeRunSync())
      def findByUserId(userId: UserId): TestEither[List[Order]] =
        Right(
          orderRef.get
            .map(
              _.values.filter(_.userId == userId).toList.sortBy(_.createdAt.toEpochMilli).reverse
            )
            .unsafeRunSync()
        )
      def update(order: Order): TestEither[Unit] = {
        orderRef.update(_.updated(order.id, order)).unsafeRunSync()
        Right(())
      }
      def delete(orderId: OrderId): TestEither[Unit] = {
        orderRef.update(_ - orderId).unsafeRunSync()
        Right(())
      }
      def exists(orderId: OrderId): TestEither[Boolean] =
        Right(orderRef.get.map(_.contains(orderId)).unsafeRunSync())

      // User operations
      def createUser(user: User): TestEither[Unit] = {
        userRef.update(_.updated(user.id, user)).unsafeRunSync()
        Right(())
      }
      def findUserById(userId: UserId): TestEither[Option[User]] =
        Right(userRef.get.map(_.get(userId)).unsafeRunSync())
      def findUserByEmail(email: String): TestEither[Option[User]] =
        Right(userRef.get.map(_.values.find(_.email == email)).unsafeRunSync())
      def updateUser(user: User): TestEither[Unit] = {
        userRef.update(_.updated(user.id, user)).unsafeRunSync()
        Right(())
      }
      def deleteUser(userId: UserId): TestEither[Unit] = {
        userRef.update(_ - userId).unsafeRunSync()
        Right(())
      }
      def userExists(userId: UserId): TestEither[Boolean] =
        Right(userRef.get.map(_.contains(userId)).unsafeRunSync())

      // Transaction support
      def commit[A](f: TestEither[A]): IO[A] = f match {
        case Left(error)  => IO.raiseError(error)
        case Right(value) => IO.pure(value)
      }
      def lift: cats.arrow.FunctionK[IO, TestEither] = new (IO ~> TestEither) {
        def apply[A](fa: IO[A]): TestEither[A] =
          try Right(fa.unsafeRunSync())
          catch { case t: Throwable => Left(t) }
      }
    }

  def createInMemoryUserService(store: OrderStore[IO, TestEither]): UserService[TestEither] =
    UserService.withStore(store)

  def createInMemoryOrderService(
    store: OrderStore[IO, TestEither],
    userService: UserService[TestEither]
  ): OrderService[TestEither] =
    OrderService.withStore(store, userService)
}
