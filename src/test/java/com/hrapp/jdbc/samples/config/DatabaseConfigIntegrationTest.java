package com.hrapp.jdbc.samples.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DatabaseConfig class using TestContainers
 * 
 * These tests verify the database configuration works with a real PostgreSQL database.
 */
@Testcontainers
@DisplayName("DatabaseConfig Integration Tests")
class DatabaseConfigIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("hrdb_test")
            .withUsername("hr_test_user")
            .withPassword("hr_test_password")
            .withInitScript("test-init.sql");
    
    @BeforeAll
    static void setUpContainer() {
        postgres.start();
        
        // Set environment variables for the test
        System.setProperty("DB_URL", postgres.getJdbcUrl());
        System.setProperty("DB_USERNAME", postgres.getUsername());
        System.setProperty("DB_PASSWORD", postgres.getPassword());
        System.setProperty("DB_SCHEMA", "hr");
    }
    
    @AfterAll
    static void tearDownContainer() {
        postgres.stop();
        
        // Clean up system properties
        System.clearProperty("DB_URL");
        System.clearProperty("DB_USERNAME");
        System.clearProperty("DB_PASSWORD");
        System.clearProperty("DB_SCHEMA");
    }
    
    @BeforeEach
    void setUp() {
        // Reset singleton instance for each test
        resetSingleton();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after each test
        try {
            DatabaseConfig config = DatabaseConfig.getInstance();
            config.shutdown();
        } catch (Exception e) {
            // Ignore cleanup errors in tests
        }
        resetSingleton();
    }
    
    /**
     * Reset the singleton instance using reflection for testing
     */
    private void resetSingleton() {
        try {
            java.lang.reflect.Field instance = DatabaseConfig.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            // Ignore reflection errors in tests
        }
    }
    
    @Test
    @DisplayName("Should connect to PostgreSQL database successfully")
    void testDatabaseConnection() throws SQLException {
        // Given
        DatabaseConfig config = DatabaseConfig.getInstance();
        
        // When
        try (Connection connection = config.getConnection()) {
            // Then
            assertNotNull(connection);
            assertFalse(connection.isClosed());
            assertTrue(connection.isValid(5));
        }
    }
    
    @Test
    @DisplayName("Should execute queries successfully")
    void testQueryExecution() throws SQLException {
        // Given
        DatabaseConfig config = DatabaseConfig.getInstance();
        
        // When & Then
        try (Connection connection = config.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Test simple query
            ResultSet resultSet = statement.executeQuery("SELECT 1 as test_value");
            assertTrue(resultSet.next());
            assertEquals(1, resultSet.getInt("test_value"));
        }
    }
    
    @Test
    @DisplayName("Should handle prepared statements correctly")
    void testPreparedStatements() throws SQLException {
        // Given
        DatabaseConfig config = DatabaseConfig.getInstance();
        
        // When & Then
        try (Connection connection = config.getConnection()) {
            // Create a test table
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS test_table (id SERIAL PRIMARY KEY, name VARCHAR(50))");
            }
            
            // Insert data using prepared statement
            try (PreparedStatement insertStmt = connection.prepareStatement(
                    "INSERT INTO test_table (name) VALUES (?) RETURNING id")) {
                insertStmt.setString(1, "Test Name");
                ResultSet resultSet = insertStmt.executeQuery();
                assertTrue(resultSet.next());
                int generatedId = resultSet.getInt("id");
                assertTrue(generatedId > 0);
            }
            
            // Query data using prepared statement
            try (PreparedStatement selectStmt = connection.prepareStatement(
                    "SELECT name FROM test_table WHERE id = ?")) {
                selectStmt.setInt(1, 1);
                ResultSet resultSet = selectStmt.executeQuery();
                assertTrue(resultSet.next());
                assertEquals("Test Name", resultSet.getString("name"));
            }
            
            // Clean up
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP TABLE test_table");
            }
        }
    }
    
    @Test
    @DisplayName("Should report healthy status with real database")
    void testHealthCheckWithRealDatabase() {
        // Given
        DatabaseConfig config = DatabaseConfig.getInstance();
        
        // When
        boolean healthy = config.isHealthy();
        DatabaseConfig.HealthInfo healthInfo = config.getHealthInfo();
        
        // Then
        assertTrue(healthy, "Database should be healthy with TestContainers");
        assertNotNull(healthInfo);
        assertTrue(healthInfo.isHealthy());
        assertEquals("Healthy", healthInfo.getStatus());
        assertNotNull(healthInfo.getPoolStats());
    }
    
    @Test
    @DisplayName("Should provide accurate pool statistics")
    void testPoolStatistics() throws SQLException {
        // Given
        DatabaseConfig config = DatabaseConfig.getInstance();
        
        // When
        DatabaseConfig.HealthInfo healthInfo = config.getHealthInfo();
        
        // Then
        assertNotNull(healthInfo.getPoolStats());
        DatabaseConfig.PoolStats stats = healthInfo.getPoolStats();
        
        // Pool should have some connections
        assertTrue(stats.getTotalConnections() >= 0);
        assertTrue(stats.getActiveConnections() >= 0);
        assertTrue(stats.getIdleConnections() >= 0);
        assertTrue(stats.getThreadsAwaitingConnection() >= 0);
        
        // Total should be sum of active and idle
        assertEquals(stats.getTotalConnections(), 
                    stats.getActiveConnections() + stats.getIdleConnections());
    }
    
    @Test
    @DisplayName("Should handle multiple concurrent connections")
    void testConcurrentConnections() throws InterruptedException {
        // Given
        DatabaseConfig config = DatabaseConfig.getInstance();
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];
        
        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try (Connection connection = config.getConnection();
                     Statement statement = connection.createStatement()) {
                    
                    ResultSet resultSet = statement.executeQuery("SELECT " + (index + 1) + " as thread_id");
                    results[index] = resultSet.next() && resultSet.getInt("thread_id") == (index + 1);
                    
                } catch (SQLException e) {
                    results[index] = false;
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then
        for (int i = 0; i < threadCount; i++) {
            assertTrue(results[i], "Thread " + i + " should successfully execute query");
        }
    }
    
    @Test
    @DisplayName("Should handle connection pool exhaustion gracefully")
    void testConnectionPoolExhaustion() throws InterruptedException {
        // Given
        DatabaseConfig config = DatabaseConfig.getInstance();
        int maxConnections = 10; // Based on default pool size
        Connection[] connections = new Connection[maxConnections + 2];
        
        try {
            // When - Try to exhaust the connection pool
            for (int i = 0; i < maxConnections; i++) {
                connections[i] = config.getConnection();
                assertNotNull(connections[i]);
            }
            
            // Try to get one more connection (should timeout or fail gracefully)
            long startTime = System.currentTimeMillis();
            try {
                connections[maxConnections] = config.getConnection();
                // If we get here, the pool might have grown or connection was available
                assertNotNull(connections[maxConnections]);
            } catch (SQLException e) {
                // This is expected if pool is exhausted
                long elapsedTime = System.currentTimeMillis() - startTime;
                assertTrue(elapsedTime >= 1000, "Should timeout after reasonable time");
            }
            
        } finally {
            // Clean up all connections
            for (Connection connection : connections) {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        // Ignore cleanup errors
                    }
                }
            }
        }
    }
    
    @Test
    @DisplayName("Should validate connection configuration")
    void testConnectionConfiguration() throws SQLException {
        // Given
        DatabaseConfig config = DatabaseConfig.getInstance();
        
        // When
        try (Connection connection = config.getConnection()) {
            // Then - Verify connection properties
            assertFalse(connection.getAutoCommit(), "Auto-commit should be disabled by default");
            
            // Verify we can set schema
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET search_path TO hr, public");
                
                // Verify the schema setting worked
                ResultSet resultSet = statement.executeQuery("SHOW search_path");
                assertTrue(resultSet.next());
                String searchPath = resultSet.getString(1);
                assertTrue(searchPath.contains("hr"));
            }
        }
    }
}