# Repository Integration Tests with TestContainers

## Overview

This document describes the comprehensive integration tests created for the AI Orders repository layer using PostgreSQL 16 with TestContainers.

## Test Coverage

### UserRepository Tests
- ✅ Create user and verify insertion
- ✅ Find user by ID (existing and non-existent)
- ✅ Find user by email (with case sensitivity)
- ✅ Handle multiple users
- ✅ Enforce unique email constraint
- ✅ Handle special characters in names and emails

### ProductRepository Tests
- ✅ Create product and verify insertion
- ✅ Find product by ID (existing and non-existent)
- ✅ Find products by subscription type (MONTHLY/ANNUAL)
- ✅ Handle different price points (including zero)
- ✅ Handle special characters in product names
- ✅ Maintain proper ordering in queries

### OrderRepository Tests
- ✅ Create order and verify insertion
- ✅ Find order by ID (existing and non-existent)
- ✅ Find orders by user ID with product details (JOIN queries)
- ✅ Handle multiple orders for same user with proper ordering
- ✅ Handle different quantities and totals
- ✅ Handle zero quantities
- ✅ Enforce foreign key constraints
- ✅ Maintain referential integrity
- ✅ Handle concurrent order creation

## Database Setup

Each test uses a fresh PostgreSQL 16 container with:
- Automatic Flyway migrations
- Clean database state per test
- Proper transaction management
- Connection pooling with HikariCP

## Test Architecture

```scala
abstract class DatabaseTestSuite extends CatsEffectSuite {
  // PostgreSQL 16 container setup
  val container = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgres:16-alpine")
  ).createContainer()
  
  // Database migration and cleanup
  def setupDatabase(): IO[Unit]
  def cleanDatabase(): IO[Unit]
  
  // Transactor resource management
  def transactorResource: Resource[IO, HikariTransactor[IO]]
}
```

## Key Testing Features

1. **Real Database Testing**: Uses actual PostgreSQL instead of mocks
2. **Data Integrity**: Tests foreign key constraints and unique constraints
3. **Complex Queries**: Tests JOIN operations for OrderWithProduct
4. **Concurrency**: Tests concurrent database operations
5. **Error Handling**: Tests constraint violations and error scenarios
6. **Performance**: Tests ordering and filtering operations

## Running the Tests

```bash
# Run all repository tests
sbt "testOnly *repository*"

# Run specific repository tests
sbt "testOnly *UserRepositorySpec"
sbt "testOnly *ProductRepositorySpec"
sbt "testOnly *OrderRepositorySpec"
```

## Benefits

- **High Confidence**: Tests against real database behavior
- **Catch Integration Issues**: Identifies SQL, constraint, and mapping problems
- **Documentation**: Tests serve as living documentation of repository behavior
- **Regression Prevention**: Prevents database-related regressions

## Implementation Status

✅ **Complete**: All repository integration tests implemented
✅ **PostgreSQL 16**: Using latest stable PostgreSQL version
✅ **TestContainers**: Proper container lifecycle management
✅ **Comprehensive Coverage**: All repository methods tested
✅ **Error Scenarios**: Constraint violations and edge cases covered