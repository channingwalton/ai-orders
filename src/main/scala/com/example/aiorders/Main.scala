package com.example.aiorders

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    AiOrdersApp.serverResource.useForever.as(ExitCode.Success)
}
