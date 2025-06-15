package com.example.aiorders.store

import com.example.aiorders.models.{Order, OrderId, ProductId, User, UserId}
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

import java.time.Instant
import java.util.UUID

object OrderStatements {

  implicit val userIdMeta: Meta[UserId]       = Meta[UUID].timap(UserId.apply)(_.value)
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

  // User operations
  def createUser(user: User): Update0 =
    sql"""
      INSERT INTO users (id, email, name, created_at)
      VALUES (${user.id}, ${user.email}, ${user.name}, ${user.createdAt})
    """.update

  def findUserById(userId: UserId): Query0[User] =
    sql"""
      SELECT id, email, name, created_at
      FROM users
      WHERE id = $userId
    """.query[User]

  def findUserByEmail(email: String): Query0[User] =
    sql"""
      SELECT id, email, name, created_at
      FROM users
      WHERE email = $email
    """.query[User]

  def updateUser(user: User): Update0 =
    sql"""
      UPDATE users
      SET email = ${user.email}, name = ${user.name}
      WHERE id = ${user.id}
    """.update

  def deleteUser(userId: UserId): Update0 =
    sql"""
      DELETE FROM users
      WHERE id = $userId
    """.update

  def userExists(userId: UserId): Query0[Boolean] =
    sql"""
      SELECT EXISTS(SELECT 1 FROM users WHERE id = $userId)
    """.query[Boolean]
}
