ThisBuild / scalaVersion     := "3.3.6"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "ai-orders",
    libraryDependencies ++= Seq(
      // HTTP4s
      "org.http4s" %% "http4s-ember-server" % "0.23.29",
      "org.http4s" %% "http4s-ember-client" % "0.23.29",
      "org.http4s" %% "http4s-circe"        % "0.23.29",
      "org.http4s" %% "http4s-dsl"          % "0.23.29",

      // Circe for JSON
      "io.circe" %% "circe-core"    % "0.14.14",
      "io.circe" %% "circe-generic" % "0.14.14",
      "io.circe" %% "circe-parser"  % "0.14.14",

      // Doobie for database access
      "org.tpolecat" %% "doobie-core"     % "1.0.0-RC9",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC9",
      "org.tpolecat" %% "doobie-hikari"   % "1.0.0-RC9",

      // Flyway for database migrations
      "org.flywaydb" % "flyway-core"                % "11.9.1",
      "org.flywaydb" % "flyway-database-postgresql" % "11.9.1",

      // PostgreSQL driver
      "org.postgresql" % "postgresql" % "42.7.4",

      // Cats Effect
      "org.typelevel" %% "cats-effect" % "3.5.7",

      // Logging
      "org.typelevel" %% "log4cats-slf4j"  % "2.7.0",
      "ch.qos.logback" % "logback-classic" % "1.5.15",

      // Configuration
      "com.github.pureconfig" %% "pureconfig-core"        % "0.17.9",
      "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.17.9",

      // Testing
      "org.scalameta" %% "munit"                           % "1.1.1"     % Test,
      "org.typelevel" %% "munit-cats-effect"               % "2.1.0"     % Test,
      "com.dimafeng"  %% "testcontainers-scala-munit"      % "0.41.5"    % Test,
      "com.dimafeng"  %% "testcontainers-scala-postgresql" % "0.41.5"    % Test,
      "org.tpolecat"  %% "doobie-munit"                    % "1.0.0-RC9" % Test
    ),

    // Test settings
    testFrameworks += new TestFramework("munit.Framework"),

    // Aliases
    addCommandAlias("ci", "test"),
    addCommandAlias(
      "commitCheck",
      "clean; scalafmtCheckAll; scalafmtSbtCheck; scalafixAll --check; test"
    )
  )

// Compiler options
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint"
)
