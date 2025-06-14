# Initial Order Management System Setup

## Status

🟢 **Complete**

## Objective

Set up a scala 3 project for building a restful API in subsequent feature documents.

## Technical Stack

### Core Technologies

- written in scala 3.3.6
- uses [http4s](https://http4s.org/) and type level libraries
- store data in postgresql version 16
- use sbt 1.11.2 for the build
- use Flyway 11.9.1 to manage the database
- use scala [doobie](https://github.com/typelevel/doobie) 1.0.0-RC9 for managing persistence in scala
- add scalafix plugin: addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.3")
- add scalafmt plugin: addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "0.14.3")
- add sbt-tpolecat: addSbtPlugin("org.typelevel" % "sbt-tpolecat" % "0.5.2")

### Code Style

- Use the projects in the examples directory to inform the style of the project
  - do not add the examples or reference dirs to git
- layer the app:
  - http routes should just deal with http requests and responses, calling internal services to do the work on model types
  - internal services should use model types and consist of a trait with concrete implementation to facilitate testing with mocks

### Testing Strategy

- everything should be unit tested using [munit](https://scalameta.org/munit/) and [munit-cat-effect](https://typelevel.org/munit-cats-effect/)
- test the storage implementation using [scala testcontainers](https://github.com/testcontainers/testcontainers-scala) and assuming docker is running on the machine
- Add a sbt alias called `ci`, that runs all the tests.

## Git Setup

Initialise a git repo.

Exclude the examples and reference directories

Add a GitHub action to build the code and add tests using:

- uses: actions/checkout@v4
- uses: actions/setup-java@v4
      with:
        distribution: temurin
        java-version: 21
        cache: sbt
    - uses: sbt/setup-sbt@v1
      with:
        sbt-runner-version: 1.11.2
    - name: Build and test
      shell: bash
      run: sbt ci


