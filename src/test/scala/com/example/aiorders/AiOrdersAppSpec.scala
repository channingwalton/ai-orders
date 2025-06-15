package com.example.aiorders

import cats.effect.IO
import com.comcast.ip4s.{host, port}
import com.example.aiorders.config.{AppConfig, ApplicationConfig, DatabaseConfig, ServerConfig}
import munit.CatsEffectSuite

class AiOrdersAppSpec extends CatsEffectSuite {

  test("application loads configuration correctly") {
    AiOrdersApp.loadConfig.map { config =>
      assertEquals(config.application.name, "ai-orders")
      assertEquals(config.application.version, "0.1.0-SNAPSHOT")
      assertEquals(config.server.host.toString, "0.0.0.0")
      assertEquals(config.server.port.value, 8080)
    }
  }

  test("server resource can be created") {
    val config = AppConfig(
      server = ServerConfig(host"127.0.0.1", port"8081"),
      application = ApplicationConfig("test-app", "1.0.0"),
      database = DatabaseConfig("jdbc:postgresql://localhost:5432/test", "test", "test")
    )

    AiOrdersApp.server(config, runMigrations = false).use { server =>
      IO {
        assert(server.address.getPort == 8081)
        assert(server.isSecure == false)
      }
    }
  }
}
