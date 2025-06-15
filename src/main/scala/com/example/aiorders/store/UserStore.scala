package com.example.aiorders.store

import cats.arrow.FunctionK
import com.example.aiorders.models.{User, UserId}

trait UserStore[F[_], G[_]] {
  def create(user: User): G[Unit]
  def findById(userId: UserId): G[Option[User]]
  def findByEmail(email: String): G[Option[User]]
  def update(user: User): G[Unit]
  def delete(userId: UserId): G[Unit]
  def exists(userId: UserId): G[Boolean]
  def commit[A](f: G[A]): F[A]
  def lift: FunctionK[F, G]
}
