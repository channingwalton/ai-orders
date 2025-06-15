# Order Management with Error Handling

## Status

🟢 **Complete**

## Objective

The previous feature asked for error handling which was not implemented.

Add error handling as follows:

- If a user is not present return a `NotFound`
- If a request cannot be converted from JSON return a `BadRequest`
- If a response cannot be converted to JSON return an `InternalServerError`

## Acceptance Criteria

- [x] `User` model and `UserService` for user management
- [x] `ServiceError` sealed trait with specific error types
- [x] `OrderService` validates users exist before creating/listing orders
- [x] `OrderRoutes` properly handles `UserNotFound` errors with 404 NotFound response
- [x] `OrderRoutes` properly handles JSON decoding failures with 400 BadRequest response
- [x] `OrderRoutes` properly handles JSON encoding failures with 500 InternalServerError response
- [x] Comprehensive error handling tests covering all scenarios
- [x] Integration with main application using both UserService and OrderService
- [x] All tests pass and code is properly formatted
