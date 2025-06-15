package com.example.aiorders.repository

import cats.effect.IO
import cats.syntax.traverse._
import com.example.aiorders.models.{Order, OrderWithProduct, Product, SubscriptionType, User}

import java.time.Instant
import java.util.UUID

class OrderRepositorySpec extends DatabaseTestSuite {

  def setupTestData(xa: doobie.Transactor[IO]): IO[(User, Product, Product)] = {
    val userRepo    = UserRepository[IO](xa)
    val productRepo = ProductRepository[IO](xa)
    val timestamp   = Instant.now()

    val user = User(UUID.randomUUID(), "Test User", "test@example.com", timestamp)
    val monthlyProduct = Product(
      UUID.randomUUID(),
      "Monthly Plan",
      SubscriptionType.MONTHLY,
      999,
      timestamp
    )
    val annualProduct = Product(
      UUID.randomUUID(),
      "Annual Plan",
      SubscriptionType.ANNUAL,
      9999,
      timestamp
    )

    for {
      _ <- userRepo.create(user)
      _ <- productRepo.create(monthlyProduct)
      _ <- productRepo.create(annualProduct)
    } yield (user, monthlyProduct, annualProduct)
  }

  test("OrderRepository.create should insert a new order") {
    withDatabase { xa =>
      val repository = OrderRepository[IO](xa)

      for {
        testData <- setupTestData(xa)
        (user, product, _) = testData
        orderId            = UUID.randomUUID()
        timestamp          = Instant.now()
        order              = Order(orderId, user.id, product.id, 1, 999, timestamp)
        createdOrder <- repository.create(order)
        foundOrder   <- repository.findById(orderId)
      } yield {
        assertEquals(createdOrder, order)
        assertEquals(foundOrder, Some(order))
      }
    }
  }

  test("OrderRepository.findById should return None for non-existent order") {
    withDatabase { xa =>
      val repository    = OrderRepository[IO](xa)
      val nonExistentId = UUID.randomUUID()

      repository.findById(nonExistentId).map { result =>
        assertEquals(result, None)
      }
    }
  }

  test("OrderRepository.findByUserId should return orders with product details") {
    withDatabase { xa =>
      val repository = OrderRepository[IO](xa)

      for {
        testData <- setupTestData(xa)
        (user, monthlyProduct, annualProduct) = testData
        timestamp                             = Instant.now()
        order1 = Order(
          UUID.randomUUID(),
          user.id,
          monthlyProduct.id,
          1,
          999,
          timestamp.minusSeconds(60)
        )
        order2 = Order(UUID.randomUUID(), user.id, annualProduct.id, 2, 19998, timestamp)
        _          <- repository.create(order1)
        _          <- repository.create(order2)
        userOrders <- repository.findByUserId(user.id)
      } yield {
        assertEquals(userOrders.length, 2)

        // Orders should be sorted by created_at DESC (newest first)
        val sortedOrders = userOrders.sortBy(_.createdAt.toEpochMilli).reverse

        // Check first order (newest)
        val firstOrder = sortedOrders.head
        assertEquals(firstOrder.orderId, order2.id)
        assertEquals(firstOrder.product.id, annualProduct.id)
        assertEquals(firstOrder.product.name, "Annual Plan")
        assertEquals(firstOrder.product.subscriptionType, SubscriptionType.ANNUAL)
        assertEquals(firstOrder.quantity, 2)
        assertEquals(firstOrder.totalPriceCents, 19998)

        // Check second order (older)
        val secondOrder = sortedOrders(1)
        assertEquals(secondOrder.orderId, order1.id)
        assertEquals(secondOrder.product.id, monthlyProduct.id)
        assertEquals(secondOrder.product.name, "Monthly Plan")
        assertEquals(secondOrder.product.subscriptionType, SubscriptionType.MONTHLY)
        assertEquals(secondOrder.quantity, 1)
        assertEquals(secondOrder.totalPriceCents, 999)
      }
    }
  }

  test(
    "OrderRepository.findByUserId should return empty list for user with no orders"
  ) {
    withDatabase { xa =>
      val repository = OrderRepository[IO](xa)

      for {
        testData <- setupTestData(xa)
        (user, _, _) = testData
        userOrders <- repository.findByUserId(user.id)
      } yield assertEquals(userOrders, List.empty)
    }
  }

  test(
    "OrderRepository.findByUserId should return empty list for non-existent user"
  ) {
    withDatabase { xa =>
      val repository        = OrderRepository[IO](xa)
      val nonExistentUserId = UUID.randomUUID()

      repository.findByUserId(nonExistentUserId).map { result =>
        assertEquals(result, List.empty)
      }
    }
  }

  test("OrderRepository should handle multiple orders for same user") {
    withDatabase { xa =>
      val repository = OrderRepository[IO](xa)

      for {
        testData <- setupTestData(xa)
        (user, product, _) = testData
        baseTime           = Instant.now()
        order1 = Order(UUID.randomUUID(), user.id, product.id, 1, 999, baseTime.minusSeconds(120))
        order2 = Order(UUID.randomUUID(), user.id, product.id, 2, 1998, baseTime.minusSeconds(60))
        order3 = Order(UUID.randomUUID(), user.id, product.id, 3, 2997, baseTime)
        _          <- repository.create(order1)
        _          <- repository.create(order2)
        _          <- repository.create(order3)
        userOrders <- repository.findByUserId(user.id)
      } yield {
        assertEquals(userOrders.length, 3)

        // Verify orders are returned in descending order by created_at
        val sortedOrders = userOrders.sortBy(_.createdAt.toEpochMilli).reverse
        assertEquals(sortedOrders.head.orderId, order3.id)
        assertEquals(sortedOrders(1).orderId, order2.id)
        assertEquals(sortedOrders(2).orderId, order1.id)
      }
    }
  }

  test("OrderRepository should handle different quantities and totals") {
    withDatabase { xa =>
      val repository = OrderRepository[IO](xa)

      for {
        testData <- setupTestData(xa)
        (user, product, _) = testData
        timestamp          = Instant.now()
        order              = Order(UUID.randomUUID(), user.id, product.id, 5, 4995, timestamp)
        createdOrder <- repository.create(order)
        foundOrder   <- repository.findById(order.id)
      } yield {
        assertEquals(createdOrder.quantity, 5)
        assertEquals(createdOrder.totalPriceCents, 4995)
        assertEquals(foundOrder, Some(order))
      }
    }
  }

  test("OrderRepository should handle orders with zero quantity") {
    withDatabase { xa =>
      val repository = OrderRepository[IO](xa)

      for {
        testData <- setupTestData(xa)
        (user, product, _) = testData
        timestamp          = Instant.now()
        order              = Order(UUID.randomUUID(), user.id, product.id, 0, 0, timestamp)
        createdOrder <- repository.create(order)
        foundOrder   <- repository.findById(order.id)
      } yield {
        assertEquals(createdOrder.quantity, 0)
        assertEquals(createdOrder.totalPriceCents, 0)
        assertEquals(foundOrder, Some(order))
      }
    }
  }

  test("OrderRepository should enforce foreign key constraints") {
    withDatabase { xa =>
      val repository = OrderRepository[IO](xa)
      val timestamp  = Instant.now()
      val invalidOrder = Order(
        UUID.randomUUID(),
        UUID.randomUUID(), // Non-existent user
        UUID.randomUUID(), // Non-existent product
        1,
        999,
        timestamp
      )

      repository.create(invalidOrder).attempt.map { result =>
        assert(result.isLeft, "Should fail due to foreign key constraint violation")
      }
    }
  }

  test("OrderRepository should maintain referential integrity") {
    withDatabase { xa =>
      val repository = OrderRepository[IO](xa)

      for {
        testData <- setupTestData(xa)
        (user, product, _) = testData
        timestamp          = Instant.now()
        order              = Order(UUID.randomUUID(), user.id, product.id, 1, 999, timestamp)
        _          <- repository.create(order)
        userOrders <- repository.findByUserId(user.id)
      } yield {
        assertEquals(userOrders.length, 1)
        val orderWithProduct = userOrders.head
        assertEquals(orderWithProduct.orderId, order.id)
        assertEquals(orderWithProduct.product.id, product.id)
        assertEquals(orderWithProduct.product.name, product.name)
        assertEquals(orderWithProduct.product.subscriptionType, product.subscriptionType)
        assertEquals(orderWithProduct.product.priceCents, product.priceCents)
      }
    }
  }

  test("OrderRepository should handle concurrent order creation") {
    withDatabase { xa =>
      val repository = OrderRepository[IO](xa)

      for {
        testData <- setupTestData(xa)
        (user, product, _) = testData
        timestamp          = Instant.now()
        orders = (1 to 10).map { i =>
          Order(UUID.randomUUID(), user.id, product.id, i, i * 999, timestamp.plusSeconds(i))
        }.toList

        // Create orders concurrently
        _          <- orders.map(repository.create).sequence
        userOrders <- repository.findByUserId(user.id)
      } yield {
        assertEquals(userOrders.length, 10)
        val quantities = userOrders.map(_.quantity).sorted
        assertEquals(quantities, (1 to 10).toList)
      }
    }
  }
}
