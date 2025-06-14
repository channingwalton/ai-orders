package com.example.aiorders.routes

import cats.effect.Sync
import cats.syntax.flatMap._
import com.example.aiorders.models.HealthCheck
import com.example.aiorders.services.HealthService
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl

object HealthRoutes {
  def apply[F[_]: Sync](healthService: HealthService[F]): HttpRoutes[F] = {
    object dsl extends Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] { case GET -> Root / "health" =>
      healthService.check.flatMap(Ok(_))
    }
  }
}
