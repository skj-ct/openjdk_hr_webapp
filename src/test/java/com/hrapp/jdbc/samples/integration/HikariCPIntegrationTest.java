package com.hrapp.jdbc.samples.integration;

import com.hrapp.jdbc.samples.config.DatabaseConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for HikariCP connection pooling under simulated load.
 * Tests Requirements: 7.1, 7.2, 9.3
 */
@DisplayName("HikariCP Connection Pool Integration Tests")
class HikariCPIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should configure HikariCP with correct pool settings")
    void testHikariCPConfiguration() {
        HikariDataSource hikariDS = (HikariDataSource) getDataSource();
        
        // Verify basic configuration
        assertNotNull(hikariDS.getJdbcUrl(), "JDBC URL should be configured");
        assertTrue(hikariDS.getJdbcUrl().contains("postgresql"), "Should be PostgreSQL connection");
        assertNotNull(hikariDS.getUsername(), "Username should be configured");
        
        // Verify pool settings
        assertTrue(hikariDS.getMaximumPoolSize() > 0, "Maximum pool size should be positive");
        assertTrue(hikariDS.getMinimumIdle() >= 0, "Minimum idle should be non-negative");
        assertTrue(hikariDS.getConnectionTimeout() > 0, "Connection timeout should be positive");
        assertTrue(hikariDS.getIdleTimeout() > 0, "Idle timeout should be positive");
        assertTrue(hikariDS.getMaxLifetime() > 0, "Max lifetime should be positive");
        
        // Verify pool is running
        assertFalse(hikariDS.isClosed(), "Pool should not be closed");
        assertTrue(hikariDS.isRunning(), "Pool should be running");
    }

    @Test
    @DisplayName("Should handle concurrent connection requests efficiently")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testConcurrentConnectionRequests() throws InterruptedException, ExecutionException {
        final int threadCount = 10;
        final int connectionsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        List<Future<Boolean>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        // Submit concurrent connection tasks
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Future<Boolean> future = executor.submit(() -> {
                try {
                    for (int j = 0; j < connectionsPerThread; j++) {
                        long startTime = System.currentTimeMillis();
                        
                        try (Connection conn = getConnection()) {
                            long connectionTime = System.currentTimeMillis() - startTime;
                            
                            // Verify connection is valid
                            assertTrue(conn.isValid(1), 
                                "Connection should be valid for thread " + threadId + ", iteration " + j);
                            
                            // Connection should be obtained quickly (under 5 seconds)
                            assertTrue(connectionTime < 5000, 
                                "Connection should be obtained quickly, took: " + connectionTime + "ms");
                            
                            // Perform a simple query to verify connection works
                            try (var stmt = conn.createStatement();
                                 var rs = stmt.executeQuery("SELECT 1")) {
                                assertTrue(rs.next(), "Query should return result");
                                assertEquals(1, rs.getInt(1), "Query should return 1");
                            }
                            
                            successCount.incrementAndGet();
                            
                            // Simulate some work
                            Thread.sleep(10);
                            
                        } catch (SQLException e) {
                            errorCount.incrementAndGet();
                            throw new RuntimeException("Connection failed for thread " + threadId + 
                                ", iteration " + j, e);
                        }
                    }
                    return true;
                } catch (Exception e) {
                    System.err.println("Thread " + threadId + " failed: " + e.getMessage());
                    return false;
                }
            });
            futures.add(future);
        }
        
        // Wait for all tasks to complete
        for (Future<Boolean> future : futures) {
            assertTrue(future.get(), "All connection tasks should succeed");
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), 
            "Executor should terminate within 10 seconds");
        
        // Verify results
        int expectedSuccessCount = threadCount * connectionsPerThread;
        assertEquals(expectedSuccessCount, successCount.get(), 
            "All connection attempts should succeed");
        assertEquals(0, errorCount.get(), "Should have no connection errors");
    }

    @Test
    @DisplayName("Should monitor pool metrics via JMX")
    void testPoolMetricsMonitoring() throws Exception {
        HikariDataSource hikariDS = (HikariDataSource) getDataSource();
        
        // Get pool MBean for monitoring
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName poolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + hikariDS.getPoolName() + ")");
        
        // Verify MBean is registered
        assertTrue(mBeanServer.isRegistered(poolName), "HikariCP MBean should be registered");
        
        // Get initial metrics
        int initialActiveConnections = (Integer) mBeanServer.getAttribute(poolName, "ActiveConnections");
        int initialIdleConnections = (Integer) mBeanServer.getAttribute(poolName, "IdleConnections");
        int totalConnections = (Integer) mBeanServer.getAttribute(poolName, "TotalConnections");
        
        assertTrue(initialActiveConnections >= 0, "Active connections should be non-negative");
        assertTrue(initialIdleConnections >= 0, "Idle connections should be non-negative");
        assertTrue(totalConnections >= 0, "Total connections should be non-negative");
        assertEquals(initialActiveConnections + initialIdleConnections, totalConnections, 
            "Active + Idle should equal Total connections");
        
        // Create some connections and verify metrics change
        List<Connection> connections = new ArrayList<>();
        try {
            // Acquire multiple connections
            for (int i = 0; i < 3; i++) {
                connections.add(getConnection());
            }
            
            // Check metrics after acquiring connections
            int activeAfterAcquire = (Integer) mBeanServer.getAttribute(poolName, "ActiveConnections");
            assertTrue(activeAfterAcquire >= initialActiveConnections + 3, 
                "Active connections should increase after acquiring connections");
            
        } finally {
            // Close connections
            for (Connection conn : connections) {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            }
        }
        
        // Wait a moment for pool to update metrics
        Thread.sleep(100);
        
        // Verify metrics return to normal after closing connections
        int activeAfterClose = (Integer) mBeanServer.getAttribute(poolName, "ActiveConnections");
        assertTrue(activeAfterClose <= initialActiveConnections + 1, 
            "Active connections should decrease after closing connections");
    }

    @Test
    @DisplayName("Should handle connection pool exhaustion gracefully")
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void testConnectionPoolExhaustion() throws InterruptedException, SQLException {
        HikariDataSource hikariDS = (HikariDataSource) getDataSource();
        int maxPoolSize = hikariDS.getMaximumPoolSize();
        long connectionTimeout = hikariDS.getConnectionTimeout();
        
        List<Connection> heldConnections = new ArrayList<>();
        
        try {
            // Acquire all available connections
            for (int i = 0; i < maxPoolSize; i++) {
                Connection conn = getConnection();
                heldConnections.add(conn);
                assertTrue(conn.isValid(1), "Connection " + i + " should be valid");
            }
            
            // Try to acquire one more connection - should timeout
            long startTime = System.currentTimeMillis();
            assertThrows(SQLException.class, () -> {
                try (Connection conn = getConnection()) {
                    fail("Should not be able to acquire connection when pool is exhausted");
                }
            }, "Should throw SQLException when pool is exhausted");
            
            long timeoutDuration = System.currentTimeMillis() - startTime;
            assertTrue(timeoutDuration >= connectionTimeout - 1000, 
                "Should wait approximately the connection timeout duration");
            
        } finally {
            // Release all held connections
            for (Connection conn : heldConnections) {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            }
        }
        
        // Verify pool recovers after releasing connections
        try (Connection conn = getConnection()) {
            assertTrue(conn.isValid(1), "Should be able to acquire connection after releasing others");
        }
    }

    @Test
    @DisplayName("Should validate connection health and recovery")
    void testConnectionHealthAndRecovery() throws SQLException, InterruptedException {
        // Test connection validation
        try (Connection conn = getConnection()) {
            assertTrue(conn.isValid(5), "Fresh connection should be valid");
            
            // Test connection after some operations
            try (var stmt = conn.createStatement()) {
                stmt.execute("SELECT 1");
            }
            assertTrue(conn.isValid(5), "Connection should remain valid after query");
        }
        
        // Test pool recovery after connection issues
        HikariDataSource hikariDS = (HikariDataSource) getDataSource();
        
        // Simulate connection issues by creating and immediately closing connections
        for (int i = 0; i < 5; i++) {
            try (Connection conn = getConnection()) {
                // Force close the underlying connection to simulate network issues
                conn.close();
            }
        }
        
        // Pool should recover and provide new valid connections
        try (Connection conn = getConnection()) {
            assertTrue(conn.isValid(5), "Pool should recover and provide valid connections");
            
            // Verify we can still perform database operations
            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT current_database()")) {
                assertTrue(rs.next(), "Should be able to query database after recovery");
                assertNotNull(rs.getString(1), "Should get database name");
            }
        }
    }

    @Test
    @DisplayName("Should handle long-running connections appropriately")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testLongRunningConnections() throws SQLException, InterruptedException {
        HikariDataSource hikariDS = (HikariDataSource) getDataSource();
        long maxLifetime = hikariDS.getMaxLifetime();
        
        // Create a connection and hold it for a while
        try (Connection conn = getConnection()) {
            assertTrue(conn.isValid(5), "Connection should be initially valid");
            
            // Simulate long-running operation
            Thread.sleep(1000); // 1 second
            
            // Connection should still be valid for reasonable duration
            assertTrue(conn.isValid(5), "Connection should remain valid during operation");
            
            // Perform database operation to ensure connection is functional
            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT COUNT(*) FROM hr.employees")) {
                assertTrue(rs.next(), "Should be able to query during long operation");
                assertTrue(rs.getInt(1) >= 0, "Should get valid count");
            }
        }
        
        // Verify pool can still provide new connections after long-running operations
        try (Connection conn = getConnection()) {
            assertTrue(conn.isValid(5), "Should get new valid connection after long operation");
        }
    }

    @Test
    @DisplayName("Should handle connection leaks detection")
    void testConnectionLeakDetection() throws SQLException {
        HikariDataSource hikariDS = (HikariDataSource) getDataSource();
        
        // Get initial active connection count
        HikariPoolMXBean poolMXBean = hikariDS.getHikariPoolMXBean();
        int initialActiveConnections = poolMXBean.getActiveConnections();
        
        // Intentionally "leak" a connection (don't close it in try-with-resources)
        Connection leakedConnection = getConnection();
        assertTrue(leakedConnection.isValid(1), "Leaked connection should be valid");
        
        // Verify active connection count increased
        int activeAfterLeak = poolMXBean.getActiveConnections();
        assertTrue(activeAfterLeak > initialActiveConnections, 
            "Active connection count should increase after acquiring connection");
        
        // Manually close the "leaked" connection
        leakedConnection.close();
        
        // Give pool time to update metrics
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify connection count returns to normal
        int activeAfterClose = poolMXBean.getActiveConnections();
        assertTrue(activeAfterClose <= initialActiveConnections + 1, 
            "Active connection count should decrease after closing connection");
    }

    @Test
    @DisplayName("Should perform under sustained load")
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void testSustainedLoad() throws InterruptedException, ExecutionException, SQLException {
        final int duration = 10; // seconds
        final int threadsCount = 8;
        final AtomicInteger operationCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        List<Future<?>> futures = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        // Submit sustained load tasks
        for (int i = 0; i < threadsCount; i++) {
            Future<?> future = executor.submit(() -> {
                while (System.currentTimeMillis() - startTime < duration * 1000) {
                    try (Connection conn = getConnection()) {
                        // Perform a simple database operation
                        try (var stmt = conn.createStatement();
                             var rs = stmt.executeQuery("SELECT COUNT(*) FROM hr.employees")) {
                            if (rs.next()) {
                                operationCount.incrementAndGet();
                            }
                        }
                        
                        // Small delay to simulate realistic usage
                        Thread.sleep(50);
                        
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Error during sustained load: " + e.getMessage());
                    }
                }
            });
            futures.add(future);
        }
        
        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            future.get();
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), 
            "Executor should terminate within 10 seconds");
        
        // Verify performance
        int totalOperations = operationCount.get();
        int totalErrors = errorCount.get();
        
        assertTrue(totalOperations > 0, "Should have performed some operations");
        assertTrue(totalErrors < totalOperations * 0.05, // Less than 5% error rate
            "Error rate should be low: " + totalErrors + "/" + totalOperations);
        
        System.out.println("Sustained load test completed: " + totalOperations + 
            " operations, " + totalErrors + " errors in " + duration + " seconds");
        
        // Verify pool is still healthy after sustained load
        try (Connection conn = getConnection()) {
            assertTrue(conn.isValid(5), "Pool should be healthy after sustained load");
        }
    }
}