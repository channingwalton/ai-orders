# Order Management System

## Status

⚪ **Pending**

## Objective

Implement two endpoints:
- create an order for a user 
- list orders for a user

The route should be given a `OrderServicea[F[_]]` trait, the `OrderService` will implement the business logic,
the route should handle the HTTP requests and responses, mapping from model objects to JSON using circe.

The route should map errors from the `OrderService` to appropriate error codes.

The `OrderService` should use a Store to store and retrieve orders. For now, uses an in memory store only.
