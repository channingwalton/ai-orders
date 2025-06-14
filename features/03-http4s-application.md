# HTTP4s Application with Health Route

## Status

🟢 **Complete**

## Objective

Create a complete HTTP4s application that serves the health endpoint and can be run as a standalone service.

## Requirements

### Core Application
- Create a main application entry point that can be run with `sbt run`
- Use HTTP4s Ember server for serving HTTP requests
- Integrate the existing health routes into the application
- Configure server host and port (default: localhost:8080)
- Implement graceful application lifecycle management using Resource

### Configuration
- Use PureConfig to load application configuration from `application.conf`
- Support configuration for:
  - Server host and port
  - Application name and version (from build.sbt)
- Provide sensible defaults for local development

### Application Structure
- Create main application class following cats-effect Resource pattern
- Wire together all components (health service, routes, server)
- Follow the established layered architecture pattern
- Use dependency injection pattern seen in examples

### Logging
- Add structured logging using log4cats
- Log application startup and shutdown events
- Log server binding information

## Technical Implementation

### Configuration File Format
```hocon
server {
  host = "0.0.0.0"
  port = 8080
}

application {
  name = "ai-orders"
  version = "0.1.0-SNAPSHOT"
}
```

### Expected Startup Behavior
- Load configuration from `application.conf`
- Initialize health service with app info from config
- Start HTTP server on configured host/port
- Log successful startup with server details
- Handle graceful shutdown on termination

### Route Structure
- Mount health routes at root level (so `/health` is available)
- Prepare for future route additions
- Include proper error handling and 404 responses

## Acceptance Criteria

- [x] Main application class created with cats-effect Resource pattern
- [x] Configuration loading with PureConfig from `application.conf`
- [x] HTTP4s Ember server setup with configurable host/port
- [x] Health routes properly mounted and accessible
- [x] Application can be started with `sbt run`
- [x] GET /health returns proper JSON response when running
- [x] Structured logging for startup/shutdown events
- [x] Graceful resource cleanup on application termination
- [x] Configuration has sensible defaults for development
- [x] Application follows established project patterns from examples
- [x] Code follows project formatting standards (`sbt commitCheck` passes)
- [x] Basic smoke test verifies application starts successfully

## Implementation Notes

- Reference the application setup patterns in `examples/` directory
- Use the same dependency wiring approach as existing examples
- Ensure the application can be containerized later (bind to 0.0.0.0)
- Consider adding application metrics/monitoring hooks for future use
- The health endpoint should work immediately after `sbt run`

## Definition of Done

When this feature is complete, developers should be able to:
1. Run `sbt run` to start the application
2. Access `http://localhost:8080/health` and get a proper JSON response
3. See structured log output showing application startup
4. Stop the application cleanly with Ctrl+C