package com.example.aiorders.db

import cats.effect.Sync
import cats.syntax.all._
import com.example.aiorders.config.AppConfig
import org.flywaydb.core.Flyway
import org.typelevel.log4cats.StructuredLogger

object DatabaseMigration {

  def migrate[F[_]: Sync: StructuredLogger](config: AppConfig): F[Unit] =
    for {
      _ <- StructuredLogger[F].info("Starting database migration")
      flyway = Flyway
        .configure()
        .dataSource(config.database.url, config.database.username, config.database.password)
        .load()
      result <- Sync[F].delay(flyway.migrate())
      _ <- StructuredLogger[F].info(
        s"Database migration completed. Applied ${result.migrationsExecuted} migrations"
      )
    } yield ()

  def clean[F[_]: Sync: StructuredLogger](config: AppConfig): F[Unit] =
    for {
      _ <- StructuredLogger[F].warn("Cleaning database - this will drop all data!")
      flyway = Flyway
        .configure()
        .dataSource(config.database.url, config.database.username, config.database.password)
        .load()
      _ <- Sync[F].delay(flyway.clean())
      _ <- StructuredLogger[F].info("Database cleaned")
    } yield ()
}
