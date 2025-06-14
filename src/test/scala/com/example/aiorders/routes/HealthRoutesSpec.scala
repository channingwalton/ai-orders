package com.example.aiorders.routes

import cats.effect.IO
import com.example.aiorders.models.{ApplicationInfo, HealthCheck}
import com.example.aiorders.services.HealthService
import io.circe.Json
import munit.CatsEffectSuite
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.{Method, Request, Response, Status, Uri}

import java.time.Instant

class HealthRoutesSpec extends CatsEffectSuite {

  private val appInfo = ApplicationInfo("test-app", "1.0.0")

  private val mockHealthService = new HealthService[IO] {
    def check: IO[HealthCheck] = IO.pure(
      HealthCheck(
        status = "healthy",
        timestamp = Instant.parse("2025-06-14T23:01:32.123Z"),
        application = appInfo
      )
    )
  }

  private val routes = HealthRoutes[IO](mockHealthService)

  test("GET /health returns 200 OK") {
    val request = Request[IO](Method.GET, Uri.unsafeFromString("/health"))

    routes.run(request).value.flatMap {
      case Some(response) =>
        assertEquals(response.status, Status.Ok)
        IO.unit
      case None =>
        fail("Expected response but got None")
    }
  }

  test("GET /health returns correct JSON response") {
    val request = Request[IO](Method.GET, Uri.unsafeFromString("/health"))

    routes.run(request).value.flatMap {
      case Some(response) =>
        response.as[Json].map { json =>
          val expectedJson = Json.obj(
            "status"    -> Json.fromString("healthy"),
            "timestamp" -> Json.fromString("2025-06-14T23:01:32.123Z"),
            "application" -> Json.obj(
              "name"    -> Json.fromString("test-app"),
              "version" -> Json.fromString("1.0.0")
            )
          )
          assertEquals(json, expectedJson)
        }
      case None =>
        fail("Expected response but got None")
    }
  }

  test("Other paths return None") {
    val request = Request[IO](Method.GET, Uri.unsafeFromString("/other"))

    routes.run(request).value.map {
      case None    => ()
      case Some(_) => fail("Expected None but got response")
    }
  }
}
