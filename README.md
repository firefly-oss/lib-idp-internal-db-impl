# Firefly IDP - Internal Database Implementation

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)

A production-ready, self-contained Identity Provider (IDP) implementation for the Firefly Banking Platform. This adapter provides database-backed authentication and authorization using PostgreSQL, R2DBC, and JWT tokens—no external IDP services required.

## Overview

The **lib-idp-internal-db-impl** is a reactive, high-performance identity provider that implements the Firefly IDP Adapter interface. It provides complete user authentication, authorization, and session management capabilities using an internal PostgreSQL database.

### Key Features

- ✅ **Self-Contained**: Standalone authentication without external dependencies (no Keycloak, Cognito, etc.)
- ✅ **Reactive Architecture**: Built on Spring WebFlux and R2DBC for non-blocking, high-throughput operations
- ✅ **JWT-Based Authentication**: Secure stateless authentication with access and refresh tokens
- ✅ **BCrypt Password Hashing**: Industry-standard password security with configurable strength
- ✅ **PostgreSQL Storage**: Production-ready database storage via R2DBC reactive driver
- ✅ **Role-Based Access Control (RBAC)**: Comprehensive user and role management with flexible permissions
- ✅ **Session Management**: Active session tracking and management capabilities
- ✅ **Token Refresh Flow**: Secure refresh token mechanism with automatic rotation
- ✅ **Database Migrations**: Automated schema management using Flyway
- ✅ **Security Center Integration**: Seamless integration with Firefly Security Center via auto-configuration
- ✅ **Fully Tested**: 31 passing unit tests covering all critical paths

### When to Use This IDP

This internal database IDP is ideal for:

- 🧪 **Development & Testing**: Quick setup without complex external IDP configuration
- 🏗️ **Microservices**: Standalone authentication for independent services
- 🚀 **Simple Requirements**: Projects not requiring advanced SSO or federated identity
- 🔒 **Self-Hosted Deployments**: Complete control over authentication data and infrastructure
- ⚡ **High-Performance Applications**: Reactive architecture for maximum throughput
- 🏢 **Internal Applications**: Corporate tools requiring basic authentication and RBAC

## Architecture

### Component Structure

```
lib-idp-internal-db-impl/
├── adapter/
│   └── InternalDbIdpAdapter.java      # IdpAdapter interface implementation
├── config/
│   ├── InternalDbIdpConfiguration.java # Spring configuration and beans
│   └── InternalDbProperties.java       # Configuration properties binding
├── domain/
│   ├── User.java                       # User entity with credentials
│   ├── Role.java                       # Role entity
│   ├── UserRole.java                   # User-role assignment junction
│   ├── Session.java                    # Active session tracking
│   └── RefreshToken.java               # Refresh token storage
├── repository/
│   ├── UserRepository.java             # R2DBC user data access
│   ├── RoleRepository.java             # R2DBC role data access
│   ├── UserRoleRepository.java         # R2DBC user-role assignments
│   ├── SessionRepository.java          # R2DBC session tracking
│   └── RefreshTokenRepository.java     # R2DBC token management
├── service/
│   ├── AuthenticationService.java      # Login, logout, token validation
│   ├── UserManagementService.java      # User CRUD operations
│   ├── RoleService.java                # Role and permission management
│   └── JwtTokenService.java            # JWT token generation/validation
└── resources/
    └── db/migration/
        └── V1__Create_IDP_Tables.sql   # Database schema initialization
```

### Database Schema

The implementation uses five core tables:

| Table | Description |
|-------|-------------|
| `idp_users` | User accounts with credentials, profile info, and account status |
| `idp_roles` | Available roles in the system |
| `idp_user_roles` | Many-to-many junction table for user-role assignments |
| `idp_sessions` | Active user sessions with access token tracking |
| `idp_refresh_tokens` | Refresh tokens for secure token renewal |

### Technology Stack

- **Java 21**: Modern Java language features and performance
- **Spring Boot 3.x**: Enterprise application framework
- **Spring WebFlux**: Reactive web framework for non-blocking I/O
- **Spring Data R2DBC**: Reactive database access
- **PostgreSQL**: Production-ready relational database
- **Flyway**: Database migration management
- **JJWT**: JWT token creation and validation
- **Spring Security Crypto**: BCrypt password hashing
- **Lombok**: Reduced boilerplate code

## Quick Start

### 1. Add Dependency

Add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-idp-internal-db-impl</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure Application

Add to your `application.yml`:

```yaml
firefly:
  security-center:
    idp:
      provider: internal-db
      internal-db:
        jwt:
          secret: "your-very-secure-secret-key-min-256-bits-32-chars!"
          issuer: "firefly-banking-platform"
          access-token-expiration: 900000      # 15 minutes
          refresh-token-expiration: 604800000  # 7 days

# R2DBC Database Configuration
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/firefly
    username: firefly_user
    password: ${DB_PASSWORD}
    
  # Flyway for migrations (uses JDBC)
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
  
  datasource:
    url: jdbc:postgresql://localhost:5432/firefly
    username: firefly_user
    password: ${DB_PASSWORD}
```

### 3. Database Setup

The database schema is automatically created via Flyway migrations on startup. Ensure your PostgreSQL database exists:

```bash
createdb firefly
```

### 4. Default Credentials

The migration creates a default admin user:
- **Username**: `admin`
- **Password**: `admin123`

⚠️ **IMPORTANT**: Change this password immediately in production!

## Configuration Reference

### JWT Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `firefly.security-center.idp.internal-db.jwt.secret` | JWT signing secret (min 256 bits) | **Required** |
| `firefly.security-center.idp.internal-db.jwt.issuer` | JWT issuer claim | `firefly-idp-internal-db` |
| `firefly.security-center.idp.internal-db.jwt.access-token-expiration` | Access token lifetime (ms) | `900000` (15 min) |
| `firefly.security-center.idp.internal-db.jwt.refresh-token-expiration` | Refresh token lifetime (ms) | `604800000` (7 days) |

### Database Tables

The implementation creates the following tables:

- **`idp_users`**: User accounts with credentials
- **`idp_roles`**: Available roles
- **`idp_user_roles`**: User-role assignments
- **`idp_sessions`**: Active user sessions
- **`idp_refresh_tokens`**: Refresh tokens for token renewal

### Database Schema Details

#### Users Table (`idp_users`)

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `username` | VARCHAR(255) | Unique username |
| `email` | VARCHAR(255) | Email address (nullable) |
| `password_hash` | VARCHAR(255) | BCrypt hashed password |
| `first_name` | VARCHAR(100) | User's first name |
| `last_name` | VARCHAR(100) | User's last name |
| `enabled` | BOOLEAN | Account enabled flag |
| `account_non_expired` | BOOLEAN | Account expiration flag |
| `account_non_locked` | BOOLEAN | Account lock flag |
| `credentials_non_expired` | BOOLEAN | Password expiration flag |
| `mfa_enabled` | BOOLEAN | MFA status (reserved for future use) |
| `created_at` | TIMESTAMP | Account creation timestamp |
| `updated_at` | TIMESTAMP | Last update timestamp |
| `last_login_at` | TIMESTAMP | Last successful login |

#### Default Roles

Three roles are automatically created during database initialization:

- **`ADMIN`**: Full administrative access to all system features
- **`MANAGER`**: Elevated permissions for management tasks
- **`USER`**: Standard user access with basic permissions

## Usage Examples

### Login

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

### Create User

**Note:** These examples assume the Security Center is running and exposing the endpoints.

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "password": "SecurePassword123!",
    "email": "john.doe@example.com",
    "givenName": "John",
    "familyName": "Doe"
  }'
```

### Assign Roles

```bash
curl -X POST http://localhost:8080/api/v1/users/{userId}/roles \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "roleNames": ["USER", "MANAGER"]
  }'
```

### Get User Info

```bash
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### Refresh Token

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "$REFRESH_TOKEN"
  }'
```

### Logout

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "accessToken": "$ACCESS_TOKEN",
    "refreshToken": "$REFRESH_TOKEN"
  }'
```

## Security Considerations

### Production Deployment

When deploying to production:

1. **Change Default Password**: Immediately change the default admin password
2. **Secure JWT Secret**: Use a strong, randomly generated secret (32+ characters)
3. **Environment Variables**: Store secrets in environment variables, not configuration files
4. **Database Security**: Use strong database credentials and restrict network access
5. **TLS/SSL**: Always use HTTPS in production
6. **Token Expiration**: Adjust token expiration times based on security requirements
7. **Password Policy**: Implement password complexity requirements in your application layer

### JWT Secret Generation

Generate a secure JWT secret:

```bash
# Using OpenSSL
openssl rand -base64 32

# Using Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

## Limitations

This internal IDP implementation has some limitations compared to full-featured IDPs:

- ❌ **No MFA**: Multi-factor authentication not implemented
- ❌ **No OAuth2 flows**: Only username/password authentication
- ❌ **No Social Login**: No integration with Google, Facebook, etc.
- ❌ **No LDAP/AD**: No integration with enterprise directories
- ❌ **Basic Session Management**: Limited session tracking features

For advanced features, consider using `lib-idp-keycloak-impl` or `lib-idp-aws-cognito-impl`.

## Adapter Implementation

This library implements the `IdpAdapter` interface from `lib-idp-adapter`. It **does not expose REST endpoints directly**. Instead, it provides the backend implementation that the **Firefly Security Center** uses to handle authentication requests.

### Implemented Methods

The `InternalDbIdpAdapter` class implements these methods:

| Method | Description | Returns |
|--------|-------------|--------|
| `login(LoginRequest)` | Authenticate user with username/password | `Mono<ResponseEntity<TokenResponse>>` |
| `refresh(RefreshRequest)` | Refresh access token using refresh token | `Mono<ResponseEntity<TokenResponse>>` |
| `logout(LogoutRequest)` | Revoke user tokens and logout | `Mono<Void>` |
| `introspect(String)` | Validate and introspect an access token | `Mono<ResponseEntity<IntrospectionResponse>>` |
| `getUserInfo(String)` | Get user information from access token | `Mono<ResponseEntity<UserInfoResponse>>` |
| `createUser(CreateUserRequest)` | Create a new user account | `Mono<ResponseEntity<CreateUserResponse>>` |
| `updateUser(UpdateUserRequest)` | Update user information | `Mono<ResponseEntity<UpdateUserResponse>>` |
| `deleteUser(DeleteUserRequest)` | Delete a user account | `Mono<Void>` |
| `assignRoles(AssignRolesRequest)` | Assign roles to a user | `Mono<Void>` |
| `removeRoles(RemoveRolesRequest)` | Remove roles from a user | `Mono<Void>` |
| `getUserSessions(GetUserSessionsRequest)` | List user's active sessions | `Mono<ResponseEntity<UserSessionsResponse>>` |
| `createRoles(CreateRolesRequest)` | Create new roles | `Mono<ResponseEntity<CreateRolesResponse>>` |
| `listRoles()` | List all available roles | `Mono<ResponseEntity<RoleListResponse>>` |

### REST Endpoints (Exposed by Security Center)

When integrated with the Firefly Security Center, the following REST endpoints become available:

**Authentication:**
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/auth/logout` - User logout
- `POST /api/v1/auth/introspect` - Token introspection
- `GET /api/v1/auth/userinfo` - Get user info

**User Management:**
- `POST /api/v1/users` - Create user
- `GET /api/v1/users/{id}` - Get user
- `PUT /api/v1/users/{id}` - Update user
- `DELETE /api/v1/users/{id}` - Delete user

**Role Management:**
- `POST /api/v1/roles` - Create roles
- `GET /api/v1/roles` - List roles
- `POST /api/v1/users/{id}/roles` - Assign roles
- `DELETE /api/v1/users/{id}/roles` - Remove roles
- `GET /api/v1/users/{id}/sessions` - Get user sessions

**Note:** The Security Center handles HTTP routing, request validation, and response serialization. This adapter focuses solely on business logic and data persistence.

For detailed API documentation, see the [Security Center API Documentation](https://github.com/firefly-oss/common-platform-security-center).

## Integration with Firefly Security Center

This adapter integrates seamlessly with the `common-platform-security-center` via Spring Boot auto-configuration:

```yaml
firefly:
  security-center:
    idp:
      provider: internal-db  # Automatically loads this implementation
```

### Auto-Configuration

When `provider: internal-db` is set, the Security Center:
1. **Auto-detects** this library on the classpath
2. **Auto-configures** the `InternalDbIdpAdapter` bean
3. **Activates** all repositories and services
4. **Applies** database migrations via Flyway
5. **Exposes** unified authentication endpoints

No additional configuration or manual bean registration is required.

## Development

### Building the Project

```bash
# Clean and build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Build and install to local Maven repository
mvn clean install
```

### Running Tests

The project includes comprehensive unit tests with mocks:

```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=AuthenticationServiceTest

# Run with verbose output
mvn test -X
```

**Test Coverage:**
- ✅ 31 unit tests
- ✅ 100% service layer coverage
- ✅ All authentication flows tested
- ✅ User management operations verified
- ✅ Role management operations validated

### Code Quality

The codebase follows enterprise Java standards:

- **Java 21** language features
- **Lombok** for reduced boilerplate
- **SLF4J** for structured logging
- **Reactive patterns** throughout
- **Comprehensive error handling**
- **Detailed JavaDoc comments**

## Troubleshooting

### Common Issues

#### 1. Migration Issues

**Problem:** Flyway migrations fail on startup

**Solution:**
```bash
# Reset database
dropdb firefly
createdb firefly

# Or via SQL
DROP DATABASE firefly;
CREATE DATABASE firefly;
```

Ensure both R2DBC and JDBC URLs are correctly configured for the same database.

#### 2. JWT Secret Error

**Problem:** `JWT secret is required` exception on startup

**Solution:**
- Verify `firefly.security-center.idp.internal-db.jwt.secret` is configured
- Secret must be **at least 32 characters** (256 bits)
- Use environment variable: `JWT_SECRET=your-secure-secret-here`

**Generate a secure secret:**
```bash
# Using OpenSSL
openssl rand -base64 32

# Using Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

#### 3. Database Connection Issues

**Problem:** Cannot connect to PostgreSQL

**Checklist:**
- [ ] PostgreSQL is running: `pg_ctl status`
- [ ] Database exists: `psql -l | grep firefly`
- [ ] Credentials are correct
- [ ] Both R2DBC and JDBC URLs match your setup
- [ ] Port 5432 is accessible
- [ ] Firewall rules allow connections

**Test connection:**
```bash
psql -h localhost -U firefly_user -d firefly
```

#### 4. BCrypt Hash Mismatch

**Problem:** Authentication fails with correct password

**Cause:** Password may have been stored without BCrypt hashing

**Solution:** Use the provided BCrypt utility to generate proper hashes:
```java
PasswordEncoder encoder = new BCryptPasswordEncoder(12);
String hash = encoder.encode("password");
```

#### 5. Token Validation Failures

**Problem:** Valid tokens are rejected

**Common Causes:**
- JWT secret mismatch between services
- System clock skew (check NTP sync)
- Token expired (check `expiresIn` configuration)

**Debug:**
```yaml
logging:
  level:
    com.firefly.idp.internaldb: DEBUG
    io.jsonwebtoken: DEBUG
```

## Performance Considerations

### Connection Pooling

The R2DBC driver uses connection pooling for optimal performance:

```yaml
spring:
  r2dbc:
    pool:
      initial-size: 10      # Initial connections
      max-size: 30          # Maximum connections
      max-idle-time: 30m    # Idle connection timeout
      validation-query: SELECT 1
```

### Token Expiration Strategy

**Recommended Settings:**

| Environment | Access Token | Refresh Token |
|-------------|--------------|---------------|
| Development | 1 hour (3600000ms) | 30 days |
| Staging | 15 minutes (900000ms) | 7 days |
| Production | 5-15 minutes | 7 days |

### Database Indexing

The migration creates indexes on:
- `idp_users.username` (unique)
- `idp_users.email` (unique)
- `idp_sessions.access_token_jti`
- `idp_refresh_tokens.token_jti`
- `idp_user_roles (user_id, role_id)`

## Limitations

This internal IDP has some limitations compared to enterprise IDP solutions:

| Feature | Status | Alternative |
|---------|--------|-------------|
| Multi-Factor Authentication (MFA) | ❌ Not implemented | Use `lib-idp-keycloak-impl` |
| OAuth2 Authorization Flows | ❌ Password grant only | Use `lib-idp-keycloak-impl` or `lib-idp-aws-cognito-impl` |
| Social Login (Google, Facebook) | ❌ Not supported | Use `lib-idp-keycloak-impl` |
| LDAP/Active Directory Integration | ❌ Not supported | Use `lib-idp-keycloak-impl` |
| Advanced Session Management | ⚠️ Basic implementation | Consider external IDP |
| User Federation | ❌ Not supported | Use `lib-idp-keycloak-impl` |
| Single Sign-On (SSO) | ❌ Not supported | Use `lib-idp-keycloak-impl` |

For advanced enterprise features, consider:
- **lib-idp-keycloak-impl**: Full OAuth2/OIDC with SSO, MFA, and federation
- **lib-idp-aws-cognito-impl**: AWS-managed identity with advanced security features

## Contributing

We welcome contributions! To contribute:

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/my-feature`
3. **Commit** your changes: `git commit -am 'Add new feature'`
4. **Push** to the branch: `git push origin feature/my-feature`
5. **Submit** a pull request

### Contribution Guidelines

- ✅ All tests must pass (`mvn test`)
- ✅ Follow existing code style and conventions
- ✅ Add unit tests for new features
- ✅ Update documentation for API changes
- ✅ Use meaningful commit messages
- ✅ Keep pull requests focused and atomic

## License

```
Copyright 2025 Firefly Software Solutions Inc

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Additional Documentation

Comprehensive documentation is available in the [docs/](docs/) directory:

| Document | Description |
|----------|-------------|
| **[Integration Guide](docs/INTEGRATION.md)** | Complete guide for integrating with Firefly Security Center |
| **[API Documentation](docs/API.md)** | Full API reference with request/response examples |
| **[DTO Field Mapping](docs/DTO_FIELD_MAPPING.md)** | Field mappings between DTOs and entities |
| **[Technical Details](docs/technical-details.md)** | Implementation details and architecture diagrams |
| **[Project Status](docs/PROJECT_STATUS.md)** | Current project status and feature completeness |

## Support & Contact

### Getting Help

- 🐛 **Bug Reports**: [Create an issue](https://github.com/firefly-oss/lib-idp-internal-db-impl/issues)
- 💬 **Questions**: Use GitHub Discussions
- 📧 **Email**: support@firefly-platform.com
- 📚 **Documentation**: See [Firefly Platform Docs](https://docs.firefly-platform.com)

### Community

- **GitHub**: [firefly-oss/lib-idp-internal-db-impl](https://github.com/firefly-oss/lib-idp-internal-db-impl)
- **Organization**: [Firefly OSS](https://github.com/firefly-oss)

## Related Projects

- **[lib-idp-adapter](https://github.com/firefly-oss/lib-idp-adapter)** - Core IDP adapter interface
- **[common-platform-security-center](https://github.com/firefly-oss/common-platform-security-center)** - Firefly Security Center
- **[lib-idp-keycloak-impl](https://github.com/firefly-oss/lib-idp-keycloak-impl)** - Keycloak IDP implementation
- **[lib-idp-aws-cognito-impl](https://github.com/firefly-oss/lib-idp-aws-cognito-impl)** - AWS Cognito IDP implementation

---

**Made with ❤️ by the Firefly Platform Team**
