# Fix timestamp precision errors in CI

## Status

🟢 **Complete**

## Objective

The database tests are failing in CI:
```
```

```Diff (- expected, + obtained)
     name = "Email User",
-    createdAt = 2025-06-15T20:33:52.075334991Z
+    createdAt = 2025-06-15T20:33:52.075335Z
   )
```

There is a difference between postgres precision and Java Instant.

Fix the issue.

## Acceptance Criteria

- [x] Ensure all timestamps are precise to 1 second
- [x] Follow all instructions in CLAUDE.md

## Implementation Summary

- Created `TimeUtils.nowWithSecondPrecision` utility function to truncate timestamps to second precision
- Updated all timestamp creation points to use the new utility function:
  - UserService.createUser
  - OrderService.createOrder 
  - TestHelpers.createInMemoryUserService
  - PostgresUserStoreSpec test data
  - UserServiceSpec test data
- All 33 tests now pass with consistent timestamp precision
