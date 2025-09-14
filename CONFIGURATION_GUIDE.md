# Configuration Guide - HR Web Application

This guide provides comprehensive configuration instructions for deploying the HR Web Application across different environments (development, testing, staging, production) after migration from Oracle to OpenJDK + PostgreSQL.

## Table of Contents

- [Configuration Overview](#configuration-overview)
- [Environment Profiles](#environment-profiles)
- [Database Configuration](#database-configuration)
- [Connection Pool Configuration](#connection-pool-configuration)
- [Security Configuration](#security-configuration)
- [Logging Configuration](#logging-configuration)
- [Performance Configuration](#performance-configuration)
- [Environment Variables](#environment-variables)
- [Docker Configuration](#docker-configuration)
- [Cloud Deployment Configuration](#cloud-deployment-configuration)

## Configuration Overview

### Configuration Hierarchy

The application uses a layered configuration approach with the following precedence (highest to lowest):

1. **JVM System Properties** (`-Dproperty=value`)
2. **Environment Variables** (`PROPERTY_NAME`)
3. **Profile-Specific Properties** (`application-{profile}.properties`)
4. **Default Properties** (`application.properties`)

### Configuration Files Structure

```
src/main/resources/
├── application.properties              # Default configuration
├── application-dev.properties          # Development environment
├── application-test.properties         # Testing environment
├── application-staging.properties      # Staging environment
├── application-prod.properties         # Production environment
├── logback-spring.xml                  # Logging configuration
└── db/migration/                       # Database migration scripts
```

## Environment Profiles

### Development Profile

**File**: `src/main/resources/application-dev.properties`

```properties
# Development Environment Configuration
app.profile=dev
app.name=HR Web Application (Development)
app.version=1.0.0-SNAPSHOT

# Database Configuration
db.url=jdbc:postgresql://localhost:5432/hrdb_dev
db.username=hr_dev_user
db.password=dev_password
db.driver=org.postgresql.Driver
db.schema=public

# HikariCP Configuration - Optimized for development
hikari.maximumPoolSize=5
hikari.minimumIdle=1
hikari.connectionTimeout=10000
hikari.idleTimeout=300000
hikari.maxLifetime=900000
hikari.leakDetectionThreshold=0
hikari.registerMbeans=true

# Development-specific HikariCP settings
hikari.cachePrepStmts=false
hikari.prepStmtCacheSize=25
hikari.prepStmtCacheSqlLimit=256

# Flyway Configuration
flyway.url=${db.url}
flyway.user=${db.username}
flyway.password=${db.password}
flyway.locations=classpath:db/migration,classpath:db/dev-data
flyway.baselineOnMigrate=true
flyway.cleanDisabled=false
flyway.validateOnMigrate=true

# Logging Configuration
logging.level.root=INFO
logging.level.com.hrapp.jdbc.samples=DEBUG
logging.level.com.zaxxer.hikari=DEBUG
logging.level.org.flywaydb=INFO
logging.level.org.postgresql=INFO
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# Development Features
app.debug.enabled=true
app.cache.enabled=false
app.metrics.enabled=true
app.health.detailed=true

# Development-specific settings
server.port=8080
server.servlet.context-path=/HRWebApp
```

### Testing Profile

**File**: `src/test/resources/application-test.properties`

```properties
# Testing Environment Configuration
app.profile=test
app.name=HR Web Application (Test)
app.version=1.0.0-TEST

# Test Database Configuration (H2 for unit tests)
db.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL
db.username=sa
db.password=
db.driver=org.h2.Driver
db.schema=public

# TestContainers Configuration (for integration tests)
testcontainers.postgresql.image=postgres:17
testcontainers.postgresql.database=hrdb_test
testcontainers.postgresql.username=test_user
testcontainers.postgresql.password=test_password
testcontainers.reuse.enable=true

# HikariCP Configuration - Minimal for testing
hikari.maximumPoolSize=2
hikari.minimumIdle=1
hikari.connectionTimeout=5000
hikari.idleTimeout=60000
hikari.maxLifetime=300000
hikari.registerMbeans=false

# Flyway Configuration
flyway.url=${db.url}
flyway.user=${db.username}
flyway.password=${db.password}
flyway.locations=classpath:db/migration,classpath:db/test-data
flyway.cleanDisabled=false
flyway.baselineOnMigrate=true

# Logging Configuration
logging.level.root=WARN
logging.level.com.hrapp.jdbc.samples=INFO
logging.level.org.testcontainers=INFO
logging.level.com.zaxxer.hikari=WARN
logging.pattern.console=%d{HH:mm:ss} %-5level %logger{36} - %msg%n

# Test Features
app.debug.enabled=true
app.cache.enabled=false
app.metrics.enabled=false
app.health.detailed=false

# Test-specific settings
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
```

### Staging Profile

**File**: `src/main/resources/application-staging.properties`

```properties
# Staging Environment Configuration
app.profile=staging
app.name=HR Web Application (Staging)
app.version=1.0.0-RC

# Database Configuration
db.url=jdbc:postgresql://staging-db-server:5432/hrdb_staging
db.username=hr_staging_user
db.password=${DB_PASSWORD}
db.driver=org.postgresql.Driver
db.schema=public

# SSL Configuration for staging
db.ssl.enabled=true
db.ssl.mode=require
db.ssl.factory=org.postgresql.ssl.DefaultJavaSSLFactory

# HikariCP Configuration - Production-like
hikari.maximumPoolSize=10
hikari.minimumIdle=3
hikari.connectionTimeout=20000
hikari.idleTimeout=600000
hikari.maxLifetime=1800000
hikari.leakDetectionThreshold=60000
hikari.registerMbeans=true

# Performance settings
hikari.cachePrepStmts=true
hikari.prepStmtCacheSize=250
hikari.prepStmtCacheSqlLimit=2048
hikari.useServerPrepStmts=true

# Flyway Configuration
flyway.url=${db.url}
flyway.user=${db.username}
flyway.password=${db.password}
flyway.locations=classpath:db/migration
flyway.baselineOnMigrate=true
flyway.cleanDisabled=true
flyway.validateOnMigrate=true
flyway.outOfOrder=false

# Logging Configuration
logging.level.root=INFO
logging.level.com.hrapp.jdbc.samples=INFO
logging.level.com.zaxxer.hikari=INFO
logging.level.org.flywaydb=INFO
logging.file.name=logs/hrapp-staging.log
logging.file.max-size=10MB
logging.file.max-history=30
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# Staging Features
app.debug.enabled=false
app.cache.enabled=true
app.cache.ttl=1800
app.metrics.enabled=true
app.health.detailed=true

# Staging-specific settings
server.port=8080
server.servlet.context-path=/HRWebApp
```

### Production Profile

**File**: `src/main/resources/application-prod.properties`

```properties
# Production Environment Configuration
app.profile=prod
app.name=HR Web Application
app.version=1.0.0

# Database Configuration
db.url=jdbc:postgresql://prod-db-cluster:5432/hrdb_prod
db.username=hr_prod_user
db.password=${DB_PASSWORD}
db.driver=org.postgresql.Driver
db.schema=public

# SSL Configuration for production
db.ssl.enabled=true
db.ssl.mode=require
db.ssl.cert=${SSL_CERT_PATH}
db.ssl.key=${SSL_KEY_PATH}
db.ssl.rootcert=${SSL_ROOT_CERT_PATH}
db.ssl.factory=org.postgresql.ssl.DefaultJavaSSLFactory

# HikariCP Configuration - Production optimized
hikari.maximumPoolSize=20
hikari.minimumIdle=5
hikari.connectionTimeout=20000
hikari.idleTimeout=600000
hikari.maxLifetime=1800000
hikari.leakDetectionThreshold=30000
hikari.registerMbeans=true

# Advanced HikariCP settings for production
hikari.cachePrepStmts=true
hikari.prepStmtCacheSize=250
hikari.prepStmtCacheSqlLimit=2048
hikari.useServerPrepStmts=true
hikari.useLocalSessionState=true
hikari.rewriteBatchedStatements=true
hikari.cacheResultSetMetadata=true
hikari.cacheServerConfiguration=true
hikari.elideSetAutoCommits=true
hikari.maintainTimeStats=false

# Flyway Configuration
flyway.url=${db.url}
flyway.user=${db.username}
flyway.password=${db.password}
flyway.locations=classpath:db/migration
flyway.baselineOnMigrate=false
flyway.cleanDisabled=true
flyway.validateOnMigrate=true
flyway.outOfOrder=false
flyway.ignoreMissingMigrations=false

# Logging Configuration
logging.level.root=WARN
logging.level.com.hrapp.jdbc.samples=INFO
logging.level.com.zaxxer.hikari=INFO
logging.level.org.flywaydb=INFO
logging.file.name=logs/hrapp-prod.log
logging.file.max-size=50MB
logging.file.max-history=90
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# Production Features
app.debug.enabled=false
app.cache.enabled=true
app.cache.ttl=3600
app.metrics.enabled=true
app.health.detailed=false
app.security.enhanced=true

# Production-specific settings
server.port=8080
server.servlet.context-path=/HRWebApp
server.compression.enabled=true
server.compression.mime-types=text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json
```

## Database Configuration

### PostgreSQL Connection Parameters

#### Basic Connection Configuration
```properties
# Standard connection parameters
db.url=jdbc:postgresql://hostname:port/database
db.username=username
db.password=password
db.driver=org.postgresql.Driver
db.schema=public

# Connection timeout settings
db.loginTimeout=10
db.connectTimeout=10000
db.socketTimeout=30000
```

#### Advanced PostgreSQL JDBC Parameters
```properties
# Performance optimization parameters
db.url=jdbc:postgresql://localhost:5432/hrdb?\
    prepareThreshold=3&\
    preparedStatementCacheQueries=256&\
    preparedStatementCacheSizeMiB=5&\
    defaultRowFetchSize=1000&\
    reWriteBatchedInserts=true&\
    ApplicationName=HRWebApp&\
    tcpKeepAlive=true&\
    assumeMinServerVersion=12.0&\
    logUnclosedConnections=true&\
    binaryTransfer=true&\
    stringtype=unspecified
```

#### SSL Configuration
```properties
# SSL/TLS configuration for secure connections
db.url=jdbc:postgresql://hostname:5432/database?\
    ssl=true&\
    sslmode=require&\
    sslcert=/path/to/client.crt&\
    sslkey=/path/to/client.key&\
    sslrootcert=/path/to/ca.crt&\
    sslpassword=${SSL_PASSWORD}
```

### Environment-Specific Database Configuration

#### Development Environment
```properties
# Local development database
db.url=jdbc:postgresql://localhost:5432/hrdb_dev
db.username=hr_dev_user
db.password=dev_password

# Relaxed settings for development
db.ssl.enabled=false
db.autoCommit=true
db.readOnly=false
```

#### Production Environment
```properties
# Production database cluster
db.url=jdbc:postgresql://prod-primary:5432,prod-secondary:5432/hrdb_prod
db.username=hr_prod_user
db.password=${DB_PASSWORD}

# Strict settings for production
db.ssl.enabled=true
db.ssl.mode=require
db.autoCommit=false
db.readOnly=false
db.targetServerType=primary
```

## Connection Pool Configuration

### HikariCP Tuning Parameters

#### Core Pool Settings
```properties
# Pool size configuration
hikari.maximumPoolSize=20              # Maximum number of connections
hikari.minimumIdle=5                   # Minimum idle connections
hikari.connectionTimeout=20000         # Max wait time for connection (ms)
hikari.idleTimeout=600000             # Max idle time before closing (ms)
hikari.maxLifetime=1800000            # Max connection lifetime (ms)
hikari.keepaliveTime=300000           # Keepalive interval (ms)
```

#### Performance Settings
```properties
# Statement caching
hikari.cachePrepStmts=true
hikari.prepStmtCacheSize=250
hikari.prepStmtCacheSqlLimit=2048

# PostgreSQL optimizations
hikari.useServerPrepStmts=true
hikari.useLocalSessionState=true
hikari.rewriteBatchedStatements=true
hikari.cacheResultSetMetadata=true
hikari.cacheServerConfiguration=true
hikari.elideSetAutoCommits=true
hikari.maintainTimeStats=false
```

#### Monitoring and Health
```properties
# Health check configuration
hikari.connectionTestQuery=SELECT 1
hikari.validationTimeout=5000
hikari.leakDetectionThreshold=60000
hikari.registerMbeans=true
hikari.allowPoolSuspension=false
```

### Environment-Specific Pool Configurations

#### Development Environment
```properties
# Small pool for development
hikari.maximumPoolSize=5
hikari.minimumIdle=1
hikari.connectionTimeout=10000
hikari.idleTimeout=300000
hikari.maxLifetime=900000
hikari.leakDetectionThreshold=0
```

#### High-Concurrency Production Environment
```properties
# Large pool for high load
hikari.maximumPoolSize=50
hikari.minimumIdle=10
hikari.connectionTimeout=30000
hikari.idleTimeout=300000
hikari.maxLifetime=1200000
hikari.leakDetectionThreshold=30000
```

#### Resource-Constrained Environment
```properties
# Minimal pool for limited resources
hikari.maximumPoolSize=8
hikari.minimumIdle=2
hikari.connectionTimeout=15000
hikari.idleTimeout=900000
hikari.maxLifetime=3600000
```

## Security Configuration

### Authentication Configuration

#### Tomcat Security Realm
```xml
<!-- server.xml - Database realm configuration -->
<Realm className="org.apache.catalina.realm.DataSourceRealm"
       dataSourceName="jdbc/HRDataSource"
       userTable="users"
       userNameCol="username"
       userCredCol="password"
       userRoleTable="user_roles"
       roleNameCol="role_name"
       digest="SHA-256" />
```

#### Form-Based Authentication
```xml
<!-- web.xml - Security configuration -->
<security-constraint>
    <web-resource-collection>
        <web-resource-name>Protected Area</web-resource-name>
        <url-pattern>/protected/*</url-pattern>
        <http-method>GET</http-method>
        <http-method>POST</http-method>
    </web-resource-collection>
    <auth-constraint>
        <role-name>manager</role-name>
        <role-name>staff</role-name>
    </auth-constraint>
    <user-data-constraint>
        <transport-guarantee>CONFIDENTIAL</transport-guarantee>
    </user-data-constraint>
</security-constraint>

<login-config>
    <auth-method>FORM</auth-method>
    <realm-name>HR Application</realm-name>
    <form-login-config>
        <form-login-page>/login.html</form-login-page>
        <form-error-page>/login-failed.html</form-error-page>
    </form-login-config>
</login-config>

<security-role>
    <role-name>manager</role-name>
</security-role>
<security-role>
    <role-name>staff</role-name>
</security-role>
```

### SSL/TLS Configuration

#### Tomcat SSL Connector
```xml
<!-- server.xml - HTTPS connector -->
<Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol"
           maxThreads="150" SSLEnabled="true">
    <SSLHostConfig>
        <Certificate certificateKeystoreFile="conf/keystore.jks"
                     certificateKeystorePassword="${KEYSTORE_PASSWORD}"
                     certificateKeyAlias="tomcat"
                     type="RSA" />
    </SSLHostConfig>
</Connector>

<!-- Redirect HTTP to HTTPS -->
<Connector port="8080" protocol="HTTP/1.1"
           connectionTimeout="20000"
           redirectPort="8443" />
```

#### Security Headers Configuration
```xml
<!-- web.xml - Security headers filter -->
<filter>
    <filter-name>SecurityHeadersFilter</filter-name>
    <filter-class>com.hrapp.jdbc.samples.web.SecurityHeadersFilter</filter-class>
    <init-param>
        <param-name>X-Frame-Options</param-name>
        <param-value>DENY</param-value>
    </init-param>
    <init-param>
        <param-name>X-Content-Type-Options</param-name>
        <param-value>nosniff</param-value>
    </init-param>
    <init-param>
        <param-name>X-XSS-Protection</param-name>
        <param-value>1; mode=block</param-value>
    </init-param>
    <init-param>
        <param-name>Strict-Transport-Security</param-name>
        <param-value>max-age=31536000; includeSubDomains</param-value>
    </init-param>
    <init-param>
        <param-name>Content-Security-Policy</param-name>
        <param-value>default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'</param-value>
    </init-param>
</filter>

<filter-mapping>
    <filter-name>SecurityHeadersFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

## Logging Configuration

### Logback Configuration

#### Development Logging
```xml
<!-- logback-spring.xml for development -->
<configuration>
    <springProfile name="dev">
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/hrapp-dev.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/hrapp-dev.%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>7</maxHistory>
            </rollingPolicy>
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        
        <logger name="com.hrapp.jdbc.samples" level="DEBUG"/>
        <logger name="com.zaxxer.hikari" level="DEBUG"/>
        <logger name="org.flywaydb" level="INFO"/>
        <logger name="org.postgresql" level="INFO"/>
        
        <root level="INFO">
            <appender-ref ref="STDOUT"/>
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
</configuration>
```

#### Production Logging
```xml
<!-- logback-spring.xml for production -->
<configuration>
    <springProfile name="prod">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/hrapp.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/hrapp.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
                <maxFileSize>50MB</maxFileSize>
                <maxHistory>90</maxHistory>
                <totalSizeCap>5GB</totalSizeCap>
            </rollingPolicy>
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        
        <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/hrapp-error.log</file>
            <filter class="ch.qos.logback.classic.filter.LevelFilter">
                <level>ERROR</level>
                <onMatch>ACCEPT</onMatch>
                <onMismatch>DENY</onMismatch>
            </filter>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/hrapp-error.%d{yyyy-MM-dd}.gz</fileNamePattern>
                <maxHistory>90</maxHistory>
            </rollingPolicy>
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        
        <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
            <appender-ref ref="FILE"/>
            <queueSize>512</queueSize>
            <discardingThreshold>0</discardingThreshold>
        </appender>
        
        <logger name="com.hrapp.jdbc.samples" level="INFO"/>
        <logger name="com.zaxxer.hikari" level="WARN"/>
        <logger name="org.flywaydb" level="INFO"/>
        <logger name="org.postgresql" level="WARN"/>
        <logger name="org.apache.tomcat" level="WARN"/>
        
        <root level="WARN">
            <appender-ref ref="ASYNC_FILE"/>
            <appender-ref ref="ERROR_FILE"/>
        </root>
    </springProfile>
</configuration>
```

### Log Levels by Component

```properties
# Application logging
logging.level.com.hrapp.jdbc.samples.bean=INFO
logging.level.com.hrapp.jdbc.samples.web=INFO
logging.level.com.hrapp.jdbc.samples.entity=WARN
logging.level.com.hrapp.jdbc.samples.config=INFO

# Third-party logging
logging.level.com.zaxxer.hikari=INFO
logging.level.org.flywaydb=INFO
logging.level.org.postgresql=WARN
logging.level.org.apache.tomcat=WARN

# Security logging
logging.level.org.apache.catalina.realm=INFO
logging.level.org.apache.catalina.authenticator=INFO

# Performance logging
logging.level.org.apache.catalina.valves.AccessLogValve=INFO
```

## Performance Configuration

### JVM Configuration by Environment

#### Development JVM Settings
```bash
# Development - Fast startup, debugging enabled
export JAVA_OPTS="-server \
    -Xms512m -Xmx2g \
    -XX:+UseParallelGC \
    -XX:TieredStopAtLevel=1 \
    -Djava.awt.headless=true \
    -Dspring.profiles.active=dev \
    -Ddebug=true \
    -Dfile.encoding=UTF-8"
```

#### Production JVM Settings
```bash
# Production - Optimized for performance
export JAVA_OPTS="-server \
    -Xms2g -Xmx8g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:G1HeapRegionSize=16m \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:+UseCompressedOops \
    -XX:ReservedCodeCacheSize=256m \
    -Djava.awt.headless=true \
    -Dspring.profiles.active=prod \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC"
```

### Tomcat Performance Configuration

#### Production Tomcat Settings
```xml
<!-- server.xml - Production connector -->
<Connector port="8080" protocol="HTTP/1.1"
           connectionTimeout="20000"
           redirectPort="8443"
           maxThreads="200"
           minSpareThreads="25"
           maxSpareThreads="75"
           acceptCount="100"
           enableLookups="false"
           compression="on"
           compressionMinSize="2048"
           compressableMimeType="text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json"
           URIEncoding="UTF-8"
           maxHttpHeaderSize="8192"
           maxPostSize="2097152"
           processorCache="200"
           tcpNoDelay="true" />

<!-- Thread pool executor -->
<Executor name="tomcatThreadPool" 
          namePrefix="catalina-exec-"
          maxThreads="200"
          minSpareThreads="25"
          maxIdleTime="60000"
          prestartminSpareThreads="true" />
```

## Environment Variables

### Database Environment Variables
```bash
# Database connection
export DB_URL="jdbc:postgresql://localhost:5432/hrdb"
export DB_USERNAME="hr_user"
export DB_PASSWORD="secure_password"
export DB_DRIVER="org.postgresql.Driver"
export DB_SCHEMA="public"

# SSL configuration
export DB_SSL_ENABLED="true"
export DB_SSL_MODE="require"
export SSL_CERT_PATH="/path/to/client.crt"
export SSL_KEY_PATH="/path/to/client.key"
export SSL_ROOT_CERT_PATH="/path/to/ca.crt"
export SSL_PASSWORD="ssl_password"
```

### HikariCP Environment Variables
```bash
# Connection pool settings
export HIKARI_MAX_POOL_SIZE="20"
export HIKARI_MIN_IDLE="5"
export HIKARI_CONNECTION_TIMEOUT="20000"
export HIKARI_IDLE_TIMEOUT="600000"
export HIKARI_MAX_LIFETIME="1800000"
export HIKARI_LEAK_DETECTION_THRESHOLD="30000"

# Performance settings
export HIKARI_CACHE_PREP_STMTS="true"
export HIKARI_PREP_STMT_CACHE_SIZE="250"
export HIKARI_PREP_STMT_CACHE_SQL_LIMIT="2048"
```

### Application Environment Variables
```bash
# Application settings
export APP_PROFILE="prod"
export APP_NAME="HR Web Application"
export APP_VERSION="1.0.0"
export APP_DEBUG_ENABLED="false"
export APP_CACHE_ENABLED="true"
export APP_METRICS_ENABLED="true"

# Logging settings
export LOG_LEVEL="INFO"
export LOG_FILE_PATH="/var/log/hrapp"
export LOG_MAX_FILE_SIZE="50MB"
export LOG_MAX_HISTORY="90"

# Security settings
export KEYSTORE_PASSWORD="keystore_password"
export TRUSTSTORE_PASSWORD="truststore_password"
```

### System Environment Variables
```bash
# Java settings
export JAVA_HOME="/opt/openjdk-21"
export PATH="$JAVA_HOME/bin:$PATH"

# Tomcat settings
export CATALINA_HOME="/opt/tomcat"
export CATALINA_BASE="/opt/tomcat"
export CATALINA_OPTS="-Xms2g -Xmx8g -XX:+UseG1GC"

# PostgreSQL settings
export PGHOST="localhost"
export PGPORT="5432"
export PGDATABASE="hrdb"
export PGUSER="hr_user"
export PGPASSWORD="hr_password"
```

## Docker Configuration

### Dockerfile
```dockerfile
FROM openjdk:21-jre-slim

# Create application user
RUN groupadd -r hrapp && useradd -r -g hrapp hrapp

# Install PostgreSQL client tools
RUN apt-get update && apt-get install -y postgresql-client && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy application
COPY target/HRWebApp.war app.war
COPY docker-entrypoint.sh /usr/local/bin/
COPY wait-for-postgres.sh /usr/local/bin/

# Set permissions
RUN chmod +x /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/wait-for-postgres.sh
RUN chown -R hrapp:hrapp /app

# Create logs directory
RUN mkdir -p /app/logs && chown hrapp:hrapp /app/logs

# Switch to application user
USER hrapp

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/HRWebApp/health || exit 1

# Entry point
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["java", "-jar", "app.war"]
```

### Docker Compose Configuration
```yaml
version: '3.8'

services:
  hrapp:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_URL=jdbc:postgresql://postgres:5432/hrdb
      - DB_USERNAME=hr_user
      - DB_PASSWORD=hr_password
      - HIKARI_MAX_POOL_SIZE=10
      - JAVA_OPTS=-Xms1g -Xmx2g -XX:+UseG1GC
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - hrapp-network
    volumes:
      - ./logs:/app/logs
      - ./config:/app/config
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/HRWebApp/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  postgres:
    image: postgres:17
    environment:
      - POSTGRES_DB=hrdb
      - POSTGRES_USER=hr_user
      - POSTGRES_PASSWORD=hr_password
      - POSTGRES_INITDB_ARGS=--encoding=UTF-8 --lc-collate=C --lc-ctype=C
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
      - ./postgresql.conf:/etc/postgresql/postgresql.conf
    networks:
      - hrapp-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U hr_user -d hrdb"]
      interval: 10s
      timeout: 5s
      retries: 5
    command: postgres -c config_file=/etc/postgresql/postgresql.conf

volumes:
  postgres_data:
    driver: local

networks:
  hrapp-network:
    driver: bridge
```

### Docker Environment Configuration
```properties
# application-docker.properties
app.profile=docker
app.name=HR Web Application (Docker)

# Database configuration for Docker
db.url=jdbc:postgresql://postgres:5432/hrdb
db.username=hr_user
db.password=hr_password

# HikariCP configuration for Docker
hikari.maximumPoolSize=10
hikari.minimumIdle=2
hikari.connectionTimeout=30000

# Logging configuration for Docker
logging.level.root=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

## Cloud Deployment Configuration

### AWS Configuration

#### Application Properties for AWS
```properties
# AWS RDS configuration
db.url=jdbc:postgresql://hrdb-cluster.cluster-xyz.us-east-1.rds.amazonaws.com:5432/hrdb
db.username=${RDS_USERNAME}
db.password=${RDS_PASSWORD}

# AWS-specific settings
aws.region=us-east-1
aws.rds.ssl.enabled=true
aws.cloudwatch.metrics.enabled=true
aws.s3.bucket.logs=hrapp-logs-bucket
```

#### AWS Environment Variables
```bash
# AWS credentials (use IAM roles in production)
export AWS_ACCESS_KEY_ID="your-access-key"
export AWS_SECRET_ACCESS_KEY="your-secret-key"
export AWS_DEFAULT_REGION="us-east-1"

# RDS connection
export RDS_ENDPOINT="hrdb-cluster.cluster-xyz.us-east-1.rds.amazonaws.com"
export RDS_USERNAME="hr_user"
export RDS_PASSWORD="secure_password"

# CloudWatch
export CLOUDWATCH_NAMESPACE="HRApp/Production"
export CLOUDWATCH_LOG_GROUP="/aws/hrapp/application"
```

### Kubernetes Configuration

#### Deployment YAML
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: hrapp-deployment
  labels:
    app: hrapp
spec:
  replicas: 3
  selector:
    matchLabels:
      app: hrapp
  template:
    metadata:
      labels:
        app: hrapp
    spec:
      containers:
      - name: hrapp
        image: hrapp:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: DB_URL
          valueFrom:
            secretKeyRef:
              name: hrapp-secrets
              key: db-url
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: hrapp-secrets
              key: db-username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: hrapp-secrets
              key: db-password
        - name: JAVA_OPTS
          value: "-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1"
        livenessProbe:
          httpGet:
            path: /HRWebApp/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /HRWebApp/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
        - name: logs-volume
          mountPath: /app/logs
      volumes:
      - name: config-volume
        configMap:
          name: hrapp-config
      - name: logs-volume
        emptyDir: {}
      imagePullSecrets:
      - name: registry-secret
---
apiVersion: v1
kind: Service
metadata:
  name: hrapp-service
spec:
  selector:
    app: hrapp
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
```

#### ConfigMap for Application Properties
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: hrapp-config
data:
  application-k8s.properties: |
    # Kubernetes-specific configuration
    app.profile=k8s
    app.name=HR Web Application (Kubernetes)
    
    # Database configuration
    db.url=${DB_URL}
    db.username=${DB_USERNAME}
    db.password=${DB_PASSWORD}
    
    # HikariCP configuration
    hikari.maximumPoolSize=15
    hikari.minimumIdle=3
    hikari.connectionTimeout=20000
    hikari.registerMbeans=true
    
    # Logging configuration
    logging.level.root=INFO
    logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
    
    # Kubernetes-specific settings
    app.metrics.enabled=true
    app.health.detailed=true
```

#### Secrets Configuration
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: hrapp-secrets
type: Opaque
data:
  db-url: amRiYzpwb3N0Z3Jlc3FsOi8vcG9zdGdyZXM6NTQzMi9ocmRi  # base64 encoded
  db-username: aHJfdXNlcg==  # base64 encoded
  db-password: aHJfcGFzc3dvcmQ=  # base64 encoded
```

This comprehensive configuration guide provides all the necessary information to deploy and configure the HR Web Application across different environments while maintaining security, performance, and reliability standards.