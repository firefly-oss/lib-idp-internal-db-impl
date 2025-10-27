# Integration Guide: Internal DB IDP with Firefly Security Center

This document provides a comprehensive guide for integrating the Internal Database IDP implementation with the Firefly Security Center.

## Overview

The `lib-idp-internal-db-impl` is a self-contained Identity Provider (IDP) adapter that integrates seamlessly with the `common-platform-security-center`. It provides database-backed user authentication without requiring external IDP services like Keycloak or AWS Cognito.

## Architecture

```
┌─────────────────────────────────────────────────┐
│   Firefly Security Center                       │
│   (common-platform-security-center-core)       │
│                                                 │
│   ┌─────────────────────────────────┐          │
│   │  IdpAutoConfiguration           │          │
│   │  - Auto-detects provider        │          │
│   │  - Loads appropriate adapter     │          │
│   └──────────────┬──────────────────┘          │
│                  │                              │
│                  │ detects provider=internal-db │
│                  ▼                              │
│   ┌─────────────────────────────────┐          │
│   │  InternalDbIdpAdapter           │◄─────────┼─ lib-idp-internal-db-impl
│   │  - Implements IdpAdapter        │          │
│   │  - Handles authentication       │          │
│   └──────────────┬──────────────────┘          │
│                  │                              │
└──────────────────┼──────────────────────────────┘
                   │
                   ▼
   ┌────────────────────────────────────┐
   │  Services Layer                    │
   │  - AuthenticationService           │
   │  - UserManagementService           │
   │  - RoleService                     │
   │  - JwtTokenService                 │
   └────────────────┬───────────────────┘
                    │
                    ▼
   ┌────────────────────────────────────┐
   │  PostgreSQL Database (R2DBC)       │
   │  - idp_users                       │
   │  - idp_roles                       │
   │  - idp_sessions                    │
   │  - idp_refresh_tokens              │
   └────────────────────────────────────┘
```

## Prerequisites

- Java 21+
- Spring Boot 3.x
- PostgreSQL 12+
- Maven 3.8+

## Integration Steps

### 1. Add Dependencies

Add both the Security Center core and the Internal DB IDP implementation to your `pom.xml`:

```xml
<dependencies>
    <!-- Firefly Security Center Core -->
    <dependency>
        <groupId>com.firefly</groupId>
        <artifactId>common-platform-security-center-core</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- Internal Database IDP Implementation -->
    <dependency>
        <groupId>com.firefly</groupId>
        <artifactId>lib-idp-internal-db-impl</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- PostgreSQL R2DBC Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>r2dbc-postgresql</artifactId>
    </dependency>

    <!-- PostgreSQL JDBC Driver (for Flyway) -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Flyway for migrations -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
</dependencies>
```

### 2. Configure Application

Add the following to your `application.yml`:

```yaml
firefly:
  security-center:
    idp:
      # Select internal-db as the IDP provider
      provider: internal-db
      
      # Internal DB specific configuration
      internal-db:
        jwt:
          # REQUIRED: JWT secret key (minimum 32 characters / 256 bits)
          secret: ${IDP_JWT_SECRET:changeme-use-secure-secret-min-32chars!}
          
          # JWT issuer identifier
          issuer: ${IDP_JWT_ISSUER:firefly-banking-platform}
          
          # Access token expiration in milliseconds (default: 15 minutes)
          access-token-expiration: 900000
          
          # Refresh token expiration in milliseconds (default: 7 days)
          refresh-token-expiration: 604800000

# Spring R2DBC configuration for reactive database access
spring:
  r2dbc:
    url: ${DB_R2DBC_URL:r2dbc:postgresql://localhost:5432/firefly}
    username: ${DB_USERNAME:firefly_user}
    password: ${DB_PASSWORD}
    pool:
      enabled: true
      initial-size: 10
      max-size: 20
      max-idle-time: 30m
      max-acquire-time: 3s

  # Spring JDBC DataSource (required for Flyway migrations)
  datasource:
    url: ${DB_JDBC_URL:jdbc:postgresql://localhost:5432/firefly}
    username: ${DB_USERNAME:firefly_user}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  # Flyway configuration for automated database migrations
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    table: flyway_schema_history
```

### 3. Set Environment Variables

For production deployments, use environment variables for sensitive configuration:

```bash
# Required: JWT secret (generate a secure random string)
export IDP_JWT_SECRET="your-secure-random-32-char-secret!"

# Database credentials
export DB_PASSWORD="secure_database_password"

# Optional: Override defaults
export IDP_JWT_ISSUER="my-application-name"
export DB_R2DBC_URL="r2dbc:postgresql://db.example.com:5432/production"
export DB_JDBC_URL="jdbc:postgresql://db.example.com:5432/production"
export DB_USERNAME="app_user"
```

### 4. Database Setup

The database schema is automatically created via Flyway migrations on application startup. Ensure your PostgreSQL database exists:

```bash
# Create database
createdb firefly

# Grant permissions
psql -c "GRANT ALL PRIVILEGES ON DATABASE firefly TO firefly_user;"
```

The migration creates:
- User authentication tables
- Role management tables
- Session tracking tables
- Initial admin user with default credentials

## How Auto-Configuration Works

The Security Center's `IdpAutoConfiguration` automatically detects and loads the Internal DB adapter:

```java
@Configuration
@ConditionalOnClass(name = "com.firefly.idp.internaldb.adapter.InternalDbIdpAdapter")
@ConditionalOnProperty(
    prefix = "firefly.security-center.idp", 
    name = "provider", 
    havingValue = "internal-db"
)
@ComponentScan(basePackages = "com.firefly.idp.internaldb")
static class InternalDbIdpConfiguration {
    public InternalDbIdpConfiguration() {
        log.info("Loading Internal Database IDP adapter configuration");
    }
}
```

**Key Points:**
- Auto-loads when `lib-idp-internal-db-impl` is on the classpath
- Only activates when `provider=internal-db` is configured
- Scans and registers all internal DB IDP components
- No manual bean configuration required

## Default Credentials

The Flyway migration (`V1__Create_IDP_Tables.sql`) creates a default admin user:

```
Username: admin
Password: admin123
Roles: ADMIN, MANAGER, USER
```

⚠️ **CRITICAL SECURITY WARNING**: Change the default admin password immediately after first deployment!

```bash
# Change password via API
curl -X PUT http://localhost:8080/api/v1/users/change-password \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "00000000-0000-0000-0000-000000000001",
    "oldPassword": "admin123",
    "newPassword": "YourSecureNewPassword123!"
  }'
```

## API Endpoints

Once integrated, the Security Center exposes these IDP endpoints:

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/login` | Authenticate user and get tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Logout and revoke tokens |
| POST | `/api/v1/auth/introspect` | Validate and introspect token |
| GET | `/api/v1/auth/userinfo` | Get authenticated user information |

### User Management Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/users` | Create new user |
| GET | `/api/v1/users/{id}` | Get user by ID |
| PUT | `/api/v1/users/{id}` | Update user |
| DELETE | `/api/v1/users/{id}` | Delete user |
| PUT | `/api/v1/users/{id}/password` | Change user password |
| PUT | `/api/v1/users/{id}/enable` | Enable/disable user |

### Role Management Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/roles` | Create new role |
| GET | `/api/v1/roles` | List all roles |
| POST | `/api/v1/users/{id}/roles` | Assign roles to user |
| DELETE | `/api/v1/users/{id}/roles` | Remove roles from user |
| GET | `/api/v1/users/{id}/roles` | Get user's roles |

## Usage Examples

### 1. User Authentication

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

### 2. Create User

```bash
export ACCESS_TOKEN="your-access-token"

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

### 3. Assign Roles

```bash
curl -X POST http://localhost:8080/api/v1/users/550e8400-e29b-41d4-a716-446655440000/roles \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "roleNames": ["USER", "MANAGER"]
  }'
```

### 4. Token Refresh

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token"
  }'
```

## Security Considerations

### Production Deployment Checklist

- [ ] Generate and set a strong JWT secret (min 32 characters)
- [ ] Change default admin password
- [ ] Use environment variables for all secrets
- [ ] Enable TLS/SSL (HTTPS)
- [ ] Configure appropriate token expiration times
- [ ] Restrict database network access
- [ ] Use strong database credentials
- [ ] Enable database encryption at rest
- [ ] Configure firewall rules
- [ ] Set up monitoring and alerting
- [ ] Implement rate limiting on authentication endpoints
- [ ] Enable audit logging

### JWT Secret Generation

Generate a cryptographically secure JWT secret:

```bash
# Using OpenSSL (recommended)
openssl rand -base64 32

# Using Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"

# Using Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

## Testing the Integration

### Unit Tests

The implementation includes comprehensive unit tests:

```bash
cd lib-idp-internal-db-impl
mvn test
```

Test coverage:
- ✅ UserManagementService (11 tests)
- ✅ AuthenticationService (8 tests)
- ✅ RoleService (12 tests)
- ✅ Total: 31 unit tests with mocked dependencies

### Integration Testing

To test the full integration with Security Center:

1. **Start PostgreSQL**:
```bash
docker run --name firefly-postgres \
  -e POSTGRES_DB=firefly \
  -e POSTGRES_USER=firefly_user \
  -e POSTGRES_PASSWORD=test_password \
  -p 5432:5432 \
  -d postgres:15-alpine
```

2. **Configure test application**:
```yaml
firefly:
  security-center:
    idp:
      provider: internal-db
      internal-db:
        jwt:
          secret: "test-secret-key-must-be-32-chars!"
```

3. **Run application and test endpoints**:
```bash
# Start application
mvn spring-boot:run

# Test login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## Troubleshooting

### Issue: "No IdpAdapter bean found"

**Cause**: Security Center cannot find the IDP adapter implementation.

**Solution**:
1. Verify `lib-idp-internal-db-impl` is in your dependencies
2. Check that `provider: internal-db` is set in configuration
3. Run `mvn dependency:tree` to confirm the artifact is present

### Issue: "JWT secret is required"

**Cause**: JWT secret configuration is missing or too short.

**Solution**:
1. Set `firefly.security-center.idp.internal-db.jwt.secret` in configuration
2. Ensure secret is at least 32 characters (256 bits)
3. Use environment variable: `export IDP_JWT_SECRET="your-secret"`

### Issue: "Failed to obtain R2DBC Connection"

**Cause**: Database connection issues.

**Solution**:
1. Verify PostgreSQL is running: `pg_isready`
2. Check connection URL and credentials
3. Ensure database exists: `createdb firefly`
4. Verify network connectivity
5. Check R2DBC pool configuration

### Issue: "Flyway migration failed"

**Cause**: Database schema conflicts or permission issues.

**Solution**:
1. Check database user has sufficient permissions
2. Review Flyway logs for specific errors
3. Reset database if necessary:
   ```sql
   DROP DATABASE firefly;
   CREATE DATABASE firefly;
   ```
4. Ensure JDBC URL is correctly configured

### Issue: "Invalid username or password"

**Cause**: Authentication credentials are incorrect.

**Solution**:
1. Verify you're using default credentials: `admin/admin123`
2. Check if admin user exists in database:
   ```sql
   SELECT * FROM idp_users WHERE username = 'admin';
   ```
3. Reset admin password if needed via database update

## Performance Tuning

### Database Connection Pool

Optimize R2DBC connection pool for your workload:

```yaml
spring:
  r2dbc:
    pool:
      initial-size: 20        # Connections on startup
      max-size: 50            # Maximum connections
      max-idle-time: 30m      # Idle timeout
      max-acquire-time: 5s    # Acquisition timeout
```

### Token Expiration

Balance security and user experience:

```yaml
firefly:
  security-center:
    idp:
      internal-db:
        jwt:
          # Short-lived access tokens (recommended)
          access-token-expiration: 900000    # 15 minutes
          
          # Longer refresh tokens
          refresh-token-expiration: 2592000000  # 30 days
```

### Database Indexing

The migrations create these indexes for optimal performance:
- `idx_users_username` - Fast username lookups
- `idx_users_email` - Fast email lookups
- `idx_sessions_user_id` - Efficient session queries
- `idx_sessions_access_token_jti` - Quick token validation
- `idx_refresh_tokens_token_jti` - Fast refresh token lookups

## Migration from Other IDPs

### From Keycloak

1. Export users from Keycloak
2. Transform user data to match Internal DB schema
3. Import using batch user creation API
4. Update application configuration to use `internal-db`

### From AWS Cognito

1. Export user pool data
2. Map Cognito attributes to Internal DB fields
3. Bulk import users
4. Reconfigure application to use `internal-db`

## Support and Documentation

- **Main README**: [README.md](README.md)
- **Security Center Docs**: `../common-platform-security-center/README.md`
- **IDP Adapter Interface**: `../lib-idp-adapter/README.md`
- **API Documentation**: Available via Swagger/OpenAPI when Security Center is running

## License

Copyright 2025 Firefly Software Solutions Inc

Licensed under the Apache License, Version 2.0
