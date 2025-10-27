# DTO Field Mapping Reference

This document maps the internal database fields to the IDP adapter DTO fields.

## Field Name Differences

### CreateUserRequest
- `givenName` → maps to internal `firstName`
- `familyName` → maps to internal `lastName`
- NO `enabled` field - must be handled via attributes

### CreateUserResponse / UpdateUserResponse
- `id` → NOT `userId`
- `createdAt` / `updatedAt` → `Instant` type
- NO firstName/lastName in response

### UpdateUserRequest
- `givenName` → maps to internal `firstName`
- `familyName` → maps to internal `lastName`
- `enabled` → Boolean field EXISTS

### UserInfoResponse
- `preferredUsername` → NOT `username`
- `givenName` → NOT `firstName`
- `familyName` → NOT `lastName`
- NO `roles` field - must be in attributes

### SessionInfo
- `createdAt` → `Instant` NOT `String`
- `lastAccessAt` → `Instant` (we use as expiresAt)
- NO `expiresAt` field directly

### CreateRolesResponse
- `createdRoleNames` → NOT `roleNames`

### CreateScopeRequest
- `name` → NOT `scopeName`
- `description` → EXISTS

### CreateScopeResponse
- `name` → NOT `scopeName`
