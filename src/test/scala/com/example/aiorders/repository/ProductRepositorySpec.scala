package com.example.aiorders.repository

import cats.effect.IO
import com.example.aiorders.models.{Product, SubscriptionType}

import java.time.Instant
import java.util.UUID

class ProductRepositorySpec extends DatabaseTestSuite {

  test("ProductRepository.create should insert a new product") {
    withDatabase { xa =>
      val repository = ProductRepository[IO](xa)
      val productId  = UUID.randomUUID()
      val timestamp  = Instant.now()
      val product = Product(
        productId,
        "Premium Monthly",
        SubscriptionType.MONTHLY,
        999,
        timestamp
      )

      for {
        createdProduct <- repository.create(product)
        foundProduct   <- repository.findById(productId)
      } yield {
        assertEquals(createdProduct, product)
        assertEquals(foundProduct, Some(product))
      }
    }
  }

  test("ProductRepository.findById should return None for non-existent product") {
    withDatabase { xa =>
      val repository    = ProductRepository[IO](xa)
      val nonExistentId = UUID.randomUUID()

      repository.findById(nonExistentId).map { result =>
        assertEquals(result, None)
      }
    }
  }

  test("ProductRepository.findByType should find products by subscription type") {
    withDatabase { xa =>
      val repository = ProductRepository[IO](xa)
      val timestamp  = Instant.now()
      val monthlyProduct1 = Product(
        UUID.randomUUID(),
        "Basic Monthly",
        SubscriptionType.MONTHLY,
        499,
        timestamp
      )
      val monthlyProduct2 = Product(
        UUID.randomUUID(),
        "Premium Monthly",
        SubscriptionType.MONTHLY,
        999,
        timestamp
      )
      val annualProduct = Product(
        UUID.randomUUID(),
        "Premium Annual",
        SubscriptionType.ANNUAL,
        9999,
        timestamp
      )

      for {
        _               <- repository.create(monthlyProduct1)
        _               <- repository.create(monthlyProduct2)
        _               <- repository.create(annualProduct)
        monthlyProducts <- repository.findByType(SubscriptionType.MONTHLY)
        annualProducts  <- repository.findByType(SubscriptionType.ANNUAL)
      } yield {
        assertEquals(monthlyProducts.length, 2)
        assert(monthlyProducts.contains(monthlyProduct1))
        assert(monthlyProducts.contains(monthlyProduct2))
        assertEquals(annualProducts.length, 1)
        assert(annualProducts.contains(annualProduct))
      }
    }
  }

  test(
    "ProductRepository.findByType should return empty list for subscription type with no products"
  ) {
    withDatabase { xa =>
      val repository = ProductRepository[IO](xa)

      repository.findByType(SubscriptionType.ANNUAL).map { result =>
        assertEquals(result, List.empty)
      }
    }
  }

  test("ProductRepository should handle both subscription types") {
    withDatabase { xa =>
      val repository = ProductRepository[IO](xa)
      val timestamp  = Instant.now()
      val monthlyProduct = Product(
        UUID.randomUUID(),
        "Monthly Plan",
        SubscriptionType.MONTHLY,
        1999,
        timestamp
      )
      val annualProduct = Product(
        UUID.randomUUID(),
        "Annual Plan",
        SubscriptionType.ANNUAL,
        19999,
        timestamp
      )

      for {
        createdMonthly <- repository.create(monthlyProduct)
        createdAnnual  <- repository.create(annualProduct)
        foundMonthly   <- repository.findById(monthlyProduct.id)
        foundAnnual    <- repository.findById(annualProduct.id)
      } yield {
        assertEquals(createdMonthly.subscriptionType, SubscriptionType.MONTHLY)
        assertEquals(createdAnnual.subscriptionType, SubscriptionType.ANNUAL)
        assertEquals(foundMonthly, Some(monthlyProduct))
        assertEquals(foundAnnual, Some(annualProduct))
      }
    }
  }

  test("ProductRepository should handle different price points") {
    withDatabase { xa =>
      val repository = ProductRepository[IO](xa)
      val timestamp  = Instant.now()
      val freeProduct = Product(
        UUID.randomUUID(),
        "Free Plan",
        SubscriptionType.MONTHLY,
        0,
        timestamp
      )
      val expensiveProduct = Product(
        UUID.randomUUID(),
        "Enterprise Plan",
        SubscriptionType.ANNUAL,
        999999,
        timestamp
      )

      for {
        createdFree      <- repository.create(freeProduct)
        createdExpensive <- repository.create(expensiveProduct)
        foundFree        <- repository.findById(freeProduct.id)
        foundExpensive   <- repository.findById(expensiveProduct.id)
      } yield {
        assertEquals(createdFree.priceCents, 0)
        assertEquals(createdExpensive.priceCents, 999999)
        assertEquals(foundFree, Some(freeProduct))
        assertEquals(foundExpensive, Some(expensiveProduct))
      }
    }
  }

  test("ProductRepository should handle special characters in product names") {
    withDatabase { xa =>
      val repository = ProductRepository[IO](xa)
      val productId  = UUID.randomUUID()
      val timestamp  = Instant.now()
      val product = Product(
        productId,
        "Prémium & Professional™ Plan (v2.0)",
        SubscriptionType.MONTHLY,
        2999,
        timestamp
      )

      for {
        createdProduct <- repository.create(product)
        foundProduct   <- repository.findById(productId)
      } yield {
        assertEquals(createdProduct, product)
        assertEquals(foundProduct, Some(product))
      }
    }
  }

  test("ProductRepository should maintain insertion order in findByType") {
    withDatabase { xa =>
      val repository = ProductRepository[IO](xa)
      val baseTime   = Instant.now()
      val product1 = Product(
        UUID.randomUUID(),
        "Product 1",
        SubscriptionType.MONTHLY,
        100,
        baseTime.minusSeconds(60)
      )
      val product2 = Product(
        UUID.randomUUID(),
        "Product 2",
        SubscriptionType.MONTHLY,
        200,
        baseTime.minusSeconds(30)
      )
      val product3 = Product(
        UUID.randomUUID(),
        "Product 3",
        SubscriptionType.MONTHLY,
        300,
        baseTime
      )

      for {
        _        <- repository.create(product1)
        _        <- repository.create(product2)
        _        <- repository.create(product3)
        products <- repository.findByType(SubscriptionType.MONTHLY)
      } yield {
        assertEquals(products.length, 3)
        assert(products.contains(product1))
        assert(products.contains(product2))
        assert(products.contains(product3))
      }
    }
  }
}
