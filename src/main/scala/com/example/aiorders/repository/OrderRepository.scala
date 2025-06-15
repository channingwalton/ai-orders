package com.example.aiorders.repository

import cats.effect.Sync
import cats.syntax.functor._
import com.example.aiorders.models.{Order, OrderWithProduct, Product, SubscriptionType}
import com.example.aiorders.repository.ProductRepository.subscriptionTypeRead
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.{Read, Write}

import java.time.Instant
import java.util.UUID

trait OrderRepository[F[_]] {
  def create(order: Order): F[Order]
  def findByUserId(userId: UUID): F[List[OrderWithProduct]]
  def findById(id: UUID): F[Option[Order]]
}

object OrderRepository {

  implicit val orderRead: Read[Order] =
    Read[(UUID, UUID, UUID, Int, Int, Instant)].map {
      case (id, userId, productId, quantity, totalPriceCents, createdAt) =>
        Order(id, userId, productId, quantity, totalPriceCents, createdAt)
    }

  implicit val orderWrite: Write[Order] =
    Write[(UUID, UUID, UUID, Int, Int, Instant)].contramap { order =>
      (
        order.id,
        order.userId,
        order.productId,
        order.quantity,
        order.totalPriceCents,
        order.createdAt
      )
    }

  implicit val orderWithProductRead: Read[OrderWithProduct] =
    Read[(UUID, UUID, String, SubscriptionType, Int, Instant, Int, Int, Instant)].map {
      case (
            orderId,
            productId,
            productName,
            subscriptionType,
            priceCents,
            productCreatedAt,
            quantity,
            totalPriceCents,
            orderCreatedAt
          ) =>
        val product =
          Product(productId, productName, subscriptionType, priceCents, productCreatedAt)
        OrderWithProduct(orderId, product, quantity, totalPriceCents, orderCreatedAt)
    }

  def apply[F[_]: Sync](xa: doobie.Transactor[F]): OrderRepository[F] =
    new OrderRepository[F] {

      def create(order: Order): F[Order] =
        sql"INSERT INTO orders (id, user_id, product_id, quantity, total_price_cents, created_at) VALUES (${order.id}, ${order.userId}, ${order.productId}, ${order.quantity}, ${order.totalPriceCents}, ${order.createdAt})".update.run
          .transact(xa)
          .as(order)

      def findByUserId(userId: UUID): F[List[OrderWithProduct]] =
        sql"""
          SELECT o.id, p.id, p.name, p.subscription_type, p.price_cents, p.created_at, o.quantity, o.total_price_cents, o.created_at
          FROM orders o
          JOIN products p ON o.product_id = p.id
          WHERE o.user_id = $userId
          ORDER BY o.created_at DESC
        """
          .query[OrderWithProduct]
          .stream
          .compile
          .toList
          .transact(xa)

      def findById(id: UUID): F[Option[Order]] =
        sql"SELECT id, user_id, product_id, quantity, total_price_cents, created_at FROM orders WHERE id = $id"
          .query[Order]
          .option
          .transact(xa)
    }
}
