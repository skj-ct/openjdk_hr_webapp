# Test Execution Guide

## Overview

This guide explains how to run different types of tests in the HR Web Application migration project.

## Test Categories

### 1. Unit Tests (Default)
Unit tests run by default and don't require external dependencies.

```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=EmployeeTest

# Run tests with coverage report
mvn clean test jacoco:report
```

### 2. Integration Tests (Conditional)
Integration tests require Docker and TestContainers. They are disabled by default.

```bash
# Enable and run integration tests (requires Docker)
mvn test -Pintegration-tests -Dintegration.tests.enabled=true

# Run only integration tests
mvn failsafe:integration-test -Pintegration-tests -Dintegration.tests.enabled=true

# Run all tests including integration tests
mvn clean verify -Pintegration-tests -Dintegration.tests.enabled=true
```

## Test Configuration

### Environment Requirements

#### Unit Tests
- Java 21+
- Maven 3.8+
- No external dependencies

#### Integration Tests
- Java 21+
- Maven 3.8+
- Docker Desktop or Docker Engine
- TestContainers support

### System Properties

| Property | Default | Description |
|----------|---------|-------------|
| `integration.tests.enabled` | `false` | Enable integration tests |
| `testcontainers.reuse.enable` | `false` | Reuse TestContainers between runs |

### Docker Configuration

Integration tests use TestContainers with PostgreSQL 17:

```yaml
# TestContainers Configuration
postgres:
  image: postgres:17-alpine
  database: hrdb_test
  username: hr_test
  password: hr_test_password
  init_script: test-schema.sql
```

## Test Profiles

### Development Profile (Default)
```bash
mvn test
```
- Runs unit tests only
- Fast execution
- No external dependencies

### Integration Profile
```bash
mvn test -Pintegration-tests -Dintegration.tests.enabled=true
```
- Runs all tests including integration tests
- Requires Docker
- Slower execution but comprehensive coverage

## Troubleshooting

### Common Issues

#### 1. Integration Tests Fail with Docker Error
```
IllegalStateException: Could not find a valid Docker environment
```

**Solution:**
- Ensure Docker is running
- Run unit tests only: `mvn test`
- Or install Docker Desktop

#### 2. BigDecimal Precision Errors
```
Expected: 105000.00, Actual: 105000
```

**Solution:**
- Tests now use `BigDecimal.compareTo()` for precision-safe comparisons
- This is automatically handled in the fixed test suite

#### 3. Mockito Stubbing Exceptions
```
UnnecessaryStubbingException: Unnecessary stubbings detected
```

**Solution:**
- Tests have been updated to remove unnecessary stubs
- Use `@MockitoSettings(strictness = Strictness.LENIENT)` if needed

#### 4. Content Type Mismatches in Web Tests
```
Expected: application/json, Actual: text/css
```

**Solution:**
- Web tests now properly mock response content types
- Servlet tests use correct HTTP method setup

## Test Coverage

### Coverage Targets
- **Entity Layer**: >95%
- **Business Logic**: >80%
- **Web Layer**: >85%
- **Configuration**: >90%

### Coverage Reports
```bash
# Generate coverage report
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

## Continuous Integration

### GitHub Actions / Jenkins
```yaml
# Unit tests (always run)
- name: Run Unit Tests
  run: mvn clean test

# Integration tests (conditional)
- name: Run Integration Tests
  run: mvn clean verify -Pintegration-tests -Dintegration.tests.enabled=true
  if: env.DOCKER_AVAILABLE == 'true'
```

### Local Development
```bash
# Quick feedback loop (unit tests only)
mvn test

# Full validation before commit
mvn clean verify -Pintegration-tests -Dintegration.tests.enabled=true
```

## Performance Expectations

| Test Type | Duration | Resource Usage |
|-----------|----------|----------------|
| Unit Tests | <30 seconds | Low CPU/Memory |
| Integration Tests | 2-5 minutes | High CPU/Memory + Docker |
| Full Test Suite | 3-6 minutes | High CPU/Memory + Docker |

## Best Practices

1. **Run unit tests frequently** during development
2. **Run integration tests** before committing major changes
3. **Use test profiles** to control execution scope
4. **Monitor test performance** and optimize slow tests
5. **Keep Docker running** if doing integration test development
6. **Use meaningful test names** and descriptions
7. **Maintain test independence** - tests should not depend on each other