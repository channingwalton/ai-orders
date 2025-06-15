# Order Management System

## Status

⚪ **Pending**

## Objective

Implement a complete order management system that allows creating orders for monthly and annual subscriptions, storing them in a PostgreSQL database, and retrieving orders for users.

## Requirements

### Core Order Functionality
- Create orders for users with monthly and annual subscription products
- Store orders persistently in PostgreSQL database
- Retrieve all orders for a specific user
- Support different subscription types (monthly, annual)
- Generate unique order IDs and track order timestamps

### Domain Model
- **User**: Customer placing orders (user ID, name, email)
- **Product**: Subscription offerings (product ID, name, type, price)
- **Order**: User's purchase (order ID, user ID, product ID, quantity, total, created timestamp)
- **Subscription Type**: MONTHLY, ANNUAL enumeration

### Database Schema
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    subscription_type VARCHAR(20) NOT NULL CHECK (subscription_type IN ('MONTHLY', 'ANNUAL')),
    price_cents INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL DEFAULT 1,
    total_price_cents INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### API Endpoints

#### Create Order
**POST** `/api/orders`

Request body:
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "productId": "987fcdeb-51a2-43d1-9f12-123456789abc",
  "quantity": 1
}
```

Response:
```json
{
  "orderId": "456e7890-e12b-34d5-a678-901234567def",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "productId": "987fcdeb-51a2-43d1-9f12-123456789abc",
  "quantity": 1,
  "totalPriceCents": 999,
  "createdAt": "2025-06-14T23:45:00.123Z"
}
```

#### Get User Orders
**GET** `/api/users/{userId}/orders`

Response:
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "orders": [
    {
      "orderId": "456e7890-e12b-34d5-a678-901234567def",
      "product": {
        "id": "987fcdeb-51a2-43d1-9f12-123456789abc",
        "name": "Premium Monthly",
        "subscriptionType": "MONTHLY",
        "priceCents": 999
      },
      "quantity": 1,
      "totalPriceCents": 999,
      "createdAt": "2025-06-14T23:45:00.123Z"
    }
  ]
}
```

## Technical Implementation

### Architecture Layers
- **Routes**: HTTP request/response handling for order and user endpoints
- **Services**: Business logic for order creation, validation, and retrieval
- **Repository**: Database access layer using Doobie
- **Models**: Domain objects with JSON codecs using Circe

### Database Integration
- Use Flyway migrations for schema management
- Implement Doobie repository pattern for type-safe database access
- Add database connection pooling with HikariCP
- Include database configuration in application.conf

### Validation and Error Handling
- Validate user and product exist before creating orders
- Handle duplicate orders appropriately
- Return proper HTTP status codes (201 for creation, 404 for not found, 400 for validation errors)
- Structured error responses with meaningful messages

### Testing Strategy
- Unit tests for all service logic and validation rules
- Integration tests for database operations using testcontainers
- Route tests for HTTP endpoints with mock services
- End-to-end tests for complete order workflow

## Acceptance Criteria

- [ ] Database schema created with proper migrations
- [ ] User, Product, and Order domain models implemented
- [ ] Order creation service with business logic validation
- [ ] User order retrieval service
- [ ] Database repository layer using Doobie
- [ ] POST /api/orders endpoint for order creation
- [ ] GET /api/users/{userId}/orders endpoint for order retrieval
- [ ] Proper JSON serialization/deserialization for all models
- [ ] Input validation and error handling
- [ ] Database configuration integrated with application.conf
- [ ] Comprehensive unit tests for all components
- [ ] Integration tests with PostgreSQL testcontainers
- [ ] Route tests for all HTTP endpoints
- [ ] End-to-end workflow tests
- [ ] Code follows project formatting standards (sbt commitCheck passes)
- [ ] API endpoints work correctly when application is running
- [ ] Documentation updated with new endpoints

## Implementation Notes

- Follow the established project patterns from examples directory
- Use UUID for all primary keys for better distribution and security
- Store prices in cents to avoid floating-point precision issues
- Implement proper database transaction handling
- Consider adding order status field for future workflow extension
- Use the existing application structure and dependency injection pattern
- Add proper logging for order operations
- Ensure database migrations are reversible

## Database Configuration

Add to `application.conf`:
```hocon
database {
  driver = "org.postgresql.Driver"
  url = "jdbc:postgresql://localhost:5432/aiorders"
  user = "aiorders"
  password = "password"
  maxPoolSize = 10
}
```

## Definition of Done

When this feature is complete, the system should support:
1. Creating orders via POST /api/orders with proper validation
2. Retrieving user orders via GET /api/users/{userId}/orders
3. Persistent storage in PostgreSQL with proper schema
4. Complete test coverage including database integration
5. Working API endpoints accessible when running `sbt run`
6. Proper error handling and HTTP status codes