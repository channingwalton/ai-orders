package com.example.aiorders

import cats.effect.IO
import com.example.aiorders.models.ApplicationInfo
import com.example.aiorders.routes.HealthRoutes
import com.example.aiorders.services.HealthService
import io.circe.Json
import munit.CatsEffectSuite
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.{Method, Request, Status, Uri}

class HealthEndpointIntegrationSpec extends CatsEffectSuite {

  test("health endpoint integration test") {
    val appInfo       = ApplicationInfo("ai-orders", "0.1.0-SNAPSHOT")
    val healthService = HealthService[IO](appInfo)
    val routes        = HealthRoutes[IO](healthService)

    val request = Request[IO](Method.GET, Uri.unsafeFromString("/health"))

    routes.run(request).value.flatMap {
      case Some(response) =>
        assertEquals(response.status, Status.Ok)

        response.as[Json].map { json =>
          val status     = json.hcursor.downField("status").as[String]
          val appName    = json.hcursor.downField("application").downField("name").as[String]
          val appVersion = json.hcursor.downField("application").downField("version").as[String]
          val timestamp  = json.hcursor.downField("timestamp").as[String]

          assertEquals(status, Right("healthy"))
          assertEquals(appName, Right("ai-orders"))
          assertEquals(appVersion, Right("0.1.0-SNAPSHOT"))
          assert(timestamp.isRight)
          assert(timestamp.getOrElse("").nonEmpty)
        }

      case None =>
        fail("Expected response but got None")
    }
  }
}
