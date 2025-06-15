# Order Management with Error Handling

## Status

⚪  **Incomplete**

## Objective

The previous feature asked for error handling which was not implemented.

Add error handling handling as follows:

- If a user is not present return a `NotFound`
- If a request cannot be converted from JSON return a `BadRequest`
- If a response cannot be converted to JSON return an `InternalServerError`
