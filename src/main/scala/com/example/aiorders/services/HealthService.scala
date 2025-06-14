package com.example.aiorders.services

import cats.effect.{Clock, Sync}
import cats.syntax.functor._
import com.example.aiorders.models.{ApplicationInfo, HealthCheck}

trait HealthService[F[_]] {
  def check: F[HealthCheck]
}

object HealthService {
  def apply[F[_]: Sync: Clock](appInfo: ApplicationInfo): HealthService[F] =
    new HealthService[F] {
      def check: F[HealthCheck] =
        Clock[F].realTimeInstant.map { timestamp =>
          HealthCheck(
            status = "healthy",
            timestamp = timestamp,
            application = appInfo
          )
        }
    }
}
