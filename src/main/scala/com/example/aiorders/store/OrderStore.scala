package com.example.aiorders.store

import cats.arrow.FunctionK
import com.example.aiorders.models.{Order, OrderId, UserId}

trait OrderStore[F[_], G[_]] {
  def create(order: Order): G[Unit]
  def findById(orderId: OrderId): G[Option[Order]]
  def findByUserId(userId: UserId): G[List[Order]]
  def update(order: Order): G[Unit]
  def delete(orderId: OrderId): G[Unit]
  def exists(orderId: OrderId): G[Boolean]
  def commit[A](f: G[A]): F[A]
  def lift: FunctionK[F, G]
}
