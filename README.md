# AI Orders

A RESTful order management system built with Scala 3, HTTP4s, and PostgreSQL.

## 🚀 Technology Stack

- **Scala 3.3.6** - Modern functional programming language
- **HTTP4s 0.23.29** - Purely functional HTTP library
- **Cats Effect** - Asynchronous and concurrent programming
- **Circe** - JSON library for Scala
- **Doobie 1.0.0-RC9** - Pure functional JDBC layer
- **PostgreSQL 16** - Relational database
- **Flyway 11.9.1** - Database migration tool
- **PureConfig** - Configuration management
- **log4cats** - Structured logging
- **munit** - Testing framework with Cats Effect support

## 📋 Prerequisites

- **Java 21** or higher
- **SBT 1.11.2**
- **PostgreSQL 16** - Required for user store functionality
- **Docker** - Required for running database tests with TestContainers

## 🏃 Quick Start

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd ai-orders
   ```

2. **Set up PostgreSQL database**
   ```bash
   # Create database and user
   createdb aiorders
   createuser aiorders
   psql -d aiorders -c "ALTER USER aiorders WITH PASSWORD 'password';"
   psql -d aiorders -c "GRANT ALL PRIVILEGES ON DATABASE aiorders TO aiorders;"
   ```

3. **Run the application**
   ```bash
   sbt run
   ```
   
   The application will automatically run Flyway migrations on startup.

4. **Access the health endpoint**
   ```bash
   curl http://localhost:8080/health
   ```

The application will start on `http://localhost:8080` by default.

## 🔌 API Endpoints

### Health Check

**GET** `/health`

Returns the application health status with current timestamp and version information.

**Response:**
```json
{
  "status": "healthy",
  "timestamp": "2025-06-14T23:01:32.123Z",
  "application": {
    "name": "ai-orders",
    "version": "0.1.0-SNAPSHOT"
  }
}
```

### Order Management

**POST** `/orders`

Creates a new order for an existing user.

**Request Body:**
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "productId": "laptop-pro",
  "quantity": 2,
  "totalAmount": 2999.98
}
```

**Response (201 Created):**
```json
{
  "id": "987fcdeb-51a2-43d1-9f4b-123456789abc",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "productId": "laptop-pro",
  "quantity": 2,
  "totalAmount": 2999.98,
  "createdAt": "2025-06-15T11:30:45.123Z"
}
```

**Error Responses:**
- `404 Not Found` - User does not exist
- `400 Bad Request` - Invalid JSON request format
- `500 Internal Server Error` - Server processing error

**GET** `/orders/user/{userId}`

Retrieves all orders for a specific user, sorted by creation time (newest first).

**Response (200 OK):**
```json
{
  "orders": [
    {
      "id": "987fcdeb-51a2-43d1-9f4b-123456789abc",
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "productId": "laptop-pro",
      "quantity": 2,
      "totalAmount": 2999.98,
      "createdAt": "2025-06-15T11:30:45.123Z"
    }
  ]
}
```

**Error Responses:**
- `404 Not Found` - User does not exist or invalid UUID format
- `500 Internal Server Error` - Server processing error

## 🧪 Development

### Running Tests

```bash
# Run all tests (includes database integration tests)
sbt test

# Run specific test suite
sbt "testOnly com.example.aiorders.routes.HealthRoutesSpec"

# Run order management tests
sbt "testOnly com.example.aiorders.routes.OrderRoutesSpec"
sbt "testOnly com.example.aiorders.routes.OrderRoutesErrorSpec"
sbt "testOnly com.example.aiorders.services.OrderServiceSpec"
sbt "testOnly com.example.aiorders.services.UserServiceSpec"

# Run database store tests (requires Docker)
sbt "testOnly com.example.aiorders.store.PostgresUserStoreSpec"
```

**Note**: Database tests use TestContainers and require Docker to be running.

### Code Quality

```bash
# Run all quality checks (formatting, linting, tests)
sbt commitCheck

# Format code
sbt scalafmtAll

# Apply scalafix rules
sbt scalafixAll
```

### Development Workflow

1. **Feature Development**: Create feature documents in `features/` directory
2. **Implementation**: Follow the established layered architecture pattern
3. **Testing**: Add comprehensive unit and integration tests
4. **Quality Check**: Run `sbt commitCheck` before committing
5. **Commit**: Use conventional commit messages with Claude co-authorship

## 🏗️ Project Structure

```
ai-orders/
├── src/
│   ├── main/
│   │   ├── scala/com/example/aiorders/
│   │   │   ├── config/          # Configuration models
│   │   │   ├── db/              # Database migrations
│   │   │   ├── models/          # Domain models
│   │   │   ├── routes/          # HTTP routes
│   │   │   ├── services/        # Business logic
│   │   │   ├── store/           # Data persistence layer
│   │   │   ├── AiOrdersApp.scala # Main application
│   │   │   └── Main.scala       # Entry point
│   │   └── resources/
│   │       ├── application.conf # Application configuration
│   │       └── db/migration/    # Flyway SQL migrations
│   └── test/
│       └── scala/               # Test suites including DatabaseSpec
├── features/                    # Feature documentation
├── examples/                    # Reference implementations
├── reference/                   # Library references
└── build.sbt                   # Build configuration
```

### Architecture Layers

- **Routes**: Handle HTTP requests/responses, delegate to services
- **Services**: Business logic, defined as traits with implementations  
- **Store**: Data persistence layer with PostgreSQL implementation using Doobie
- **Models**: Domain objects with JSON codecs (Order, User, ServiceError)
- **Config**: Configuration management with PureConfig
- **DB**: Database migrations and setup using Flyway

### Domain Models

- **Order**: Core business entity representing a customer order
- **User**: User entity for validation and management
- **ServiceError**: Sealed trait for structured error handling
- **OrderId, UserId, ProductId**: Strongly-typed identifiers

### Data Persistence

The application uses a layered approach to data persistence:

- **UserStore**: Generic trait `UserStore[F[_], G[_]]` for user CRUD operations
- **PostgresUserStore**: PostgreSQL implementation using Doobie
- **Transaction Support**: Operations can be composed within database transactions
- **Migration Management**: Flyway handles schema versioning and updates
- **Test Infrastructure**: `DatabaseSpec` provides TestContainer-based database testing

**User Store Operations:**
- `create(user: User): G[Unit]` - Create new user
- `findById(userId: UserId): G[Option[User]]` - Find user by ID
- `findByEmail(email: String): G[Option[User]]` - Find user by email
- `update(user: User): G[Unit]` - Update existing user
- `delete(userId: UserId): G[Unit]` - Delete user
- `exists(userId: UserId): G[Boolean]` - Check if user exists

**Database Schema:**
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
```

## ⚙️ Configuration

The application uses `src/main/resources/application.conf` for configuration:

```hocon
server {
  host = "0.0.0.0"
  port = 8080
}

application {
  name = "ai-orders"
  version = "0.1.0-SNAPSHOT"
}

database {
  url = "jdbc:postgresql://localhost:5432/aiorders"
  username = "aiorders"
  password = "password"
  driver = "org.postgresql.Driver"
}
```

Environment variables can override configuration values:
- `DATABASE_URL`
- `DATABASE_USERNAME` 
- `DATABASE_PASSWORD`

## 🚀 Deployment

The application is designed to be containerized and deployed in cloud environments:

- Binds to `0.0.0.0` for container compatibility
- Uses structured logging for observability
- Implements graceful shutdown
- Follows 12-factor app principles
- Automatic database migrations on startup via Flyway
- PostgreSQL connection pooling with HikariCP

## 🤝 Contributing

1. Follow the established patterns in the `examples/` directory
2. Use the feature document template in `features/`
3. Ensure all tests pass with `sbt commitCheck`
4. Include Claude as co-author in commits:
   ```
   Co-Authored-By: Claude <noreply@anthropic.com>
   ```

## 📚 Documentation

- **Feature Documents**: See `features/` directory for detailed specifications
- **API Documentation**: Available endpoints documented above
- **Code Examples**: Reference implementations in `examples/`
- **HTTP4s Documentation**: https://http4s.org/
- **Cats Effect**: https://typelevel.org/cats-effect/

---

**Built with ❤️ using Scala 3 and the Typelevel ecosystem**