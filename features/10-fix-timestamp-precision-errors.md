# Fix timestamp precision errors in CI

## Status

- ⚪ **Pending**

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

- [ ] Ensure all timestamps are precise to 1 second
- [ ] Follow all instructions in CLAUDE.md
