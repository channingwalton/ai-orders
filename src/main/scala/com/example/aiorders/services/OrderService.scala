package com.example.aiorders.services

import cats.effect.MonadCancelThrow
import cats.syntax.all._
import com.example.aiorders.models.{CreateOrderRequest, Order, OrderId, ServiceError, UserId}
import com.example.aiorders.store.OrderStore
import com.example.aiorders.utils.TimeUtils

import java.util.UUID

trait OrderService[F[_]] {
  def createOrder(request: CreateOrderRequest): F[Order]
  def getOrdersForUser(userId: UserId): F[List[Order]]
}

class DatabaseOrderService[F[_], G[_]](
  store: OrderStore[F, G],
  userService: UserService[F]
)(implicit F: MonadCancelThrow[F])
    extends OrderService[F] {

  def createOrder(request: CreateOrderRequest): F[Order] =
    for {
      userExists <- userService.userExists(request.userId)
      _ <- if (userExists) F.unit else F.raiseError(ServiceError.UserNotFound(request.userId))
      order = Order(
        id = OrderId(UUID.randomUUID()),
        userId = request.userId,
        productId = request.productId,
        quantity = request.quantity,
        totalAmount = request.totalAmount,
        createdAt = TimeUtils.nowWithSecondPrecision
      )
      _ <- store.commit(store.create(order))
    } yield order

  def getOrdersForUser(userId: UserId): F[List[Order]] =
    for {
      userExists <- userService.userExists(userId)
      _          <- if (userExists) F.unit else F.raiseError(ServiceError.UserNotFound(userId))
      orders     <- store.commit(store.findByUserId(userId))
    } yield orders
}

object OrderService {
  def withStore[F[_]: MonadCancelThrow, G[_]](
    store: OrderStore[F, G],
    userService: UserService[F]
  ): OrderService[F] =
    new DatabaseOrderService[F, G](store, userService)
}
