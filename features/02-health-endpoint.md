# Health Endpoint

## Status

🟢 **Complete**

## Objective

Add a health check endpoint at `/health` to allow monitoring systems and other services to verify the application is running and responsive.

## Requirements

### Core Health Endpoint
- Create a GET endpoint at `/health` 
- Return HTTP 200 status with JSON response when application is healthy
- Response should include:
  - Application status
  - Timestamp of the check
  - Basic system information

### Response Format
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

### Technical Implementation
- Use HTTP4s DSL for route definition
- Follow the layered architecture pattern:
  - HTTP route handles request/response
  - Internal service provides health check logic
- Include proper JSON encoding with Circe
- Add comprehensive unit tests

## Acceptance Criteria

- [x] Health endpoint responds at `/health` with GET request
- [x] Returns HTTP 200 status code when healthy
- [x] Returns proper JSON response format
- [x] Includes application name and version from build.sbt
- [x] Includes current timestamp in ISO format
- [x] HTTP route layer only handles HTTP concerns
- [x] Business logic contained in separate service trait with implementation
- [x] Unit tests cover both service and route layers
- [x] Integration test verifies end-to-end functionality
- [x] Code follows project formatting standards (`sbt commitCheck` passes)
- [x] Documentation updated if needed

## Implementation Notes

- Reference existing examples in the `examples/` directory for HTTP4s patterns
- Use the established project structure and coding conventions
- Ensure the endpoint is lightweight and fast-responding
- Consider this endpoint will be called frequently by monitoring systems