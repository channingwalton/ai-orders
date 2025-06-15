package com.example.aiorders.services

import cats.effect.{Clock, Sync}
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.example.aiorders.models.{CreateOrderRequest, Order, UserOrdersResponse}
import com.example.aiorders.repository.{OrderRepository, ProductRepository, UserRepository}
import org.typelevel.log4cats.Logger

import java.util.UUID

sealed trait OrderError extends Throwable {
  def message: String
  override def getMessage: String = message
}

object OrderError {
  case class UserNotFound(userId: UUID) extends OrderError {
    def message: String = s"User with id $userId not found"
  }

  case class ProductNotFound(productId: UUID) extends OrderError {
    def message: String = s"Product with id $productId not found"
  }

  case class InvalidQuantity(quantity: Int) extends OrderError {
    def message: String = s"Invalid quantity: $quantity. Must be greater than 0"
  }
}

trait OrderService[F[_]] {
  def createOrder(request: CreateOrderRequest): F[Order]
  def getUserOrders(userId: UUID): F[UserOrdersResponse]
}

object OrderService {
  def apply[F[_]: Sync: Clock: Logger](
    userRepository: UserRepository[F],
    productRepository: ProductRepository[F],
    orderRepository: OrderRepository[F]
  ): OrderService[F] =
    new OrderService[F] {

      def createOrder(request: CreateOrderRequest): F[Order] = {
        for {
          _ <- Logger[F].info(
            s"Creating order for user ${request.userId}, product ${request.productId}"
          )
          _         <- validateQuantity(request.quantity)
          _         <- validateUserExists(request.userId)
          product   <- validateProductExists(request.productId)
          orderId   <- Sync[F].delay(UUID.randomUUID())
          timestamp <- Clock[F].realTimeInstant
          totalPrice = product.priceCents * request.quantity
          order = Order(
            orderId,
            request.userId,
            request.productId,
            request.quantity,
            totalPrice,
            timestamp
          )
          createdOrder <- orderRepository.create(order)
          _            <- Logger[F].info(s"Order created successfully: ${createdOrder.id}")
        } yield createdOrder
      }.handleErrorWith { error =>
        Logger[F].error(error)(s"Failed to create order: ${error.getMessage}") *>
          Sync[F].raiseError(error)
      }

      def getUserOrders(userId: UUID): F[UserOrdersResponse] = {
        for {
          _      <- Logger[F].info(s"Retrieving orders for user $userId")
          _      <- validateUserExists(userId)
          orders <- orderRepository.findByUserId(userId)
          _      <- Logger[F].info(s"Found ${orders.length} orders for user $userId")
        } yield UserOrdersResponse(userId, orders)
      }.handleErrorWith { error =>
        Logger[F].error(error)(
          s"Failed to retrieve orders for user $userId: ${error.getMessage}"
        ) *>
          Sync[F].raiseError(error)
      }

      private def validateQuantity(quantity: Int): F[Unit] =
        if (quantity > 0) Sync[F].unit
        else Sync[F].raiseError(OrderError.InvalidQuantity(quantity))

      private def validateUserExists(userId: UUID): F[Unit] =
        userRepository.findById(userId).flatMap {
          case Some(_) => Sync[F].unit
          case None    => Sync[F].raiseError(OrderError.UserNotFound(userId))
        }

      private def validateProductExists(productId: UUID) =
        productRepository.findById(productId).flatMap {
          case Some(product) => Sync[F].pure(product)
          case None          => Sync[F].raiseError(OrderError.ProductNotFound(productId))
        }
    }
}
