package com.example.aiorders.utils

import java.time.Instant
import java.time.temporal.ChronoUnit

object TimeUtils {

  def nowWithSecondPrecision: Instant =
    Instant.now().truncatedTo(ChronoUnit.SECONDS)

  def nowWithMicrosecondPrecision: Instant =
    Instant.now().truncatedTo(ChronoUnit.MICROS)
}
