# Postgres OrderStore

## Status

- 🟢 **Complete**

## Objective

- Implement a postgres store for orders
- The OrderStore should have a trait and postgres implementation
- PostgresUserStore and PostgresOrderStore should use the same transactor
- Use the doobie lib
- Use Flyway for database migrations, which should run on application start
- The store should support transactions so that multiple database operations can be inside a single transaction
- Tests should use a DatabaseSpec abstract class that sets up the database store per test
  - use projects in the examples projects for guidance

## Acceptance Criteria

- [x] Follow all instructions in CLAUDE.md
- [x] All tests pass and code is properly formatted
