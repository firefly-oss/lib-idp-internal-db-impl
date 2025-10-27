# Contributing to Internal Database IDP

Thank you for your interest in contributing to the Firefly Internal Database IDP implementation!

## How to Contribute

### Reporting Issues

If you find a bug or have a feature request:

1. Check if the issue already exists in the issue tracker
2. If not, create a new issue with:
   - Clear title and description
   - Steps to reproduce (for bugs)
   - Expected vs actual behavior
   - Environment details (Java version, database version, etc.)
   - Code samples or logs if applicable

### Submitting Changes

1. **Fork the repository** and create your branch from `main`
2. **Make your changes** following the code style guidelines
3. **Add tests** for any new functionality
4. **Update documentation** if you're changing functionality
5. **Ensure all tests pass**: `mvn test`
6. **Submit a pull request** with a clear description of changes

## Development Setup

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 12+ (for integration testing)
- Git

### Build the Project

```bash
# Clone the repository
git clone <repository-url>
cd lib-idp-internal-db-impl

# Build and run tests
mvn clean install

# Run tests only
mvn test
```

## Code Style Guidelines

### Java Code

- Follow standard Java naming conventions
- Use meaningful variable and method names
- Keep methods focused and concise
- Add JavaDoc comments for public APIs
- Use Lombok annotations to reduce boilerplate

Example:
```java
/**
 * Authenticates a user with username and password.
 * 
 * @param username the user's username
 * @param password the user's password
 * @return Mono emitting TokenResponse on success
 */
public Mono<TokenResponse> authenticate(String username, String password) {
    // Implementation
}
```

### Testing

- Write unit tests for all new functionality
- Use descriptive test method names: `shouldDoSomethingWhenCondition()`
- Mock external dependencies
- Ensure tests are independent and can run in any order

Example:
```java
@Test
void shouldAuthenticateUserWithValidCredentials() {
    // Given
    when(userRepository.findByUsername("user")).thenReturn(Mono.just(testUser));
    when(passwordEncoder.matches(any(), any())).thenReturn(true);
    
    // When & Then
    StepVerifier.create(authService.authenticate("user", "pass"))
        .assertNext(response -> {
            assertThat(response.getAccessToken()).isNotBlank();
        })
        .verifyComplete();
}
```

### Documentation

- Write clear, concise documentation
- Use American English spelling
- Include code examples
- Update the relevant docs in `docs/` directory
- Keep the main README up to date

## Pull Request Guidelines

### Before Submitting

- [ ] All tests pass: `mvn clean test`
- [ ] Code follows style guidelines
- [ ] New functionality has tests
- [ ] Documentation is updated
- [ ] Commit messages are clear and descriptive
- [ ] Branch is up to date with `main`

### Pull Request Description

Include:
- **Summary**: What does this PR do?
- **Motivation**: Why is this change needed?
- **Changes**: List of changes made
- **Testing**: How was this tested?
- **Screenshots**: If applicable
- **Breaking Changes**: Any breaking changes?

## Commit Message Guidelines

Use clear, descriptive commit messages:

```
feat: add user account locking feature
fix: resolve NPE in token validation
docs: update API documentation for refresh endpoint
test: add tests for role assignment
refactor: simplify authentication service logic
```

Prefixes:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `test`: Adding or updating tests
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `chore`: Maintenance tasks

## Testing Requirements

### Unit Tests

All new functionality must have unit tests:

```bash
# Run unit tests
mvn test

# Run specific test class
mvn test -Dtest=UserManagementServiceTest
```

### Test Coverage

- Aim for high test coverage
- Focus on critical paths and edge cases
- Mock external dependencies

## Code Review Process

1. Submit pull request
2. Automated tests run
3. Code review by maintainers
4. Address feedback if needed
5. Approval and merge

## Security

### Reporting Security Issues

**DO NOT** create public issues for security vulnerabilities.

Instead, email security concerns to: security@firefly-platform.com

### Security Guidelines

- Never commit secrets, passwords, or API keys
- Use environment variables for sensitive configuration
- Follow security best practices in code
- Validate and sanitize all user inputs
- Use parameterized queries (R2DBC handles this)

## Questions?

- Check the [documentation](docs/)
- Review existing issues and pull requests
- Ask questions in pull request comments
- Contact the maintainers

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.

## Thank You!

Your contributions help make this project better for everyone. We appreciate your time and effort!
