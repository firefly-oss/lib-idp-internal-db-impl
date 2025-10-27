# API Documentation - Internal Database IDP

This document provides complete API documentation for the Internal Database Identity Provider implementation.

## Table of Contents

- [Overview](#overview)
- [Authentication](#authentication)
- [API Endpoints](#api-endpoints)
  - [Authentication Endpoints](#authentication-endpoints)
  - [User Management Endpoints](#user-management-endpoints)
  - [Role Management Endpoints](#role-management-endpoints)
- [Data Models](#data-models)
- [Error Handling](#error-handling)
- [Security](#security)

## Overview

The Internal Database IDP provides RESTful HTTP endpoints for:
- User authentication and authorization
- JWT token management
- User account management
- Role-based access control (RBAC)

**Base URL**: `http://localhost:8080/api/v1`  
**Content-Type**: `application/json`  
**Authentication**: Bearer Token (for protected endpoints)

## Authentication

Most endpoints require authentication using JWT Bearer tokens:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Tokens are obtained via the `/auth/login` endpoint and should be included in the `Authorization` header for all protected endpoints.

---

## API Endpoints

### Authentication Endpoints

#### POST /auth/login

Authenticate a user and obtain access and refresh tokens.

**Request Body:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid credentials
- `403 Forbidden` - Account disabled or locked

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

---

#### POST /auth/refresh

Refresh an expired access token using a refresh token.

**Request Body:**
```json
{
  "refreshToken": "string"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid or expired refresh token

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

---

#### POST /auth/logout

Logout a user and revoke their tokens.

**Request Body:**
```json
{
  "accessToken": "string",
  "refreshToken": "string"
}
```

**Response (204 No Content):**
No response body

**Error Responses:**
- `400 Bad Request` - Invalid request

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

---

#### POST /auth/introspect

Validate and introspect an access token.

**Query Parameter:**
- `token` - Access token to introspect

**Response (200 OK):**
```json
{
  "active": true,
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john.doe",
  "scope": "USER MANAGER",
  "exp": 1704067200,
  "iat": 1704063600
}
```

**Example:**
```bash
curl -X POST "http://localhost:8080/api/v1/auth/introspect?token=eyJhbGci..." \
  -H "Content-Type: application/json"
```

---

#### GET /auth/userinfo

Get information about the authenticated user.

**Headers:**
- `Authorization: Bearer <access_token>`

**Response (200 OK):**
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "preferredUsername": "john.doe",
  "email": "john.doe@example.com",
  "givenName": "John",
  "familyName": "Doe",
  "name": "John Doe"
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid or missing token

**Example:**
```bash
curl -X GET http://localhost:8080/api/v1/auth/userinfo \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### User Management Endpoints

#### POST /users

Create a new user account.

**Headers:**
- `Authorization: Bearer <access_token>`

**Request Body:**
```json
{
  "username": "string",
  "password": "string",
  "email": "string",
  "givenName": "string",
  "familyName": "string",
  "enabled": true
}
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john.doe",
  "email": "john.doe@example.com",
  "createdAt": "2025-01-01T00:00:00Z"
}
```

**Error Responses:**
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Missing or invalid token
- `409 Conflict` - Username or email already exists

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "password": "SecurePassword123!",
    "email": "john.doe@example.com",
    "givenName": "John",
    "familyName": "Doe",
    "enabled": true
  }'
```

---

#### GET /users/{userId}

Get user details by user ID.

**Headers:**
- `Authorization: Bearer <access_token>`

**Path Parameters:**
- `userId` - User UUID

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john.doe",
  "email": "john.doe@example.com",
  "givenName": "John",
  "familyName": "Doe",
  "enabled": true,
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-01-01T00:00:00Z"
}
```

**Error Responses:**
- `401 Unauthorized` - Missing or invalid token
- `404 Not Found` - User not found

**Example:**
```bash
curl -X GET http://localhost:8080/api/v1/users/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

#### PUT /users/{userId}

Update user information.

**Headers:**
- `Authorization: Bearer <access_token>`

**Path Parameters:**
- `userId` - User UUID

**Request Body:**
```json
{
  "userId": "string",
  "email": "string",
  "givenName": "string",
  "familyName": "string",
  "enabled": true
}
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john.doe",
  "email": "john.updated@example.com",
  "givenName": "John",
  "familyName": "Updated",
  "enabled": true,
  "updatedAt": "2025-01-02T00:00:00Z"
}
```

**Error Responses:**
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Missing or invalid token
- `404 Not Found` - User not found

**Example:**
```bash
curl -X PUT http://localhost:8080/api/v1/users/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john.updated@example.com",
    "givenName": "John",
    "familyName": "Updated",
    "enabled": true
  }'
```

---

#### DELETE /users/{userId}

Delete a user account.

**Headers:**
- `Authorization: Bearer <access_token>`

**Path Parameters:**
- `userId` - User UUID

**Response (204 No Content):**
No response body

**Error Responses:**
- `401 Unauthorized` - Missing or invalid token
- `404 Not Found` - User not found

**Example:**
```bash
curl -X DELETE http://localhost:8080/api/v1/users/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

#### PUT /users/{userId}/password

Change a user's password.

**Headers:**
- `Authorization: Bearer <access_token>`

**Path Parameters:**
- `userId` - User UUID

**Request Body:**
```json
{
  "userId": "string",
  "oldPassword": "string",
  "newPassword": "string"
}
```

**Response (204 No Content):**
No response body

**Error Responses:**
- `400 Bad Request` - Invalid old password
- `401 Unauthorized` - Missing or invalid token
- `404 Not Found` - User not found

**Example:**
```bash
curl -X PUT http://localhost:8080/api/v1/users/550e8400-e29b-41d4-a716-446655440000/password \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "oldPassword": "OldPassword123!",
    "newPassword": "NewSecurePassword456!"
  }'
```

---

### Role Management Endpoints

#### POST /roles

Create a new role.

**Headers:**
- `Authorization: Bearer <access_token>`

**Request Body:**
```json
{
  "roleNames": ["string"]
}
```

**Response (201 Created):**
```json
{
  "createdRoleNames": ["ANALYST", "AUDITOR"]
}
```

**Error Responses:**
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Missing or invalid token
- `409 Conflict` - Role already exists

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/roles \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "roleNames": ["ANALYST", "AUDITOR"]
  }'
```

---

#### GET /roles

List all available roles.

**Headers:**
- `Authorization: Bearer <access_token>`

**Response (200 OK):**
```json
[
  "ADMIN",
  "MANAGER",
  "USER",
  "ANALYST"
]
```

**Error Responses:**
- `401 Unauthorized` - Missing or invalid token

**Example:**
```bash
curl -X GET http://localhost:8080/api/v1/roles \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

#### POST /users/{userId}/roles

Assign roles to a user.

**Headers:**
- `Authorization: Bearer <access_token>`

**Path Parameters:**
- `userId` - User UUID

**Request Body:**
```json
{
  "userId": "string",
  "roleNames": ["string"]
}
```

**Response (204 No Content):**
No response body

**Error Responses:**
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Missing or invalid token
- `404 Not Found` - User or role not found

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/users/550e8400-e29b-41d4-a716-446655440000/roles \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "roleNames": ["USER", "MANAGER"]
  }'
```

---

#### GET /users/{userId}/roles

Get roles assigned to a user.

**Headers:**
- `Authorization: Bearer <access_token>`

**Path Parameters:**
- `userId` - User UUID

**Response (200 OK):**
```json
[
  "USER",
  "MANAGER"
]
```

**Error Responses:**
- `401 Unauthorized` - Missing or invalid token
- `404 Not Found` - User not found

**Example:**
```bash
curl -X GET http://localhost:8080/api/v1/users/550e8400-e29b-41d4-a716-446655440000/roles \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

#### DELETE /users/{userId}/roles

Remove roles from a user.

**Headers:**
- `Authorization: Bearer <access_token>`

**Path Parameters:**
- `userId` - User UUID

**Request Body:**
```json
{
  "userId": "string",
  "roleNames": ["string"]
}
```

**Response (204 No Content):**
No response body

**Error Responses:**
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Missing or invalid token
- `404 Not Found` - User or role not found

**Example:**
```bash
curl -X DELETE http://localhost:8080/api/v1/users/550e8400-e29b-41d4-a716-446655440000/roles \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "roleNames": ["MANAGER"]
  }'
```

---

## Data Models

### User

```json
{
  "id": "uuid",
  "username": "string",
  "email": "string",
  "givenName": "string",
  "familyName": "string",
  "enabled": "boolean",
  "accountNonExpired": "boolean",
  "accountNonLocked": "boolean",
  "credentialsNonExpired": "boolean",
  "mfaEnabled": "boolean",
  "createdAt": "timestamp",
  "updatedAt": "timestamp",
  "lastLoginAt": "timestamp"
}
```

### Role

```json
{
  "id": "uuid",
  "name": "string",
  "description": "string",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### Token Response

```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "tokenType": "string",
  "expiresIn": "number"
}
```

### Session

```json
{
  "id": "uuid",
  "userId": "uuid",
  "accessTokenJti": "string",
  "createdAt": "timestamp",
  "expiresAt": "timestamp",
  "revoked": "boolean",
  "revokedAt": "timestamp"
}
```

---

## Error Handling

All error responses follow a standard format:

```json
{
  "error": "string",
  "message": "string",
  "status": "number",
  "timestamp": "string"
}
```

### Common HTTP Status Codes

| Code | Description |
|------|-------------|
| `200 OK` | Request successful |
| `201 Created` | Resource created successfully |
| `204 No Content` | Request successful, no content returned |
| `400 Bad Request` | Invalid request parameters or body |
| `401 Unauthorized` | Missing or invalid authentication |
| `403 Forbidden` | Access denied |
| `404 Not Found` | Resource not found |
| `409 Conflict` | Resource already exists |
| `500 Internal Server Error` | Server error |

### Error Examples

**400 Bad Request:**
```json
{
  "error": "Validation Error",
  "message": "Username is required",
  "status": 400,
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**401 Unauthorized:**
```json
{
  "error": "Unauthorized",
  "message": "Invalid username or password",
  "status": 401,
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**404 Not Found:**
```json
{
  "error": "Not Found",
  "message": "User not found",
  "status": 404,
  "timestamp": "2025-01-01T00:00:00Z"
}
```

---

## Security

### Password Requirements

Passwords should meet these minimum requirements:
- At least 8 characters long
- Contains uppercase and lowercase letters
- Contains at least one number
- Contains at least one special character

### Token Security

- **Access Tokens**: Short-lived (default: 15 minutes)
- **Refresh Tokens**: Longer-lived (default: 7 days)
- **Token Storage**: Tokens should be stored securely (e.g., httpOnly cookies)
- **Token Transmission**: Always use HTTPS in production

### Rate Limiting

Consider implementing rate limiting for:
- Login attempts: 5 attempts per 15 minutes
- Password reset: 3 attempts per hour
- Token refresh: 10 attempts per hour

### Audit Logging

The system logs:
- All authentication attempts
- User account changes
- Role assignments
- Password changes
- Session activity

---

## Testing

### Using cURL

Save your access token for repeated use:

```bash
export ACCESS_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Then use it in requests
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/v1/auth/userinfo
```

### Using Postman

1. Import the API collection
2. Set environment variable `baseUrl` to `http://localhost:8080/api/v1`
3. Authenticate to get token
4. Set `accessToken` environment variable
5. Use `{{accessToken}}` in Authorization header

### Using HTTPie

```bash
# Login
http POST :8080/api/v1/auth/login username=admin password=admin123

# Use token
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
http GET :8080/api/v1/auth/userinfo "Authorization: Bearer $TOKEN"
```

---

## Support

For API support and issues:
- Review the [Integration Guide](INTEGRATION.md)
- Check the [Main README](README.md)
- Report issues in the project repository
- Contact the Firefly Platform team

## License

Copyright 2025 Firefly Software Solutions Inc

Licensed under the Apache License, Version 2.0
