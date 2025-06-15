package com.example.aiorders.services

import cats.effect.Ref
import cats.syntax.all._
import com.example.aiorders.models.{CreateOrderRequest, Order, OrderId, ServiceError, UserId}
import com.example.aiorders.utils.TimeUtils

import java.util.UUID

trait OrderService[F[_]] {
  def createOrder(request: CreateOrderRequest): F[Order]
  def getOrdersForUser(userId: UserId): F[List[Order]]
}

class InMemoryOrderService[F[_]](
  storage: Ref[F, Map[OrderId, Order]],
  userService: UserService[F]
)(implicit F: cats.effect.Sync[F])
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
      _ <- storage.update(_.updated(order.id, order))
    } yield order

  def getOrdersForUser(userId: UserId): F[List[Order]] =
    for {
      userExists <- userService.userExists(userId)
      _          <- if (userExists) F.unit else F.raiseError(ServiceError.UserNotFound(userId))
      orders <- storage.get.map(
        _.values.filter(_.userId == userId).toList.sortBy(_.createdAt.toEpochMilli).reverse
      )
    } yield orders
}

object OrderService {
  def inMemory[F[_]: cats.effect.Sync](userService: UserService[F]): F[OrderService[F]] =
    Ref.of[F, Map[OrderId, Order]](Map.empty).map(new InMemoryOrderService[F](_, userService))
}
