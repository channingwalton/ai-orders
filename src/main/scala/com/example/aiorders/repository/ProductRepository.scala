package com.example.aiorders.repository

import cats.effect.Sync
import cats.syntax.functor._
import com.example.aiorders.models.{Product, SubscriptionType}
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.{Read, Write}

import java.time.Instant
import java.util.UUID

trait ProductRepository[F[_]] {
  def findById(id: UUID): F[Option[Product]]
  def findByType(subscriptionType: SubscriptionType): F[List[Product]]
  def create(product: Product): F[Product]
}

object ProductRepository {

  implicit val subscriptionTypeRead: Read[SubscriptionType] =
    Read[String].map(SubscriptionType.valueOf)

  implicit val subscriptionTypeWrite: Write[SubscriptionType] =
    Write[String].contramap(_.toString)

  implicit val productRead: Read[Product] =
    Read[(UUID, String, SubscriptionType, Int, Instant)].map {
      case (id, name, subscriptionType, priceCents, createdAt) =>
        Product(id, name, subscriptionType, priceCents, createdAt)
    }

  implicit val productWrite: Write[Product] =
    Write[(UUID, String, SubscriptionType, Int, Instant)].contramap { product =>
      (product.id, product.name, product.subscriptionType, product.priceCents, product.createdAt)
    }

  def apply[F[_]: Sync](xa: doobie.Transactor[F]): ProductRepository[F] =
    new ProductRepository[F] {

      def findById(id: UUID): F[Option[Product]] =
        sql"SELECT id, name, subscription_type, price_cents, created_at FROM products WHERE id = $id"
          .query[Product]
          .option
          .transact(xa)

      def findByType(subscriptionType: SubscriptionType): F[List[Product]] =
        sql"SELECT id, name, subscription_type, price_cents, created_at FROM products WHERE subscription_type = $subscriptionType"
          .query[Product]
          .stream
          .compile
          .toList
          .transact(xa)

      def create(product: Product): F[Product] =
        sql"INSERT INTO products (id, name, subscription_type, price_cents, created_at) VALUES (${product.id}, ${product.name}, ${product.subscriptionType}, ${product.priceCents}, ${product.createdAt})".update.run
          .transact(xa)
          .as(product)
    }
}
