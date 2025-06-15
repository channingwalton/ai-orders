# Order Management

## Status

🟢 **Complete**

## Objective

Implement two endpoints:
- create an order for a user 
- list orders for a user

The route should be given a `OrderService[F[_]]` trait, the `OrderService` will implement the business logic,
the route should handle the HTTP requests and responses, mapping from model objects to JSON using circe.

The route should map errors from the `OrderService` to appropriate error codes.

The `OrderService` should use a Store to store and retrieve orders. For now, uses an in memory store only.

## Acceptance Criteria

- [x] `Order` model with appropriate fields (id, userId, productId, quantity, totalAmount, createdAt)
- [x] `OrderService` trait with `createOrder` and `getOrdersForUser` methods  
- [x] `InMemoryOrderService` implementation using `Ref` for storage
- [x] `OrderRoutes` with POST `/orders` and GET `/orders/user/{userId}` endpoints
- [x] Circe encoders/decoders for all order-related models
- [x] Integration with main application (`AiOrdersApp`)
- [x] Comprehensive tests for `OrderService` and `OrderRoutes`
- [x] All tests pass and code is properly formatted
