package com.example.aiorders.services

import cats.effect.Ref
import cats.syntax.all._
import com.example.aiorders.models.{CreateOrderRequest, Order, OrderId, UserId}

import java.time.Instant
import java.util.UUID

trait OrderService[F[_]] {
  def createOrder(request: CreateOrderRequest): F[Order]
  def getOrdersForUser(userId: UserId): F[List[Order]]
}

class InMemoryOrderService[F[_]](storage: Ref[F, Map[OrderId, Order]])(implicit
  F: cats.effect.Sync[F]
) extends OrderService[F] {

  def createOrder(request: CreateOrderRequest): F[Order] = {
    val order = Order(
      id = OrderId(UUID.randomUUID()),
      userId = request.userId,
      productId = request.productId,
      quantity = request.quantity,
      totalAmount = request.totalAmount,
      createdAt = Instant.now()
    )

    storage.update(_.updated(order.id, order)) *> F.pure(order)
  }

  def getOrdersForUser(userId: UserId): F[List[Order]] =
    storage.get.map(
      _.values.filter(_.userId == userId).toList.sortBy(_.createdAt.toEpochMilli).reverse
    )
}

object OrderService {
  def inMemory[F[_]: cats.effect.Sync]: F[OrderService[F]] =
    Ref.of[F, Map[OrderId, Order]](Map.empty).map(new InMemoryOrderService[F](_))
}
