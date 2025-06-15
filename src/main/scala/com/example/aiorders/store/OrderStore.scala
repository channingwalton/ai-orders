package com.example.aiorders.store

import cats.arrow.FunctionK
import com.example.aiorders.models.{Order, OrderId, User, UserId}

trait OrderStore[F[_], G[_]] {
  // Order operations
  def create(order: Order): G[Unit]
  def findById(orderId: OrderId): G[Option[Order]]
  def findByUserId(userId: UserId): G[List[Order]]
  def update(order: Order): G[Unit]
  def delete(orderId: OrderId): G[Unit]
  def exists(orderId: OrderId): G[Boolean]

  // User operations
  def createUser(user: User): G[Unit]
  def findUserById(userId: UserId): G[Option[User]]
  def findUserByEmail(email: String): G[Option[User]]
  def updateUser(user: User): G[Unit]
  def deleteUser(userId: UserId): G[Unit]
  def userExists(userId: UserId): G[Boolean]

  // Transaction support
  def commit[A](f: G[A]): F[A]
  def lift: FunctionK[F, G]
}
