# Performance Tuning Guide - HR Web Application

This guide provides comprehensive performance tuning recommendations for the HR Web Application running on OpenJDK 21 with PostgreSQL and HikariCP after migration from Oracle.

## Table of Contents

- [Performance Baseline](#performance-baseline)
- [HikariCP Connection Pool Tuning](#hikaricp-connection-pool-tuning)
- [PostgreSQL Database Optimization](#postgresql-database-optimization)
- [JVM Performance Tuning](#jvm-performance-tuning)
- [Application-Level Optimizations](#application-level-optimizations)
- [Tomcat Server Tuning](#tomcat-server-tuning)
- [Monitoring and Metrics](#monitoring-and-metrics)
- [Load Testing](#load-testing)
- [Environment-Specific Tuning](#environment-specific-tuning)
- [Migration-Specific Optimizations](#migration-specific-optimizations)

## Performance Baseline

### Expected Performance Metrics

| Metric | Target | Acceptable | Poor | Notes |
|--------|--------|------------|------|-------|
| **Page Load Time** | < 500ms | < 1000ms | > 2000ms | End-to-end response time |
| **Database Query Time** | < 100ms | < 500ms | > 1000ms | Average query execution |
| **Memory Usage** | < 512MB | < 1GB | > 2GB | JVM heap utilization |
| **CPU Usage** | < 50% | < 75% | > 90% | Average CPU utilization |
| **Concurrent Users** | 50+ | 25+ | < 10 | Simultaneous active users |
| **Connection Pool Utilization** | < 70% | < 85% | > 95% | HikariCP pool usage |
| **Throughput** | 100+ req/s | 50+ req/s | < 25 req/s | Requests per second |

### Performance Testing Setup

#### Basic Load Test Script
```bash
#!/bin/bash
# load-test.sh

echo "Starting HR Web Application Load Test"
echo "====================================="

# Configuration
BASE_URL="http://localhost:8080/HRWebApp"
CONCURRENT_USERS=10
TOTAL_REQUESTS=1000
TEST_DURATION=300  # 5 minutes

# Create curl format file
cat > curl-format.txt << 'EOF'
     time_namelookup:  %{time_namelookup}\n
        time_connect:  %{time_connect}\n
     time_appconnect:  %{time_appconnect}\n
    time_pretransfer:  %{time_pretransfer}\n
       time_redirect:  %{time_redirect}\n
  time_starttransfer:  %{time_starttransfer}\n
                     ----------\n
          time_total:  %{time_total}\n
EOF

# Test scenarios
echo "1. Testing login page load time..."
for i in {1..10}; do
  curl -w "@curl-format.txt" -o /dev/null -s "$BASE_URL/login.html"
done

echo "2. Testing authenticated employee listing..."
for i in {1..10}; do
  curl -w "@curl-format.txt" -o /dev/null -s \
    -u admin:admin123 \
    "$BASE_URL/protected/listAll"
done

echo "3. Concurrent user simulation..."
for i in $(seq 1 $CONCURRENT_USERS); do
  {
    for j in $(seq 1 $((TOTAL_REQUESTS / CONCURRENT_USERS))); do
      curl -s -u admin:admin123 "$BASE_URL/protected/listAll" > /dev/null
    done
  } &
done
wait

echo "Load test completed."
rm curl-format.txt
```

#### JMeter Test Plan Configuration
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan testname="HR App Performance Test">
      <elementProp name="TestPlan.arguments" elementType="Arguments">
        <collectionProp name="Arguments.arguments">
          <elementProp name="host" elementType="Argument">
            <stringProp name="Argument.name">host</stringProp>
            <stringProp name="Argument.value">localhost</stringProp>
          </elementProp>
          <elementProp name="port" elementType="Argument">
            <stringProp name="Argument.name">port</stringProp>
            <stringProp name="Argument.value">8080</stringProp>
          </elementProp>
          <elementProp name="users" elementType="Argument">
            <stringProp name="Argument.name">users</stringProp>
            <stringProp name="Argument.value">50</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
  </hashTree>
</jmeterTestPlan>
```

## HikariCP Connection Pool Tuning

### Pool Size Optimization

#### Formula-Based Sizing
```properties
# Recommended formula: ((core_count * 2) + effective_spindle_count)
# For 4-core system with SSD: (4 * 2) + 1 = 9
hikari.maximumPoolSize=10
hikari.minimumIdle=2

# Conservative approach for high-concurrency
hikari.maximumPoolSize=20
hikari.minimumIdle=5

# Aggressive approach for maximum performance
hikari.maximumPoolSize=30
hikari.minimumIdle=10
```

#### Environment-Specific Pool Configurations

**Development Environment:**
```properties
# Optimized for fast startup and low resource usage
hikari.maximumPoolSize=3
hikari.minimumIdle=1
hikari.connectionTimeout=10000
hikari.idleTimeout=300000
hikari.maxLifetime=900000
hikari.leakDetectionThreshold=0
```

**Production Environment:**
```properties
# Optimized for performance and reliability
hikari.maximumPoolSize=15
hikari.minimumIdle=5
hikari.connectionTimeout=20000
hikari.idleTimeout=600000
hikari.maxLifetime=1800000
hikari.leakDetectionThreshold=60000
```

**High-Load Environment:**
```properties
# Optimized for maximum throughput
hikari.maximumPoolSize=25
hikari.minimumIdle=10
hikari.connectionTimeout=30000
hikari.idleTimeout=300000
hikari.maxLifetime=1200000
hikari.leakDetectionThreshold=30000
```

### Advanced HikariCP Settings

#### Performance Optimizations
```properties
# Statement caching - Critical for performance
hikari.cachePrepStmts=true
hikari.prepStmtCacheSize=250
hikari.prepStmtCacheSqlLimit=2048

# PostgreSQL-specific optimizations
hikari.useServerPrepStmts=true
hikari.useLocalSessionState=true
hikari.rewriteBatchedStatements=true
hikari.cacheResultSetMetadata=true
hikari.cacheServerConfiguration=true
hikari.elideSetAutoCommits=true
hikari.maintainTimeStats=false

# Connection validation
hikari.connectionTestQuery=SELECT 1
hikari.validationTimeout=5000
hikari.initializationFailTimeout=30000
```

#### Connection Lifecycle Management
```properties
# Optimal connection lifecycle settings
hikari.keepaliveTime=300000      # 5 minutes
hikari.maxLifetime=1800000       # 30 minutes
hikari.idleTimeout=600000        # 10 minutes

# For high-frequency applications
hikari.keepaliveTime=120000      # 2 minutes
hikari.maxLifetime=900000        # 15 minutes
hikari.idleTimeout=300000        # 5 minutes
```

### Monitoring HikariCP Performance

#### JMX Metrics Collection
```java
// Enable JMX monitoring
hikari.registerMbeans=true

// Monitor via JConsole or programmatically
public class HikariMetricsCollector {
    private final HikariDataSource dataSource;
    
    public void logPoolMetrics() {
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        
        int active = pool.getActiveConnections();
        int idle = pool.getIdleConnections();
        int total = pool.getTotalConnections();
        int waiting = pool.getThreadsAwaitingConnection();
        
        double utilization = (active * 100.0) / total;
        
        logger.info("HikariCP Metrics - Active: {}, Idle: {}, Total: {}, " +
                   "Waiting: {}, Utilization: {:.1f}%", 
                   active, idle, total, waiting, utilization);
        
        // Alert if utilization is too high
        if (utilization > 80) {
            logger.warn("High connection pool utilization: {:.1f}%", utilization);
        }
    }
}
```

#### Custom Health Checks
```java
@Component
public class ConnectionPoolHealthIndicator {
    
    @Autowired
    private HikariDataSource dataSource;
    
    public boolean isHealthy() {
        try {
            HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
            int active = pool.getActiveConnections();
            int total = pool.getTotalConnections();
            
            // Consider unhealthy if utilization > 90%
            return (active * 100.0 / total) < 90;
        } catch (Exception e) {
            return false;
        }
    }
}
```

## PostgreSQL Database Optimization

### Configuration Tuning

#### Memory Settings
```ini
# postgresql.conf - Memory configuration based on system RAM

# For 8GB system
shared_buffers = 2GB                     # 25% of system RAM
effective_cache_size = 6GB              # 75% of system RAM  
work_mem = 4MB                          # Per connection work memory
maintenance_work_mem = 512MB            # Maintenance operations

# For 16GB system
shared_buffers = 4GB
effective_cache_size = 12GB
work_mem = 8MB
maintenance_work_mem = 1GB

# For 32GB system
shared_buffers = 8GB
effective_cache_size = 24GB
work_mem = 16MB
maintenance_work_mem = 2GB
```

#### Connection and Performance Settings
```ini
# Connection settings
max_connections = 100                    # Adjust based on application needs
superuser_reserved_connections = 3       # Reserved for superuser

# Performance settings optimized for SSD
random_page_cost = 1.1                  # SSD optimization (default: 4.0)
effective_io_concurrency = 200          # SSD optimization (default: 1)
seq_page_cost = 1.0                     # Sequential scan cost

# Checkpoint settings for performance
checkpoint_completion_target = 0.9       # Spread checkpoints over time
checkpoint_timeout = 5min                # Maximum time between checkpoints
max_wal_size = 1GB                       # WAL size limit
min_wal_size = 80MB                      # Minimum WAL size

# WAL settings
wal_buffers = 16MB                       # WAL buffer size
wal_writer_delay = 200ms                 # WAL writer delay
commit_delay = 0                         # Commit delay
commit_siblings = 5                      # Commit siblings
```

#### Query Planner Settings
```ini
# Query planner configuration
default_statistics_target = 100         # Statistics detail level
constraint_exclusion = partition        # Enable constraint exclusion
enable_partitionwise_join = on          # Partition-wise joins
enable_partitionwise_aggregate = on     # Partition-wise aggregates
enable_hashjoin = on                     # Enable hash joins
enable_mergejoin = on                    # Enable merge joins
enable_nestloop = on                     # Enable nested loop joins
```

### Index Optimization

#### Essential Indexes for HR Application
```sql
-- Primary indexes for common queries
CREATE INDEX CONCURRENTLY idx_employees_job_id ON employees(job_id);
CREATE INDEX CONCURRENTLY idx_employees_salary ON employees(salary);
CREATE INDEX CONCURRENTLY idx_employees_name ON employees(first_name, last_name);
CREATE INDEX CONCURRENTLY idx_employees_email ON employees(email);

-- Composite indexes for complex queries
CREATE INDEX CONCURRENTLY idx_employees_job_salary ON employees(job_id, salary);
CREATE INDEX CONCURRENTLY idx_employees_name_job ON employees(last_name, first_name, job_id);

-- Partial indexes for specific conditions
CREATE INDEX CONCURRENTLY idx_employees_high_salary 
ON employees(salary) WHERE salary > 50000;

CREATE INDEX CONCURRENTLY idx_employees_active 
ON employees(employee_id) WHERE active = true;

-- Functional indexes for case-insensitive searches
CREATE INDEX CONCURRENTLY idx_employees_email_lower 
ON employees(LOWER(email));

CREATE INDEX CONCURRENTLY idx_employees_name_lower 
ON employees(LOWER(first_name), LOWER(last_name));
```

#### Index Maintenance and Monitoring
```sql
-- Monitor index usage
SELECT schemaname, tablename, indexname, idx_tup_read, idx_tup_fetch,
       idx_tup_read + idx_tup_fetch as total_reads
FROM pg_stat_user_indexes 
WHERE schemaname = 'public'
ORDER BY total_reads DESC;

-- Find unused indexes
SELECT schemaname, tablename, indexname, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes 
WHERE idx_tup_read = 0 AND idx_tup_fetch = 0
AND schemaname = 'public'
AND indexname NOT LIKE '%_pkey';

-- Check index sizes
SELECT schemaname, tablename, indexname, 
       pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes 
WHERE schemaname = 'public'
ORDER BY pg_relation_size(indexrelid) DESC;

-- Reindex periodically (during maintenance window)
REINDEX INDEX CONCURRENTLY idx_employees_job_id;
REINDEX TABLE employees;
```

### Query Optimization

#### Enable Query Statistics
```sql
-- Enable pg_stat_statements extension
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Configure in postgresql.conf
-- shared_preload_libraries = 'pg_stat_statements'
-- pg_stat_statements.max = 10000
-- pg_stat_statements.track = all
```

#### Analyze Query Performance
```sql
-- Find slowest queries
SELECT query, calls, total_time, mean_time, 
       100.0 * total_time / sum(total_time) OVER() AS percentage,
       stddev_time, min_time, max_time
FROM pg_stat_statements 
WHERE query NOT LIKE '%pg_stat_statements%'
ORDER BY total_time DESC 
LIMIT 10;

-- Find most frequently called queries
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements 
WHERE query NOT LIKE '%pg_stat_statements%'
ORDER BY calls DESC 
LIMIT 10;

-- Analyze specific query
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) 
SELECT e.*, j.job_title 
FROM employees e 
JOIN jobs j ON e.job_id = j.job_id 
WHERE e.salary > 50000 
ORDER BY e.salary DESC;
```

#### Optimize Common Queries
```sql
-- Original query (potentially slow)
SELECT * FROM employees WHERE UPPER(first_name) = 'JOHN';

-- Optimized with functional index
CREATE INDEX idx_employees_first_name_upper ON employees(UPPER(first_name));

-- Use parameterized queries to enable plan caching
PREPARE get_employee_by_id AS 
SELECT * FROM employees WHERE employee_id = $1;

EXECUTE get_employee_by_id(100);

-- Optimize salary increment query
PREPARE increment_salary AS
UPDATE employees 
SET salary = salary * (1 + $1 / 100.0)
WHERE job_id = $2;

EXECUTE increment_salary(10, 'IT_PROG');
```

### Connection Optimization

#### PostgreSQL JDBC URL Optimization
```properties
# Optimized JDBC URL with performance parameters
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
    stringtype=unspecified&\
    sendBufferSize=65536&\
    receiveBufferSize=65536
```

## JVM Performance Tuning

### OpenJDK 21 Optimizations

#### Production JVM Settings
```bash
# Comprehensive production JVM configuration
export JAVA_OPTS="-server \
    -Xms2g -Xmx8g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:G1HeapRegionSize=16m \
    -XX:G1NewSizePercent=20 \
    -XX:G1MaxNewSizePercent=30 \
    -XX:G1MixedGCCountTarget=8 \
    -XX:InitiatingHeapOccupancyPercent=45 \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -XX:ReservedCodeCacheSize=256m \
    -XX:InitialCodeCacheSize=64m \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=4 \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC \
    -Djava.security.egd=file:/dev/./urandom"
```

#### Development JVM Settings
```bash
# Optimized for fast startup and development
export JAVA_OPTS="-server \
    -Xms512m -Xmx2g \
    -XX:+UseParallelGC \
    -XX:TieredStopAtLevel=1 \
    -XX:+UseCompressedOops \
    -Djava.awt.headless=true \
    -Dspring.profiles.active=dev"
```

### Garbage Collection Tuning

#### G1GC Configuration (Recommended for Production)
```bash
# G1GC optimized for low latency
export JAVA_OPTS="$JAVA_OPTS \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:G1HeapRegionSize=16m \
    -XX:G1NewSizePercent=20 \
    -XX:G1MaxNewSizePercent=30 \
    -XX:G1MixedGCCountTarget=8 \
    -XX:InitiatingHeapOccupancyPercent=45 \
    -XX:G1MixedGCLiveThresholdPercent=85 \
    -XX:G1HeapWastePercent=5 \
    -XX:+UseStringDeduplication"
```

#### Parallel GC Configuration (Alternative)
```bash
# Parallel GC for high throughput
export JAVA_OPTS="$JAVA_OPTS \
    -XX:+UseParallelGC \
    -XX:ParallelGCThreads=8 \
    -XX:MaxGCPauseMillis=500 \
    -XX:GCTimeRatio=19 \
    -XX:+UseAdaptiveSizePolicy"
```

#### GC Logging and Monitoring
```bash
# Enable comprehensive GC logging
export JAVA_OPTS="$JAVA_OPTS \
    -Xlog:gc*:logs/gc.log:time,tags \
    -XX:+UseGCLogFileRotation \
    -XX:NumberOfGCLogFiles=5 \
    -XX:GCLogFileSize=10M"

# Monitor GC performance
jstat -gc [pid] 5s

# Analyze GC logs
# Use tools like GCViewer, GCPlot.com, or CRaC
```

### Memory Management

#### Heap Size Calculation
```bash
# Calculate optimal heap size based on available memory
# Rule of thumb: 50-75% of available system memory

# For 8GB system:
export JAVA_OPTS="-Xms2g -Xmx4g"

# For 16GB system:
export JAVA_OPTS="-Xms4g -Xmx8g"

# For 32GB system:
export JAVA_OPTS="-Xms8g -Xmx16g"

# Enable heap dump on OOM for debugging
export JAVA_OPTS="$JAVA_OPTS \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=logs/heapdump.hprof \
    -XX:OnOutOfMemoryError='kill -9 %p'"
```

#### Metaspace Configuration
```bash
# Configure Metaspace for class metadata
export JAVA_OPTS="$JAVA_OPTS \
    -XX:MetaspaceSize=256m \
    -XX:MaxMetaspaceSize=512m \
    -XX:CompressedClassSpaceSize=128m"
```

## Application-Level Optimizations

### Connection Management Best Practices

#### Proper Resource Management
```java
// Use try-with-resources for automatic cleanup
public List<Employee> getAllEmployees() {
    String sql = "SELECT employee_id, first_name, last_name, email, " +
                "phone_number, job_id, salary FROM employees ORDER BY employee_id";
    List<Employee> employees = new ArrayList<>();
    
    try (Connection conn = ConnectionFactory.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
        
        while (rs.next()) {
            employees.add(mapResultSetToEmployee(rs));
        }
    } catch (SQLException e) {
        logger.error("Error retrieving employees", e);
        throw new RuntimeException("Database error", e);
    }
    
    return employees;
}

// Efficient ResultSet mapping
private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
    Employee emp = new Employee();
    emp.setEmployeeId(rs.getInt("employee_id"));
    emp.setFirstName(rs.getString("first_name"));
    emp.setLastName(rs.getString("last_name"));
    emp.setEmail(rs.getString("email"));
    emp.setPhoneNumber(rs.getString("phone_number"));
    emp.setJobId(rs.getString("job_id"));
    emp.setSalary(rs.getBigDecimal("salary"));
    return emp;
}
```

#### Batch Operations Optimization
```java
// Optimize bulk operations with batching
public void updateEmployeeSalaries(List<Employee> employees) {
    String sql = "UPDATE employees SET salary = ? WHERE employee_id = ?";
    
    try (Connection conn = ConnectionFactory.getConnection()) {
        conn.setAutoCommit(false);
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int batchSize = 100;
            int count = 0;
            
            for (Employee emp : employees) {
                stmt.setBigDecimal(1, emp.getSalary());
                stmt.setInt(2, emp.getEmployeeId());
                stmt.addBatch();
                
                if (++count % batchSize == 0) {
                    stmt.executeBatch();
                    stmt.clearBatch();
                }
            }
            
            // Execute remaining batch
            if (count % batchSize != 0) {
                stmt.executeBatch();
            }
            
            conn.commit();
            
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }
        
    } catch (SQLException e) {
        logger.error("Error updating employee salaries", e);
        throw new RuntimeException("Batch update failed", e);
    }
}
```

### Caching Strategies

#### Simple In-Memory Caching
```java
public class JobTitleCache {
    private static final Map<String, String> JOB_TITLES = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY = 3600000; // 1 hour
    private static volatile long lastRefresh = 0;
    
    public String getJobTitle(String jobId) {
        refreshCacheIfNeeded();
        return JOB_TITLES.computeIfAbsent(jobId, this::loadJobTitleFromDB);
    }
    
    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastRefresh > CACHE_EXPIRY) {
            synchronized (JOB_TITLES) {
                if (now - lastRefresh > CACHE_EXPIRY) {
                    JOB_TITLES.clear();
                    lastRefresh = now;
                }
            }
        }
    }
    
    private String loadJobTitleFromDB(String jobId) {
        String sql = "SELECT job_title FROM jobs WHERE job_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, jobId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("job_title") : "Unknown";
            }
        } catch (SQLException e) {
            logger.error("Error loading job title for jobId: " + jobId, e);
            return "Unknown";
        }
    }
}
```

#### Advanced Caching with Caffeine
```java
public class EmployeeService {
    private final Cache<String, List<Employee>> employeeCache = 
        Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(15))
            .recordStats()
            .build();
    
    public List<Employee> getEmployeesByJobId(String jobId) {
        return employeeCache.get(jobId, this::loadEmployeesByJobId);
    }
    
    private List<Employee> loadEmployeesByJobId(String jobId) {
        String sql = "SELECT * FROM employees WHERE job_id = ? ORDER BY employee_id";
        List<Employee> employees = new ArrayList<>();
        
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, jobId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    employees.add(mapResultSetToEmployee(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading employees for jobId: " + jobId, e);
        }
        
        return employees;
    }
    
    public void invalidateCache() {
        employeeCache.invalidateAll();
    }
    
    public CacheStats getCacheStats() {
        return employeeCache.stats();
    }
}
```

### Query Optimization

#### Prepared Statement Caching
```java
public class OptimizedJdbcBeanImpl implements JdbcBean {
    private static final Map<String, String> PREPARED_QUERIES = Map.of(
        "GET_ALL_EMPLOYEES", "SELECT * FROM employees ORDER BY employee_id",
        "GET_EMPLOYEE_BY_ID", "SELECT * FROM employees WHERE employee_id = ?",
        "UPDATE_EMPLOYEE_SALARY", "UPDATE employees SET salary = ? WHERE employee_id = ?",
        "DELETE_EMPLOYEE", "DELETE FROM employees WHERE employee_id = ?"
    );
    
    @Override
    public List<Employee> getAllEmployees() {
        return executeQuery(PREPARED_QUERIES.get("GET_ALL_EMPLOYEES"), 
                          this::mapResultSetToEmployeeList);
    }
    
    @Override
    public Employee getEmployeeById(int employeeId) {
        return executeQuery(PREPARED_QUERIES.get("GET_EMPLOYEE_BY_ID"), 
                          stmt -> {
                              stmt.setInt(1, employeeId);
                              return stmt;
                          },
                          this::mapResultSetToEmployee);
    }
    
    private <T> T executeQuery(String sql, Function<ResultSet, T> mapper) {
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            return mapper.apply(rs);
        } catch (SQLException e) {
            logger.error("Error executing query: " + sql, e);
            throw new RuntimeException("Database error", e);
        }
    }
}
```

## Tomcat Server Tuning

### Connector Configuration

#### HTTP Connector Optimization
```xml
<!-- server.xml - Optimized HTTP connector -->
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
           tcpNoDelay="true"
           keepAliveTimeout="15000"
           maxKeepAliveRequests="100" />
```

#### NIO Connector for High Performance
```xml
<!-- server.xml - NIO connector for better performance -->
<Connector port="8080" protocol="org.apache.coyote.http11.Http11NioProtocol"
           connectionTimeout="20000"
           redirectPort="8443"
           maxThreads="200"
           minSpareThreads="25"
           acceptCount="100"
           enableLookups="false"
           compression="on"
           compressionMinSize="2048"
           compressableMimeType="text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json"
           URIEncoding="UTF-8"
           maxConnections="8192"
           acceptorThreadCount="2"
           pollerThreadCount="4" />
```

#### Thread Pool Configuration
```xml
<!-- Shared thread pool for better resource management -->
<Executor name="tomcatThreadPool" 
          namePrefix="catalina-exec-"
          maxThreads="200"
          minSpareThreads="25"
          maxIdleTime="60000"
          prestartminSpareThreads="true"
          maxQueueSize="100" />

<Connector executor="tomcatThreadPool"
           port="8080" protocol="HTTP/1.1"
           connectionTimeout="20000"
           redirectPort="8443" />
```

### Memory and Resource Settings

#### JVM Memory for Tomcat
```bash
# Tomcat-specific memory settings
export CATALINA_OPTS="-server \
    -Xms2g -Xmx8g \
    -XX:MetaspaceSize=256m \
    -XX:MaxMetaspaceSize=512m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8 \
    -Djava.security.egd=file:/dev/./urandom"
```

#### Resource Cleanup Configuration
```xml
<!-- context.xml - Resource cleanup configuration -->
<Context>
    <Manager pathname="" />
    <JarScanner scanClassPath="false" />
    <Resources cachingAllowed="true" 
               cacheMaxSize="100000" 
               cacheTtl="300000" />
    
    <!-- Prevent memory leaks -->
    <Listener className="org.apache.catalina.core.JreMemoryLeakPreventionListener" />
    <Listener className="org.apache.catalina.mbeans.GlobalResourcesLifecycleListener" />
    <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />
</Context>
```

## Monitoring and Metrics

### Application Performance Monitoring

#### Custom Metrics Collection
```java
@Component
public class PerformanceMetrics {
    private final MeterRegistry meterRegistry;
    private final Timer.Sample sample;
    
    public PerformanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public void recordDatabaseQueryTime(String operation, long duration) {
        Timer.builder("database.query.duration")
            .tag("operation", operation)
            .register(meterRegistry)
            .record(duration, TimeUnit.MILLISECONDS);
    }
    
    public void recordConnectionPoolUsage(int active, int total) {
        Gauge.builder("hikaricp.connections.active")
            .register(meterRegistry, () -> active);
        Gauge.builder("hikaricp.connections.total")
            .register(meterRegistry, () -> total);
        
        double utilization = (active * 100.0) / total;
        Gauge.builder("hikaricp.connections.utilization")
            .register(meterRegistry, () -> utilization);
    }
    
    public void recordHttpRequestTime(String endpoint, long duration) {
        Timer.builder("http.request.duration")
            .tag("endpoint", endpoint)
            .register(meterRegistry)
            .record(duration, TimeUnit.MILLISECONDS);
    }
}
```

#### Health Check Endpoints
```java
@WebServlet("/health/detailed")
public class DetailedHealthCheckServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        Map<String, Object> health = new HashMap<>();
        
        // Database health
        health.put("database", checkDatabaseHealth());
        
        // Connection pool health
        health.put("connectionPool", checkConnectionPoolHealth());
        
        // Memory health
        health.put("memory", checkMemoryHealth());
        
        // Application health
        health.put("application", checkApplicationHealth());
        
        resp.setContentType("application/json");
        resp.getWriter().write(new Gson().toJson(health));
    }
    
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();
        try (Connection conn = ConnectionFactory.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT 1");
            ResultSet rs = stmt.executeQuery();
            
            dbHealth.put("status", "UP");
            dbHealth.put("responseTime", measureQueryTime("SELECT 1"));
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }
        return dbHealth;
    }
    
    private Map<String, Object> checkConnectionPoolHealth() {
        Map<String, Object> poolHealth = new HashMap<>();
        try {
            HikariPoolMXBean pool = ConnectionFactory.getHikariPoolMXBean();
            int active = pool.getActiveConnections();
            int total = pool.getTotalConnections();
            double utilization = (active * 100.0) / total;
            
            poolHealth.put("status", utilization < 90 ? "UP" : "WARN");
            poolHealth.put("activeConnections", active);
            poolHealth.put("totalConnections", total);
            poolHealth.put("utilization", String.format("%.1f%%", utilization));
        } catch (Exception e) {
            poolHealth.put("status", "DOWN");
            poolHealth.put("error", e.getMessage());
        }
        return poolHealth;
    }
}
```

### Database Monitoring

#### Performance Queries
```sql
-- Monitor connection usage
SELECT count(*), state, application_name, client_addr
FROM pg_stat_activity 
WHERE datname = 'hrdb' 
GROUP BY state, application_name, client_addr
ORDER BY count(*) DESC;

-- Monitor query performance
SELECT query, calls, total_time, mean_time, 
       stddev_time, min_time, max_time,
       100.0 * total_time / sum(total_time) OVER() AS percentage
FROM pg_stat_statements 
WHERE query LIKE '%employees%'
ORDER BY mean_time DESC
LIMIT 10;

-- Monitor table statistics
SELECT schemaname, tablename, n_tup_ins, n_tup_upd, n_tup_del, 
       n_tup_hot_upd, n_live_tup, n_dead_tup, 
       last_vacuum, last_autovacuum, last_analyze, last_autoanalyze
FROM pg_stat_user_tables 
WHERE tablename = 'employees';

-- Monitor index usage and efficiency
SELECT schemaname, tablename, indexname, 
       idx_tup_read, idx_tup_fetch,
       pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes 
WHERE schemaname = 'public'
ORDER BY idx_tup_read DESC;

-- Monitor locks and blocking queries
SELECT blocked_locks.pid AS blocked_pid,
       blocked_activity.usename AS blocked_user,
       blocking_locks.pid AS blocking_pid,
       blocking_activity.usename AS blocking_user,
       blocked_activity.query AS blocked_statement,
       blocking_activity.query AS current_statement_in_blocking_process
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```

## Load Testing

### Comprehensive Load Testing Strategy

#### Test Scenarios

**Scenario 1: Normal Load**
```bash
# 25 concurrent users, 10-minute test
ab -n 15000 -c 25 -t 600 \
   -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" \
   http://localhost:8080/HRWebApp/protected/listAll
```

**Scenario 2: Peak Load**
```bash
# 50 concurrent users, stress test
ab -n 30000 -c 50 -t 1200 \
   -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" \
   http://localhost:8080/HRWebApp/protected/listAll
```

**Scenario 3: Database Write Load**
```bash
# Test salary increment functionality
for i in {1..50}; do
  curl -X POST \
    -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" \
    -d "increment_pct=5" \
    http://localhost:8080/HRWebApp/protected/incrementSalary &
done
wait
```

#### Performance Benchmarking Script
```bash
#!/bin/bash
# performance-benchmark.sh

echo "HR Web Application Performance Benchmark"
echo "========================================"

BASE_URL="http://localhost:8080/HRWebApp"
AUTH_HEADER="Authorization: Basic YWRtaW46YWRtaW4xMjM="

# Test 1: Single user response time
echo "Test 1: Single User Response Time"
echo "--------------------------------"
for i in {1..10}; do
  curl -w "Response time: %{time_total}s\n" -o /dev/null -s \
    -H "$AUTH_HEADER" "$BASE_URL/protected/listAll"
done

# Test 2: Concurrent users
echo -e "\nTest 2: Concurrent Users (10 users, 100 requests each)"
echo "-----------------------------------------------------"
start_time=$(date +%s)
for i in {1..10}; do
  {
    for j in {1..100}; do
      curl -s -H "$AUTH_HEADER" "$BASE_URL/protected/listAll" > /dev/null
    done
  } &
done
wait
end_time=$(date +%s)
duration=$((end_time - start_time))
total_requests=1000
throughput=$((total_requests / duration))
echo "Total time: ${duration}s"
echo "Throughput: ${throughput} requests/second"

# Test 3: Memory usage during load
echo -e "\nTest 3: Memory Usage During Load"
echo "--------------------------------"
pid=$(pgrep -f "java.*HRWebApp")
if [ -n "$pid" ]; then
  echo "Monitoring JVM memory for PID: $pid"
  jstat -gc $pid 5s 3
else
  echo "Tomcat process not found"
fi

echo -e "\nBenchmark completed."
```

## Environment-Specific Tuning

### Development Environment

#### Fast Startup Configuration
```properties
# application-dev.properties
hikari.maximumPoolSize=3
hikari.minimumIdle=1
hikari.connectionTimeout=5000
hikari.registerMbeans=false

# JVM settings for development
-Xms256m -Xmx1g
-XX:TieredStopAtLevel=1
-XX:+UseParallelGC
```

### Production Environment

#### High-Performance Configuration
```properties
# application-prod.properties
hikari.maximumPoolSize=20
hikari.minimumIdle=5
hikari.connectionTimeout=20000
hikari.leakDetectionThreshold=30000
hikari.registerMbeans=true

# Production JVM settings
-Xms4g -Xmx16g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
```

### Cloud Environment Considerations

#### Container Resource Limits
```yaml
# Docker resource limits
resources:
  limits:
    memory: "8Gi"
    cpu: "4"
  requests:
    memory: "4Gi"
    cpu: "2"

# JVM settings for containers
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
-XX:InitialRAMPercentage=50.0
```

## Migration-Specific Optimizations

### Oracle to PostgreSQL Performance Considerations

#### Query Pattern Optimization
```java
// Oracle-style query (less efficient in PostgreSQL)
String oracleStyle = "SELECT * FROM employees WHERE ROWNUM <= 10";

// PostgreSQL-optimized query
String postgresStyle = "SELECT * FROM employees LIMIT 10";

// Use PostgreSQL-specific features
String withCTE = """
    WITH high_earners AS (
        SELECT * FROM employees WHERE salary > 75000
    )
    SELECT he.*, j.job_title 
    FROM high_earners he 
    JOIN jobs j ON he.job_id = j.job_id
    ORDER BY he.salary DESC
    """;
```

#### Data Type Optimization
```java
// Optimize for PostgreSQL data types
public class OptimizedEmployee {
    private Integer employeeId;        // Use Integer for nullable IDs
    private String firstName;          // VARCHAR maps well
    private String lastName;
    private String email;
    private String phoneNumber;
    private String jobId;
    private BigDecimal salary;         // Use BigDecimal for NUMERIC
    private LocalDateTime hireDate;    // Use LocalDateTime for TIMESTAMP
    
    // Optimized for PostgreSQL array types if needed
    private String[] skills;           // Maps to PostgreSQL text[]
}
```

#### Connection Pool Sizing for Migration
```properties
# Conservative settings during migration
hikari.maximumPoolSize=10
hikari.minimumIdle=3
hikari.connectionTimeout=30000

# Gradually increase after migration stabilizes
hikari.maximumPoolSize=20
hikari.minimumIdle=5
hikari.connectionTimeout=20000
```

This comprehensive performance tuning guide provides the foundation for optimizing the HR Web Application after migration from Oracle to OpenJDK + PostgreSQL. Regular monitoring and iterative tuning based on actual usage patterns will help maintain optimal performance in production environments.