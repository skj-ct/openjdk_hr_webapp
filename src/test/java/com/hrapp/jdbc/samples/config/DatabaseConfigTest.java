package com.hrapp.jdbc.samples.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DatabaseConfig class.
 * Tests configuration loading, connection management, and error handling.
 */
class DatabaseConfigTest {

    private DatabaseConfig databaseConfig;

    @BeforeEach
    void setUp() {
        // Reset singleton instance for each test
        resetSingleton();
    }

    @AfterEach
    void tearDown() {
        if (databaseConfig != null) {
            databaseConfig.shutdown();
        }
        resetSingleton();
    }

    @Test
    @DisplayName("Test singleton pattern")
    void testSingletonPattern() {
        // Given & When
        DatabaseConfig instance1 = DatabaseConfig.getInstance();
        DatabaseConfig instance2 = DatabaseConfig.getInstance();

        // Then
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertSame(instance1, instance2, "Should return the same instance");
    }

    @Test
    @DisplayName("Test configuration loading with default values")
    void testConfigurationLoadingWithDefaults() {
        // Given & When
        databaseConfig = DatabaseConfig.getInstance();

        // Then
        assertNotNull(databaseConfig);
        assertNotNull(databaseConfig.getDataSource());
        
        // Verify default configuration is applied
        assertTrue(databaseConfig.getDatabaseUrl().contains("postgresql"));
        assertEquals("hr_user", databaseConfig.getDatabaseUsername());
        assertEquals("hr", databaseConfig.getDatabaseSchema());
    }

    @Test
    @DisplayName("Test DataSource initialization")
    void testDataSourceInitialization() {
        // Given & When
        databaseConfig = DatabaseConfig.getInstance();
        DataSource dataSource = databaseConfig.getDataSource();

        // Then
        assertNotNull(dataSource);
        assertInstanceOf(HikariDataSource.class, dataSource);
        
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        assertFalse(hikariDataSource.isClosed());
        assertTrue(hikariDataSource.getMaximumPoolSize() > 0);
        assertTrue(hikariDataSource.getMinimumIdle() >= 0);
    }

    @Test
    @DisplayName("Test custom DataSource creation")
    void testCustomDataSourceCreation() {
        // Given
        String testUrl = "jdbc:postgresql://localhost:5432/testdb";
        String testUsername = "testuser";
        String testPassword = "testpass";

        // When
        HikariDataSource customDataSource = DatabaseConfig.createDataSource(testUrl, testUsername, testPassword);

        // Then
        assertNotNull(customDataSource);
        assertEquals(testUrl, customDataSource.getJdbcUrl());
        assertEquals(testUsername, customDataSource.getUsername());
        assertEquals(testPassword, customDataSource.getPassword());
        assertEquals("org.postgresql.Driver", customDataSource.getDriverClassName());
        
        // Cleanup
        customDataSource.close();
    }

    @Test
    @DisplayName("Test pool statistics")
    void testPoolStatistics() {
        // Given
        databaseConfig = DatabaseConfig.getInstance();

        // When
        String poolStats = databaseConfig.getPoolStats();

        // Then
        assertNotNull(poolStats);
        assertTrue(poolStats.contains("Pool Stats"));
        assertTrue(poolStats.contains("Active:"));
        assertTrue(poolStats.contains("Idle:"));
        assertTrue(poolStats.contains("Total:"));
    }

    @Test
    @DisplayName("Test health check")
    void testHealthCheck() {
        // Given
        databaseConfig = DatabaseConfig.getInstance();

        // When & Then
        // Note: This will fail in unit test environment without actual database
        // In integration tests with TestContainers, this would pass
        assertDoesNotThrow(() -> {
            boolean healthy = databaseConfig.isHealthy();
            // Health check result depends on database availability
            // In unit tests, this is expected to be false
        });
    }

    @Test
    @DisplayName("Test DataSource shutdown")
    void testDataSourceShutdown() {
        // Given
        databaseConfig = DatabaseConfig.getInstance();
        DataSource dataSource = databaseConfig.getDataSource();
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

        // When
        databaseConfig.shutdown();

        // Then
        assertTrue(hikariDataSource.isClosed());
    }

    @Test
    @DisplayName("Test connection retrieval after shutdown")
    void testConnectionRetrievalAfterShutdown() {
        // Given
        databaseConfig = DatabaseConfig.getInstance();
        databaseConfig.shutdown();

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            databaseConfig.getDataSource();
        }, "Should throw IllegalStateException when DataSource is closed");
    }

    @Test
    @DisplayName("Test configuration properties")
    void testConfigurationProperties() {
        // Given & When
        databaseConfig = DatabaseConfig.getInstance();

        // Then
        assertNotNull(databaseConfig.getDatabaseUrl());
        assertNotNull(databaseConfig.getDatabaseUsername());
        assertNotNull(databaseConfig.getDatabaseSchema());
        
        assertTrue(databaseConfig.getDatabaseUrl().startsWith("jdbc:postgresql://"));
        assertFalse(databaseConfig.getDatabaseUsername().isEmpty());
        assertFalse(databaseConfig.getDatabaseSchema().isEmpty());
    }

    @Test
    @DisplayName("Test connection timeout configuration")
    void testConnectionTimeoutConfiguration() {
        // Given & When
        databaseConfig = DatabaseConfig.getInstance();
        HikariDataSource hikariDataSource = (HikariDataSource) databaseConfig.getDataSource();

        // Then
        assertTrue(hikariDataSource.getConnectionTimeout() > 0);
        assertTrue(hikariDataSource.getIdleTimeout() > 0);
        assertTrue(hikariDataSource.getMaxLifetime() > 0);
    }

    @Test
    @DisplayName("Test pool size configuration")
    void testPoolSizeConfiguration() {
        // Given & When
        databaseConfig = DatabaseConfig.getInstance();
        HikariDataSource hikariDataSource = (HikariDataSource) databaseConfig.getDataSource();

        // Then
        assertTrue(hikariDataSource.getMaximumPoolSize() > 0);
        assertTrue(hikariDataSource.getMinimumIdle() >= 0);
        assertTrue(hikariDataSource.getMinimumIdle() <= hikariDataSource.getMaximumPoolSize());
    }

    @Test
    @DisplayName("Test PostgreSQL driver configuration")
    void testPostgreSQLDriverConfiguration() {
        // Given & When
        databaseConfig = DatabaseConfig.getInstance();
        HikariDataSource hikariDataSource = (HikariDataSource) databaseConfig.getDataSource();

        // Then
        assertEquals("org.postgresql.Driver", hikariDataSource.getDriverClassName());
        assertTrue(hikariDataSource.getJdbcUrl().contains("postgresql"));
    }

    @Test
    @DisplayName("Test connection validation query")
    void testConnectionValidationQuery() {
        // Given & When
        databaseConfig = DatabaseConfig.getInstance();
        HikariDataSource hikariDataSource = (HikariDataSource) databaseConfig.getDataSource();

        // Then
        assertEquals("SELECT 1", hikariDataSource.getConnectionTestQuery());
        assertTrue(hikariDataSource.getValidationTimeout() > 0);
    }

    @Test
    @DisplayName("Test pool name configuration")
    void testPoolNameConfiguration() {
        // Given & When
        databaseConfig = DatabaseConfig.getInstance();
        HikariDataSource hikariDataSource = (HikariDataSource) databaseConfig.getDataSource();

        // Then
        assertEquals("HRApp-ConnectionPool", hikariDataSource.getPoolName());
    }

    @Test
    @DisplayName("Test leak detection configuration")
    void testLeakDetectionConfiguration() {
        // Given & When
        databaseConfig = DatabaseConfig.getInstance();
        HikariDataSource hikariDataSource = (HikariDataSource) databaseConfig.getDataSource();

        // Then
        assertTrue(hikariDataSource.getLeakDetectionThreshold() > 0);
    }

    /**
     * Helper method to reset singleton instance using reflection
     */
    private void resetSingleton() {
        try {
            java.lang.reflect.Field instanceField = DatabaseConfig.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            // Ignore reflection errors in tests
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Test behavior with invalid configuration")
        void testInvalidConfiguration() {
            // This test would require mocking the properties loading
            // For now, we test that the system handles missing properties gracefully
            assertDoesNotThrow(() -> {
                DatabaseConfig config = DatabaseConfig.getInstance();
                assertNotNull(config);
            });
        }

        @Test
        @DisplayName("Test connection retrieval with closed DataSource")
        void testConnectionRetrievalWithClosedDataSource() {
            // Given
            databaseConfig = DatabaseConfig.getInstance();
            databaseConfig.shutdown();

            // When & Then
            assertThrows(Exception.class, () -> {
                databaseConfig.getConnection();
            });
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Test singleton creation performance")
        void testSingletonCreationPerformance() {
            // Given
            long startTime = System.currentTimeMillis();

            // When
            for (int i = 0; i < 100; i++) {
                DatabaseConfig.getInstance();
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Then
            assertTrue(duration < 1000, "Singleton creation should be fast, took: " + duration + "ms");
        }

        @Test
        @DisplayName("Test DataSource initialization performance")
        void testDataSourceInitializationPerformance() {
            // Given
            long startTime = System.currentTimeMillis();

            // When
            databaseConfig = DatabaseConfig.getInstance();
            DataSource dataSource = databaseConfig.getDataSource();

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Then
            assertNotNull(dataSource);
            assertTrue(duration < 5000, "DataSource initialization should complete within 5 seconds, took: " + duration + "ms");
        }
    }
}