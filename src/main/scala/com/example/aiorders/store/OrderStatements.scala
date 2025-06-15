package com.example.aiorders.store

import com.example.aiorders.models.{Order, OrderId, ProductId, UserId}
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

import java.time.Instant
import java.util.UUID

object OrderStatements {

  implicit val orderIdMeta: Meta[OrderId]     = Meta[UUID].timap(OrderId.apply)(_.value)
  implicit val productIdMeta: Meta[ProductId] = Meta[String].timap(ProductId.apply)(_.value)

  def create(order: Order): Update0 =
    sql"""
      INSERT INTO orders (id, user_id, product_id, quantity, total_amount, created_at)
      VALUES (${order.id}, ${order.userId}, ${order.productId}, ${order.quantity}, ${order.totalAmount}, ${order.createdAt})
    """.update

  def findById(orderId: OrderId): Query0[Order] =
    sql"""
      SELECT id, user_id, product_id, quantity, total_amount, created_at
      FROM orders
      WHERE id = $orderId
    """.query[Order]

  def findByUserId(userId: UserId): Query0[Order] =
    sql"""
      SELECT id, user_id, product_id, quantity, total_amount, created_at
      FROM orders
      WHERE user_id = $userId
      ORDER BY created_at DESC
    """.query[Order]

  def update(order: Order): Update0 =
    sql"""
      UPDATE orders
      SET product_id = ${order.productId}, quantity = ${order.quantity}, total_amount = ${order.totalAmount}
      WHERE id = ${order.id}
    """.update

  def delete(orderId: OrderId): Update0 =
    sql"""
      DELETE FROM orders
      WHERE id = $orderId
    """.update

  def exists(orderId: OrderId): Query0[Boolean] =
    sql"""
      SELECT EXISTS(SELECT 1 FROM orders WHERE id = $orderId)
    """.query[Boolean]
}
