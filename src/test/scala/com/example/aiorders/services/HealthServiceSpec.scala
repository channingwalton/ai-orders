package com.example.aiorders.services

import cats.effect.IO
import com.example.aiorders.models.ApplicationInfo
import munit.CatsEffectSuite

import java.time.Instant

class HealthServiceSpec extends CatsEffectSuite {

  private val appInfo       = ApplicationInfo("test-app", "1.0.0")
  private val healthService = HealthService[IO](appInfo)

  test("check returns healthy status") {
    healthService.check.map { result =>
      assertEquals(result.status, "healthy")
      assertEquals(result.application, appInfo)
      assert(result.timestamp.isInstanceOf[Instant])
    }
  }

  test("check returns current timestamp") {
    val before = Instant.now()
    healthService.check.map { result =>
      val after = Instant.now()
      assert(!result.timestamp.isBefore(before))
      assert(!result.timestamp.isAfter(after))
    }
  }
}
