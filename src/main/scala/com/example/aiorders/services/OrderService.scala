package com.example.aiorders.services

import cats.MonadError
import cats.syntax.all._
import com.example.aiorders.models.{CreateOrderRequest, Order, OrderId, ServiceError, UserId}
import com.example.aiorders.store.OrderStore
import com.example.aiorders.utils.TimeUtils

import java.util.UUID

trait OrderService[G[_]] {
  def createOrder(request: CreateOrderRequest): G[Order]
  def getOrdersForUser(userId: UserId): G[List[Order]]
}

class DatabaseOrderService[F[_], G[_]](
  store: OrderStore[F, G],
  userService: UserService[G]
)(implicit G: MonadError[G, Throwable])
    extends OrderService[G] {

  def createOrder(request: CreateOrderRequest): G[Order] =
    for {
      userExists <- userService.userExists(request.userId)
      _ <- if (userExists) G.unit else G.raiseError(ServiceError.UserNotFound(request.userId))
      order = Order(
        id = OrderId(UUID.randomUUID()),
        userId = request.userId,
        productId = request.productId,
        quantity = request.quantity,
        totalAmount = request.totalAmount,
        createdAt = TimeUtils.nowWithSecondPrecision
      )
      _ <- store.create(order)
    } yield order

  def getOrdersForUser(userId: UserId): G[List[Order]] =
    for {
      userExists <- userService.userExists(userId)
      _          <- if (userExists) G.unit else G.raiseError(ServiceError.UserNotFound(userId))
      orders     <- store.findByUserId(userId)
    } yield orders
}

object OrderService {
  def withStore[F[_], G[_]](
    store: OrderStore[F, G],
    userService: UserService[G]
  )(implicit G: MonadError[G, Throwable]): OrderService[G] =
    new DatabaseOrderService[F, G](store, userService)
}
