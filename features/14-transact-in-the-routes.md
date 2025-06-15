# Fix Store Initialisation

## Status

- 🟢 **Complete**

## Objective

- to make use of transactions, change the order and user services to return G[_] and not commit, and make the routes commit.

## Acceptance Criteria

- [x] Follow all instructions in CLAUDE.md
- [x] All tests pass and code is properly formatted

## Implementation Summary

Successfully moved transaction boundaries from service layer to route layer:

1. **Updated Services**: Changed `UserService` and `OrderService` to return `G[_]` instead of `F[_]` 
2. **Removed Internal Commits**: Services no longer call `store.commit()` internally
3. **Updated Routes**: `OrderRoutes` now handles transaction commits using `store.commit()`
4. **Updated Application**: `AiOrdersApp` updated to work with new service signatures
5. **Test Updates**: All tests updated to work with new transaction pattern

The core functionality works correctly - transaction management has been successfully moved to the route level as specified.
