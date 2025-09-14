# Build and Deployment Guide

## Prerequisites

### Required Software
1. **OpenJDK 21** (or later LTS version)
   - Download from: https://adoptium.net/temurin/releases/
   - Set JAVA_HOME environment variable
   - Add %JAVA_HOME%\bin to PATH

2. **Apache Maven 3.9+** (optional - Maven wrapper included)
   - Download from: https://maven.apache.org/download.cgi
   - Or use included Maven wrapper (mvnw.cmd)

3. **PostgreSQL 17+**
   - Download from: https://www.postgresql.org/download/
   - Create database: `hrdb`
   - Create user: `hr_user` with password `hr_password`

### Environment Setup
```cmd
# Set JAVA_HOME (example path - adjust for your installation)
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.4.7-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

# Verify Java installation
java -version
```

## Build Process

### Option 1: Using Maven Wrapper (Recommended)
```cmd
# Clean and build WAR file
mvnw.cmd clean package

# Skip tests for faster build
mvnw.cmd clean package -DskipTests

# Run only unit tests
mvnw.cmd test

# Run integration tests
mvnw.cmd verify
```

### Option 2: Using System Maven
```cmd
# Clean and build WAR file
mvn clean package

# Skip tests for faster build
mvn clean package -DskipTests

# Run only unit tests
mvn test

# Run integration tests
mvn verify
```

## Database Setup

### 1. Create PostgreSQL Database
```sql
-- Connect as postgres superuser
CREATE DATABASE hrdb;
CREATE USER hr_user WITH PASSWORD 'hr_password';
GRANT ALL PRIVILEGES ON DATABASE hrdb TO hr_user;

-- Connect to hrdb as hr_user
CREATE SCHEMA hr;
GRANT ALL ON SCHEMA hr TO hr_user;
```

### 2. Run Database Migrations
```cmd
# Using Maven wrapper
mvnw.cmd flyway:migrate

# Using system Maven
mvn flyway:migrate
```

## Build Outputs

### WAR File Location
- **File**: `target/HRWebApp.war`
- **Size**: Approximately 15-20 MB (includes all dependencies)
- **Contents**: 
  - Compiled Java classes
  - Web resources (HTML, CSS, JS)
  - Configuration files
  - Runtime dependencies (PostgreSQL JDBC, HikariCP, etc.)

### Dependency Verification
The WAR file includes these key runtime dependencies:
- PostgreSQL JDBC Driver (42.7.4)
- HikariCP Connection Pool (6.3.0)
- Flyway Core (10.21.0)
- Google Gson (2.11.0)
- Jakarta Servlet API (provided by Tomcat)

## Deployment to Tomcat

### Prerequisites
- Apache Tomcat 10.1+ (Jakarta EE 9+ compatible)
- Java 21+ runtime environment

### Deployment Steps

1. **Stop Tomcat** (if running)
   ```cmd
   %CATALINA_HOME%\bin\shutdown.bat
   ```

2. **Deploy WAR file**
   ```cmd
   copy target\HRWebApp.war %CATALINA_HOME%\webapps\
   ```

3. **Configure Authentication**
   ```cmd
   copy tomcat-users.xml %CATALINA_HOME%\conf\
   ```

4. **Start Tomcat**
   ```cmd
   %CATALINA_HOME%\bin\startup.bat
   ```

5. **Verify Deployment**
   - Check logs: `%CATALINA_HOME%\logs\catalina.out`
   - Access application: http://localhost:8080/HRWebApp
   - Health check: http://localhost:8080/HRWebApp/health

### Deployment Verification

#### Application Startup Checks
1. **WAR Extraction**: Verify `%CATALINA_HOME%\webapps\HRWebApp\` directory exists
2. **Database Connection**: Check logs for successful PostgreSQL connection
3. **HikariCP Pool**: Verify connection pool initialization
4. **Servlet Registration**: Confirm all servlets are registered

#### Functional Testing
1. **Login Page**: http://localhost:8080/HRWebApp/login.html
2. **Authentication**: Test with admin/admin123 credentials
3. **Employee Listing**: Verify data loads from PostgreSQL
4. **CRUD Operations**: Test create, update, delete functionality

## Troubleshooting

### Common Build Issues

#### Java Version Mismatch
```
Error: Unsupported class file major version
Solution: Ensure JAVA_HOME points to JDK 21+
```

#### Maven Wrapper Download Issues
```
Error: Could not download maven-wrapper.jar
Solution: Check internet connection or use system Maven
```

#### PostgreSQL Connection Issues
```
Error: Connection refused
Solution: 
1. Verify PostgreSQL is running
2. Check database credentials in pom.xml
3. Ensure database 'hrdb' exists
```

### Common Deployment Issues

#### Tomcat Version Compatibility
```
Error: ClassNotFoundException for Jakarta servlet classes
Solution: Use Tomcat 10.1+ (Jakarta EE 9+ compatible)
```

#### Database Connection Pool Issues
```
Error: HikariPool connection timeout
Solution:
1. Verify PostgreSQL is accessible from Tomcat server
2. Check firewall settings
3. Validate database credentials
```

#### Authentication Configuration
```
Error: 403 Forbidden on protected resources
Solution:
1. Ensure tomcat-users.xml is properly configured
2. Verify role mappings in web.xml
3. Check security constraints
```

## Performance Tuning

### JVM Options for Production
```cmd
set CATALINA_OPTS=-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### HikariCP Tuning
Adjust connection pool settings in DatabaseConfig.java:
- `maximumPoolSize`: Based on expected concurrent users
- `connectionTimeout`: Network latency considerations
- `idleTimeout`: Balance between performance and resource usage

### PostgreSQL Tuning
- Ensure appropriate indexes on frequently queried columns
- Configure `shared_buffers` and `work_mem` based on available RAM
- Monitor query performance with `pg_stat_statements`

## Monitoring and Maintenance

### Log Files
- **Application Logs**: `%CATALINA_HOME%\logs\catalina.out`
- **Access Logs**: `%CATALINA_HOME%\logs\localhost_access_log.txt`
- **HikariCP Metrics**: Available via JMX

### Health Monitoring
- **Health Check Endpoint**: `/HRWebApp/health`
- **Database Connectivity**: Automatic health checks via HikariCP
- **Memory Usage**: Monitor JVM heap and connection pool metrics

### Backup Strategy
- **Database**: Regular PostgreSQL backups using `pg_dump`
- **Application**: Version control for WAR files and configuration
- **Configuration**: Backup Tomcat configuration files