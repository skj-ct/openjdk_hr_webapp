# Troubleshooting Guide - Oracle to OpenJDK Migration

This guide provides comprehensive troubleshooting solutions for common issues encountered during and after the migration from Oracle JDK + Oracle Database to OpenJDK 21 + PostgreSQL.

## Table of Contents

- [Quick Diagnostic Commands](#quick-diagnostic-commands)
- [Migration-Specific Issues](#migration-specific-issues)
- [Database Connection Issues](#database-connection-issues)
- [Build and Deployment Issues](#build-and-deployment-issues)
- [Runtime Issues](#runtime-issues)
- [Performance Issues](#performance-issues)
- [Authentication and Security Issues](#authentication-and-security-issues)
- [Environment-Specific Issues](#environment-specific-issues)
- [Data Migration Issues](#data-migration-issues)
- [Advanced Troubleshooting](#advanced-troubleshooting)

## Quick Diagnostic Commands

### System Health Check Script

Create a diagnostic script to quickly assess system health:

**diagnose-system.cmd** (Windows)
```cmd
@echo off
echo ========================================
echo HR Web Application - System Diagnostics
echo ========================================

echo.
echo === Java Version ===
java -version 2>&1

echo.
echo === Maven Version ===
mvn -version 2>&1

echo.
echo === PostgreSQL Status ===
pg_isready -h localhost -p 5432 2>&1
if %errorlevel% equ 0 (
    echo PostgreSQL is running
) else (
    echo PostgreSQL is not responding
)

echo.
echo === Database Connection Test ===
psql -U hr_user -d hrdb -h localhost -c "SELECT version();" 2>&1

echo.
echo === Tomcat Status ===
netstat -an | findstr :8080
if %errorlevel% equ 0 (
    echo Tomcat is listening on port 8080
) else (
    echo Tomcat is not running on port 8080
)

echo.
echo === Application Health Check ===
curl -f http://localhost:8080/HRWebApp/health 2>&1
if %errorlevel% equ 0 (
    echo Application is responding
) else (
    echo Application is not responding
)

echo.
echo === Disk Space ===
dir C:\ | findstr "bytes free"

echo.
echo === Memory Usage ===
wmic OS get TotalVisibleMemorySize,FreePhysicalMemory /format:list

echo.
echo Diagnostics completed.
pause
```

**diagnose-system.sh** (Linux/macOS)
```bash
#!/bin/bash
echo "========================================"
echo "HR Web Application - System Diagnostics"
echo "========================================"

echo
echo "=== Java Version ==="
java -version

echo
echo "=== Maven Version ==="
mvn -version

echo
echo "=== PostgreSQL Status ==="
if pg_isready -h localhost -p 5432; then
    echo "PostgreSQL is running"
else
    echo "PostgreSQL is not responding"
fi

echo
echo "=== Database Connection Test ==="
psql -U hr_user -d hrdb -h localhost -c "SELECT version();"

echo
echo "=== Tomcat Status ==="
if netstat -an | grep :8080; then
    echo "Tomcat is listening on port 8080"
else
    echo "Tomcat is not running on port 8080"
fi

echo
echo "=== Application Health Check ==="
if curl -f http://localhost:8080/HRWebApp/health; then
    echo "Application is responding"
else
    echo "Application is not responding"
fi

echo
echo "=== System Resources ==="
free -h
df -h

echo
echo "Diagnostics completed."
```

### Log Collection Script

**collect-logs.cmd** (Windows)
```cmd
@echo off
set TIMESTAMP=%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set LOG_DIR=diagnostic_logs_%TIMESTAMP%

echo Creating diagnostic log collection: %LOG_DIR%
mkdir %LOG_DIR%

echo Collecting system information...
systeminfo > %LOG_DIR%\system_info.txt
java -version > %LOG_DIR%\java_version.txt 2>&1
mvn -version > %LOG_DIR%\maven_version.txt 2>&1

echo Collecting application logs...
if exist logs\*.log copy logs\*.log %LOG_DIR%\
if exist target\surefire-reports\*.* copy target\surefire-reports\*.* %LOG_DIR%\

echo Collecting Tomcat logs...
if exist "%CATALINA_HOME%\logs\*" copy "%CATALINA_HOME%\logs\*" %LOG_DIR%\

echo Collecting PostgreSQL logs...
if exist "C:\Program Files\PostgreSQL\17\data\log\*" copy "C:\Program Files\PostgreSQL\17\data\log\*" %LOG_DIR%\

echo Collecting configuration files...
copy pom.xml %LOG_DIR%\ 2>nul
copy src\main\resources\application*.properties %LOG_DIR%\ 2>nul
copy tomcat-users.xml %LOG_DIR%\ 2>nul

echo Log collection completed in: %LOG_DIR%
```

## Migration-Specific Issues

### Oracle to PostgreSQL Data Type Issues

#### Issue: Numeric Precision Errors
```
ERROR: numeric field overflow
DETAIL: A field with precision 8, scale 2 must round to an absolute value less than 10^6
```

**Root Cause**: Oracle NUMBER type handling differs from PostgreSQL NUMERIC

**Solutions**:

1. **Update Data Type Definitions**
   ```sql
   -- Change from Oracle NUMBER to PostgreSQL NUMERIC with appropriate precision
   ALTER TABLE employees ALTER COLUMN salary TYPE NUMERIC(10,2);
   ```

2. **Validate Data Before Migration**
   ```sql
   -- Check for values that exceed PostgreSQL limits
   SELECT employee_id, salary 
   FROM employees 
   WHERE salary >= 1000000 OR salary <= -1000000;
   ```

3. **Update Application Code**
   ```java
   // Change from int to BigDecimal for salary handling
   // Old Oracle code:
   // int salary = rs.getInt("salary");
   
   // New PostgreSQL code:
   BigDecimal salary = rs.getBigDecimal("salary");
   ```

#### Issue: Date/Time Format Differences
```
ERROR: date/time field value out of range
```

**Root Cause**: Oracle DATE includes time, PostgreSQL DATE is date-only

**Solutions**:

1. **Use TIMESTAMP for Date+Time Data**
   ```sql
   -- Migration script
   ALTER TABLE employees ADD COLUMN hire_date_new TIMESTAMP;
   UPDATE employees SET hire_date_new = hire_date::TIMESTAMP;
   ALTER TABLE employees DROP COLUMN hire_date;
   ALTER TABLE employees RENAME COLUMN hire_date_new TO hire_date;
   ```

2. **Update Java Code**
   ```java
   // Use appropriate Java types
   Timestamp hireDate = rs.getTimestamp("hire_date");
   LocalDateTime localDateTime = hireDate.toLocalDateTime();
   ```

### Stored Procedure Migration Issues

#### Issue: Oracle PL/SQL Syntax Not Supported
```
ERROR: syntax error at or near "PACKAGE"
```

**Root Cause**: PostgreSQL doesn't support Oracle PL/SQL packages

**Solutions**:

1. **Convert Package to Individual Functions**
   ```sql
   -- Oracle PL/SQL Package (Original)
   CREATE OR REPLACE PACKAGE refcur_pkg AS
     TYPE refcur_t IS REF CURSOR;
     PROCEDURE incrementsalary(increment_pct IN NUMBER, emp_refcur OUT refcur_t);
   END refcur_pkg;
   
   -- PostgreSQL Function (Converted)
   CREATE OR REPLACE FUNCTION increment_salary_function(increment_pct INTEGER)
   RETURNS TABLE(employee_id INTEGER, first_name VARCHAR, last_name VARCHAR, 
                 email VARCHAR, phone_number VARCHAR, job_id VARCHAR, salary NUMERIC)
   AS $$
   BEGIN
       UPDATE employees SET salary = salary * (1 + increment_pct / 100.0);
       RETURN QUERY SELECT * FROM employees ORDER BY employee_id;
   END;
   $$ LANGUAGE plpgsql;
   ```

2. **Update Java Application Code**
   ```java
   // Old Oracle code:
   CallableStatement cs = conn.prepareCall("{call refcur_pkg.incrementsalary(?, ?)}");
   cs.setInt(1, incrementPct);
   cs.registerOutParameter(2, OracleTypes.CURSOR);
   cs.execute();
   ResultSet rs = (ResultSet) cs.getObject(2);
   
   // New PostgreSQL code:
   PreparedStatement ps = conn.prepareStatement("SELECT * FROM increment_salary_function(?)");
   ps.setInt(1, incrementPct);
   ResultSet rs = ps.executeQuery();
   ```

### JDBC Driver Migration Issues

#### Issue: Oracle-Specific JDBC Features Not Available
```
java.sql.SQLException: Method not supported
```

**Root Cause**: Application using Oracle-specific JDBC features

**Solutions**:

1. **Replace Oracle-Specific Types**
   ```java
   // Remove Oracle-specific imports
   // import oracle.jdbc.OracleTypes;
   // import oracle.sql.ARRAY;
   
   // Use standard JDBC types
   import java.sql.Types;
   import java.sql.Array;
   ```

2. **Update Connection String Format**
   ```properties
   # Old Oracle connection string:
   # db.url=jdbc:oracle:thin:@localhost:1521:XE
   
   # New PostgreSQL connection string:
   db.url=jdbc:postgresql://localhost:5432/hrdb
   ```

3. **Replace Oracle-Specific SQL Functions**
   ```sql
   -- Oracle: NVL function
   SELECT NVL(phone_number, 'N/A') FROM employees;
   
   -- PostgreSQL: COALESCE function
   SELECT COALESCE(phone_number, 'N/A') FROM employees;
   ```

## Database Connection Issues

### Connection Refused Errors

#### Issue: PostgreSQL Service Not Running
```
org.postgresql.util.PSQLException: Connection to localhost:5432 refused
```

**Diagnostic Steps**:

1. **Check PostgreSQL Service Status**
   ```cmd
   # Windows
   sc query postgresql-x64-17
   net start postgresql-x64-17
   
   # Linux
   sudo systemctl status postgresql
   sudo systemctl start postgresql
   
   # macOS
   brew services list | grep postgresql
   brew services start postgresql@17
   ```

2. **Verify PostgreSQL is Listening**
   ```cmd
   # Check if PostgreSQL is listening on port 5432
   netstat -an | findstr :5432
   
   # Test connection with psql
   psql -U postgres -h localhost -p 5432
   ```

3. **Check PostgreSQL Configuration**
   ```ini
   # postgresql.conf
   listen_addresses = 'localhost'  # or '*' for all interfaces
   port = 5432
   ```

#### Issue: Authentication Failed
```
org.postgresql.util.PSQLException: FATAL: password authentication failed for user "hr_user"
```

**Solutions**:

1. **Verify User Credentials**
   ```sql
   -- Connect as superuser and check user
   psql -U postgres
   \du hr_user
   
   -- Reset password if needed
   ALTER USER hr_user WITH PASSWORD 'hr_password';
   ```

2. **Check pg_hba.conf Configuration**
   ```ini
   # Ensure proper authentication method
   local   all             all                                     md5
   host    all             all             127.0.0.1/32            md5
   host    all             all             ::1/128                 md5
   ```

3. **Reload PostgreSQL Configuration**
   ```cmd
   # Linux
   sudo systemctl reload postgresql
   
   # Windows
   pg_ctl reload -D "C:\Program Files\PostgreSQL\17\data"
   ```

### Connection Pool Issues

#### Issue: HikariCP Connection Pool Exhaustion
```
com.zaxxer.hikari.pool.HikariPool - Connection is not available, request timed out after 30000ms
```

**Diagnostic Steps**:

1. **Monitor Active Connections**
   ```sql
   -- Check active connections to the database
   SELECT count(*), state, application_name 
   FROM pg_stat_activity 
   WHERE datname = 'hrdb' 
   GROUP BY state, application_name;
   ```

2. **Check for Connection Leaks**
   ```properties
   # Enable leak detection in application.properties
   hikari.leakDetectionThreshold=30000
   ```

3. **Adjust Pool Settings**
   ```properties
   # Increase pool size temporarily
   hikari.maximumPoolSize=20
   hikari.connectionTimeout=60000
   
   # Reduce connection lifetime
   hikari.maxLifetime=900000
   hikari.idleTimeout=300000
   ```

## Build and Deployment Issues

### Maven Build Problems

#### Issue: Dependency Resolution Failures
```
[ERROR] Failed to execute goal on project HRWebApp: Could not resolve dependencies
```

**Solutions**:

1. **Clear Maven Cache**
   ```cmd
   # Clear local repository
   mvnw.cmd dependency:purge-local-repository
   
   # Force update dependencies
   mvnw.cmd clean compile -U
   ```

2. **Check Maven Settings**
   ```xml
   <!-- ~/.m2/settings.xml -->
   <settings>
     <mirrors>
       <mirror>
         <id>central</id>
         <mirrorOf>central</mirrorOf>
         <url>https://repo1.maven.org/maven2</url>
       </mirror>
     </mirrors>
   </settings>
   ```

3. **Verify Internet Connectivity**
   ```cmd
   # Test Maven Central connectivity
   curl -I https://repo1.maven.org/maven2/
   
   # Check proxy settings if behind corporate firewall
   ```

#### Issue: Java Version Compatibility
```
[ERROR] Source option 21 is no longer supported. Use 8 or later.
```

**Solutions**:

1. **Verify Java Installation**
   ```cmd
   java -version
   echo %JAVA_HOME%
   
   # Should show OpenJDK 21.x.x
   ```

2. **Update Maven Compiler Plugin**
   ```xml
   <plugin>
     <groupId>org.apache.maven.plugins</groupId>
     <artifactId>maven-compiler-plugin</artifactId>
     <version>3.12.1</version>
     <configuration>
       <source>21</source>
       <target>21</target>
     </configuration>
   </plugin>
   ```

### Tomcat Deployment Issues

#### Issue: Jakarta EE vs Java EE Compatibility
```
java.lang.NoClassDefFoundError: javax/servlet/http/HttpServlet
```

**Root Cause**: Tomcat 10+ uses Jakarta EE (jakarta.servlet) instead of Java EE (javax.servlet)

**Solutions**:

1. **Update Servlet Imports**
   ```java
   // Old Java EE imports:
   // import javax.servlet.http.HttpServlet;
   // import javax.servlet.http.HttpServletRequest;
   
   // New Jakarta EE imports:
   import jakarta.servlet.http.HttpServlet;
   import jakarta.servlet.http.HttpServletRequest;
   ```

2. **Use Compatible Tomcat Version**
   ```
   Tomcat 10.1+ : Jakarta EE 9+ (jakarta.servlet)
   Tomcat 9.x   : Java EE 8 (javax.servlet)
   ```

3. **Update web.xml Namespace**
   ```xml
   <!-- Old Java EE namespace -->
   <!-- <web-app xmlns="http://java.sun.com/xml/ns/javaee" version="3.0"> -->
   
   <!-- New Jakarta EE namespace -->
   <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee 
                                https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
            version="6.0">
   ```

## Runtime Issues

### Memory Issues

#### Issue: OutOfMemoryError
```
java.lang.OutOfMemoryError: Java heap space
```

**Solutions**:

1. **Increase JVM Heap Size**
   ```bash
   # Set JVM options for Tomcat
   export CATALINA_OPTS="-Xms1g -Xmx4g -XX:+UseG1GC"
   
   # Windows
   set CATALINA_OPTS=-Xms1g -Xmx4g -XX:+UseG1GC
   ```

2. **Enable Heap Dump on OOM**
   ```bash
   export CATALINA_OPTS="$CATALINA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=logs/"
   ```

3. **Monitor Memory Usage**
   ```bash
   # Monitor JVM memory
   jstat -gc [tomcat_pid] 5s
   
   # Generate heap dump for analysis
   jmap -dump:format=b,file=heapdump.hprof [tomcat_pid]
   ```

### Application Startup Issues

#### Issue: Flyway Migration Failures
```
org.flywaydb.core.api.FlywayException: Unable to obtain connection from database
```

**Solutions**:

1. **Verify Database Connectivity**
   ```cmd
   # Test database connection
   psql -U hr_user -d hrdb -h localhost
   
   # Check Flyway configuration
   mvnw.cmd flyway:info
   ```

2. **Reset Migration State** (Development Only)
   ```cmd
   # Clean and re-run migrations
   mvnw.cmd flyway:clean
   mvnw.cmd flyway:migrate
   ```

3. **Manual Migration Repair**
   ```cmd
   # Repair migration metadata
   mvnw.cmd flyway:repair
   ```

## Performance Issues

### Slow Query Performance

#### Issue: Database Queries Taking Too Long
```
Query execution time: 5000ms (expected < 1000ms)
```

**Diagnostic Steps**:

1. **Enable Query Logging**
   ```sql
   -- Enable slow query logging
   ALTER SYSTEM SET log_min_duration_statement = 1000;
   SELECT pg_reload_conf();
   ```

2. **Analyze Query Performance**
   ```sql
   -- Check currently running queries
   SELECT pid, now() - pg_stat_activity.query_start AS duration, query 
   FROM pg_stat_activity 
   WHERE (now() - pg_stat_activity.query_start) > interval '5 minutes';
   
   -- Analyze specific query
   EXPLAIN ANALYZE SELECT * FROM employees WHERE job_id = 'IT_PROG';
   ```

3. **Create Missing Indexes**
   ```sql
   -- Create indexes for common queries
   CREATE INDEX CONCURRENTLY idx_employees_job_id ON employees(job_id);
   CREATE INDEX CONCURRENTLY idx_employees_salary ON employees(salary);
   ```

### Connection Pool Performance

#### Issue: High Connection Pool Utilization
```
HikariCP pool utilization: 95% (target < 80%)
```

**Solutions**:

1. **Monitor Pool Metrics**
   ```java
   // Enable JMX monitoring
   hikari.registerMbeans=true
   
   // Monitor via JConsole or programmatically
   HikariPoolMXBean poolBean = pool.getHikariPoolMXBean();
   int activeConnections = poolBean.getActiveConnections();
   int totalConnections = poolBean.getTotalConnections();
   ```

2. **Optimize Pool Configuration**
   ```properties
   # Increase pool size if needed
   hikari.maximumPoolSize=15
   hikari.minimumIdle=5
   
   # Reduce connection timeout
   hikari.connectionTimeout=20000
   ```

## Authentication and Security Issues

### Login Problems

#### Issue: Valid Credentials Rejected
```
HTTP Status 403 - Access Denied
```

**Solutions**:

1. **Verify tomcat-users.xml**
   ```xml
   <tomcat-users>
     <role rolename="manager"/>
     <role rolename="staff"/>
     <user username="admin" password="admin123" roles="manager"/>
     <user username="hr" password="hr123" roles="staff"/>
   </tomcat-users>
   ```

2. **Check web.xml Security Configuration**
   ```xml
   <security-constraint>
     <web-resource-collection>
       <web-resource-name>Protected Area</web-resource-name>
       <url-pattern>/protected/*</url-pattern>
     </web-resource-collection>
     <auth-constraint>
       <role-name>manager</role-name>
       <role-name>staff</role-name>
     </auth-constraint>
   </security-constraint>
   ```

3. **Clear Browser Cache**
   ```
   - Clear browser cache and cookies
   - Try incognito/private browsing mode
   - Test with curl to eliminate browser issues
   ```

## Environment-Specific Issues

### Development Environment

#### Issue: TestContainers Fails to Start
```
org.testcontainers.containers.ContainerLaunchException: Could not create/start container
```

**Solutions**:

1. **Verify Docker Installation**
   ```cmd
   # Check Docker status
   docker version
   docker ps
   
   # Test PostgreSQL container
   docker run --rm postgres:17 postgres --version
   ```

2. **Configure TestContainers**
   ```properties
   # testcontainers.properties
   testcontainers.reuse.enable=true
   testcontainers.ryuk.disabled=false
   ```

### Production Environment

#### Issue: SSL Connection Problems
```
org.postgresql.util.PSQLException: SSL error: certificate verify failed
```

**Solutions**:

1. **Configure SSL Properly**
   ```properties
   # SSL configuration
   db.url=jdbc:postgresql://prod-server:5432/hrdb?ssl=true&sslmode=require&sslcert=client.crt&sslkey=client.key&sslrootcert=ca.crt
   ```

2. **Import Certificates**
   ```bash
   # Import CA certificate to Java truststore
   keytool -import -alias postgresql-ca -file ca.crt -keystore $JAVA_HOME/lib/security/cacerts
   ```

## Data Migration Issues

### Data Integrity Problems

#### Issue: Foreign Key Constraint Violations
```
ERROR: insert or update on table "employees" violates foreign key constraint
```

**Solutions**:

1. **Check Data Consistency**
   ```sql
   -- Find orphaned records
   SELECT e.employee_id, e.job_id 
   FROM employees e 
   LEFT JOIN jobs j ON e.job_id = j.job_id 
   WHERE j.job_id IS NULL;
   ```

2. **Create Missing Reference Data**
   ```sql
   -- Insert missing job records
   INSERT INTO jobs (job_id, job_title) 
   VALUES ('IT_PROG', 'Programmer');
   ```

#### Issue: Character Encoding Problems
```
ERROR: invalid byte sequence for encoding "UTF8"
```

**Solutions**:

1. **Set Proper Encoding**
   ```sql
   -- Create database with UTF8 encoding
   CREATE DATABASE hrdb 
   WITH ENCODING = 'UTF8' 
   LC_COLLATE = 'en_US.UTF-8' 
   LC_CTYPE = 'en_US.UTF-8';
   ```

2. **Clean Data Before Migration**
   ```bash
   # Convert file encoding
   iconv -f ISO-8859-1 -t UTF-8 data_export.csv > data_export_utf8.csv
   ```

## Advanced Troubleshooting

### Performance Profiling

#### JVM Performance Analysis
```bash
# Enable JFR profiling
java -XX:+FlightRecorder -XX:StartFlightRecording=duration=60s,filename=profile.jfr

# Use async profiler
java -jar async-profiler.jar -d 60 -f profile.html [tomcat_pid]

# Monitor GC performance
jstat -gc [tomcat_pid] 5s
```

#### Database Performance Analysis
```sql
-- Enable pg_stat_statements
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Monitor query performance
SELECT query, calls, total_time, mean_time, 
       100.0 * total_time / sum(total_time) OVER() AS percentage
FROM pg_stat_statements 
ORDER BY total_time DESC 
LIMIT 10;

-- Monitor locks and blocking
SELECT blocked_locks.pid AS blocked_pid,
       blocked_activity.usename AS blocked_user,
       blocking_locks.pid AS blocking_pid,
       blocking_activity.usename AS blocking_user,
       blocked_activity.query AS blocked_statement
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```

### Network Troubleshooting

#### Connection Issues
```cmd
# Test network connectivity
telnet localhost 5432
ping localhost

# Check port availability
netstat -an | findstr :5432
netstat -an | findstr :8080

# Test with different tools
psql -U hr_user -h localhost -p 5432 -d hrdb
curl -v http://localhost:8080/HRWebApp/health
```

### Log Analysis

#### Automated Log Analysis Script
```bash
#!/bin/bash
# analyze-logs.sh

echo "=== Analyzing Application Logs ==="
echo "Error count in last hour:"
grep "ERROR" logs/hrapp.log | grep "$(date -d '1 hour ago' '+%Y-%m-%d %H')" | wc -l

echo "Most common errors:"
grep "ERROR" logs/hrapp.log | awk '{print $5}' | sort | uniq -c | sort -nr | head -5

echo "=== Analyzing PostgreSQL Logs ==="
echo "Connection errors:"
grep "FATAL" /var/log/postgresql/postgresql-17-main.log | tail -10

echo "Slow queries (>1s):"
grep "duration:" /var/log/postgresql/postgresql-17-main.log | awk '$12 > 1000' | tail -10

echo "=== Analyzing Tomcat Logs ==="
echo "Deployment errors:"
grep "ERROR" $CATALINA_HOME/logs/catalina.out | grep -i deploy | tail -10
```

## Getting Help

### Information to Collect for Support

When reporting issues, collect the following information:

1. **System Information**
   ```cmd
   # Windows
   systeminfo > system_info.txt
   java -version > java_version.txt 2>&1
   
   # Linux
   uname -a > system_info.txt
   java -version > java_version.txt 2>&1
   ```

2. **Application Configuration**
   ```cmd
   # Copy configuration files
   copy src\main\resources\application*.properties config_backup\
   copy pom.xml config_backup\
   copy tomcat-users.xml config_backup\
   ```

3. **Log Files**
   ```cmd
   # Collect all relevant logs
   copy logs\*.log log_backup\
   copy "%CATALINA_HOME%\logs\*" log_backup\
   copy "C:\Program Files\PostgreSQL\17\data\log\*" log_backup\
   ```

4. **Error Details**
   - Exact error message and stack trace
   - Steps to reproduce the issue
   - When the issue started occurring
   - Any recent changes to the system

### Support Resources

- **PostgreSQL Documentation**: https://www.postgresql.org/docs/17/
- **HikariCP GitHub**: https://github.com/brettwooldridge/HikariCP
- **Apache Tomcat Documentation**: https://tomcat.apache.org/tomcat-10.1-doc/
- **OpenJDK Documentation**: https://openjdk.org/projects/jdk/21/
- **Stack Overflow**: Tag questions with `postgresql`, `hikaricp`, `tomcat`, `openjdk`

This troubleshooting guide should help resolve most common issues encountered during the Oracle to OpenJDK migration. For complex issues, use the diagnostic scripts and log collection procedures to gather information before seeking additional support.