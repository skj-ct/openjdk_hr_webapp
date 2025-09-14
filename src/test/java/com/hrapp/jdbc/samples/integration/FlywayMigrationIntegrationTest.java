package com.hrapp.jdbc.samples.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Flyway migrations in test environment.
 * Tests Requirements: 7.1, 7.2, 9.3
 */
@DisplayName("Flyway Migration Integration Tests")
class FlywayMigrationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should apply all migrations successfully")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testMigrationsAppliedSuccessfully() {
        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] migrations = infoService.all();
        
        assertTrue(migrations.length >= 3, "Should have at least 3 migrations (V1, V2, V3)");
        
        for (MigrationInfo migration : migrations) {
            assertEquals(MigrationState.SUCCESS, migration.getState(), 
                "Migration " + migration.getVersion() + " should be successful");
            assertNotNull(migration.getInstalledOn(), 
                "Migration " + migration.getVersion() + " should have installation timestamp");
        }
    }

    @Test
    @DisplayName("Should validate V1 migration - Create employees table")
    void testV1Migration() throws SQLException {
        try (Connection conn = getConnection()) {
            // Verify employees table exists with correct structure
            String tableExistsSql = """
                SELECT EXISTS (
                    SELECT FROM information_schema.tables 
                    WHERE table_schema = 'hr' 
                    AND table_name = 'employees'
                )
                """;
            
            try (var stmt = conn.prepareStatement(tableExistsSql);
                 var rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertTrue(rs.getBoolean(1), "Employees table should exist after V1 migration");
            }
            
            // Verify table structure
            String columnsSql = """
                SELECT column_name, data_type, is_nullable, column_default
                FROM information_schema.columns 
                WHERE table_schema = 'hr' AND table_name = 'employees'
                ORDER BY ordinal_position
                """;
            
            try (var stmt = conn.prepareStatement(columnsSql);
                 var rs = stmt.executeQuery()) {
                
                // Verify expected columns exist
                boolean hasEmployeeId = false;
                boolean hasFirstName = false;
                boolean hasLastName = false;
                boolean hasEmail = false;
                boolean hasPhoneNumber = false;
                boolean hasJobId = false;
                boolean hasSalary = false;
                
                while (rs.next()) {
                    String columnName = rs.getString("column_name");
                    String dataType = rs.getString("data_type");
                    String isNullable = rs.getString("is_nullable");
                    
                    switch (columnName) {
                        case "employee_id":
                            hasEmployeeId = true;
                            assertEquals("integer", dataType);
                            assertEquals("NO", isNullable);
                            break;
                        case "first_name":
                            hasFirstName = true;
                            assertEquals("character varying", dataType);
                            assertEquals("NO", isNullable);
                            break;
                        case "last_name":
                            hasLastName = true;
                            assertEquals("character varying", dataType);
                            assertEquals("NO", isNullable);
                            break;
                        case "email":
                            hasEmail = true;
                            assertEquals("character varying", dataType);
                            assertEquals("NO", isNullable);
                            break;
                        case "phone_number":
                            hasPhoneNumber = true;
                            assertEquals("character varying", dataType);
                            assertEquals("YES", isNullable);
                            break;
                        case "job_id":
                            hasJobId = true;
                            assertEquals("character varying", dataType);
                            assertEquals("NO", isNullable);
                            break;
                        case "salary":
                            hasSalary = true;
                            assertEquals("numeric", dataType);
                            assertEquals("NO", isNullable);
                            break;
                    }
                }
                
                assertTrue(hasEmployeeId, "Should have employee_id column");
                assertTrue(hasFirstName, "Should have first_name column");
                assertTrue(hasLastName, "Should have last_name column");
                assertTrue(hasEmail, "Should have email column");
                assertTrue(hasPhoneNumber, "Should have phone_number column");
                assertTrue(hasJobId, "Should have job_id column");
                assertTrue(hasSalary, "Should have salary column");
            }
        }
    }

    @Test
    @DisplayName("Should validate V2 migration - Insert sample data")
    void testV2Migration() throws SQLException {
        try (Connection conn = getConnection()) {
            // Verify sample data was inserted
            String countSql = "SELECT COUNT(*) FROM hr.employees";
            try (var stmt = conn.prepareStatement(countSql);
                 var rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                int employeeCount = rs.getInt(1);
                assertTrue(employeeCount >= 5, 
                    "Should have at least 5 sample employees after V2 migration, found: " + employeeCount);
            }
            
            // Verify specific sample data
            String sampleDataSql = """
                SELECT first_name, last_name, email, job_id, salary 
                FROM hr.employees 
                WHERE email IN ('john.doe@anyco.com', 'jane.smith@anyco.com')
                ORDER BY email
                """;
            
            try (var stmt = conn.prepareStatement(sampleDataSql);
                 var rs = stmt.executeQuery()) {
                
                // Check Jane Smith
                assertTrue(rs.next(), "Should find Jane Smith");
                assertEquals("Jane", rs.getString("first_name"));
                assertEquals("Smith", rs.getString("last_name"));
                assertEquals("jane.smith@anyco.com", rs.getString("email"));
                assertEquals("HR_REP", rs.getString("job_id"));
                assertTrue(rs.getBigDecimal("salary").compareTo(java.math.BigDecimal.ZERO) > 0);
                
                // Check John Doe
                assertTrue(rs.next(), "Should find John Doe");
                assertEquals("John", rs.getString("first_name"));
                assertEquals("Doe", rs.getString("last_name"));
                assertEquals("john.doe@anyco.com", rs.getString("email"));
                assertEquals("IT_PROG", rs.getString("job_id"));
                assertTrue(rs.getBigDecimal("salary").compareTo(java.math.BigDecimal.ZERO) > 0);
            }
        }
    }

    @Test
    @DisplayName("Should validate V3 migration - Create salary increment function")
    void testV3Migration() throws SQLException {
        try (Connection conn = getConnection()) {
            // Verify function exists
            String functionExistsSql = """
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.routines 
                    WHERE routine_schema = 'hr' 
                    AND routine_name = 'increment_salary_function'
                    AND routine_type = 'FUNCTION'
                )
                """;
            
            try (var stmt = conn.prepareStatement(functionExistsSql);
                 var rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertTrue(rs.getBoolean(1), "Salary increment function should exist after V3 migration");
            }
            
            // Test function functionality
            String testFunctionSql = "SELECT * FROM hr.increment_salary_function(10)";
            try (var stmt = conn.prepareStatement(testFunctionSql);
                 var rs = stmt.executeQuery()) {
                
                assertTrue(rs.next(), "Function should return at least one employee");
                
                // Verify function returns expected columns
                assertDoesNotThrow(() -> rs.getInt("employee_id"));
                assertDoesNotThrow(() -> rs.getString("first_name"));
                assertDoesNotThrow(() -> rs.getString("last_name"));
                assertDoesNotThrow(() -> rs.getString("email"));
                assertDoesNotThrow(() -> rs.getString("phone_number"));
                assertDoesNotThrow(() -> rs.getString("job_id"));
                assertDoesNotThrow(() -> rs.getBigDecimal("salary"));
            }
        }
    }

    @Test
    @DisplayName("Should handle migration rollback scenarios")
    void testMigrationRollback() {
        // Create a separate Flyway instance for rollback testing
        Flyway testFlyway = Flyway.configure()
                .dataSource(getDataSource())
                .locations("classpath:db/migration")
                .schemas("hr_rollback_test")
                .createSchemas(true)
                .load();
        
        // Apply migrations
        testFlyway.migrate();
        
        // Verify migrations applied
        MigrationInfoService infoService = testFlyway.info();
        MigrationInfo[] migrations = infoService.all();
        assertTrue(migrations.length >= 3, "Should have migrations applied");
        
        // Clean (rollback all migrations)
        testFlyway.clean();
        
        // Verify schema is clean
        MigrationInfoService cleanInfoService = testFlyway.info();
        MigrationInfo[] cleanMigrations = cleanInfoService.all();
        for (MigrationInfo migration : cleanMigrations) {
            assertEquals(MigrationState.PENDING, migration.getState(), 
                "Migration should be pending after clean");
        }
        
        // Re-apply migrations to verify repeatability
        testFlyway.migrate();
        
        MigrationInfoService reapplyInfoService = testFlyway.info();
        MigrationInfo[] reapplyMigrations = reapplyInfoService.all();
        for (MigrationInfo migration : reapplyMigrations) {
            assertEquals(MigrationState.SUCCESS, migration.getState(), 
                "Migration should be successful after re-application");
        }
    }

    @Test
    @DisplayName("Should validate migration checksums and versions")
    void testMigrationChecksums() {
        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] migrations = infoService.all();
        
        for (MigrationInfo migration : migrations) {
            assertNotNull(migration.getVersion(), "Migration should have version");
            assertNotNull(migration.getDescription(), "Migration should have description");
            assertNotNull(migration.getChecksum(), "Migration should have checksum");
            
            // Verify version format (should be like 1, 2, 3, etc.)
            String version = migration.getVersion().toString();
            assertTrue(version.matches("\\d+(\\.\\d+)*"), 
                "Version should be numeric: " + version);
        }
        
        // Verify specific expected migrations
        boolean hasV1 = false;
        boolean hasV2 = false;
        boolean hasV3 = false;
        
        for (MigrationInfo migration : migrations) {
            String version = migration.getVersion().toString();
            switch (version) {
                case "1":
                    hasV1 = true;
                    assertTrue(migration.getDescription().toLowerCase().contains("employees"), 
                        "V1 should be about creating employees table");
                    break;
                case "2":
                    hasV2 = true;
                    assertTrue(migration.getDescription().toLowerCase().contains("sample") || 
                              migration.getDescription().toLowerCase().contains("data"), 
                        "V2 should be about inserting sample data");
                    break;
                case "3":
                    hasV3 = true;
                    assertTrue(migration.getDescription().toLowerCase().contains("salary") || 
                              migration.getDescription().toLowerCase().contains("function"), 
                        "V3 should be about salary increment function");
                    break;
            }
        }
        
        assertTrue(hasV1, "Should have V1 migration");
        assertTrue(hasV2, "Should have V2 migration");
        assertTrue(hasV3, "Should have V3 migration");
    }

    @Test
    @DisplayName("Should handle concurrent migration attempts")
    void testConcurrentMigrations() throws InterruptedException {
        final int threadCount = 3;
        Thread[] threads = new Thread[threadCount];
        final Exception[] exceptions = new Exception[threadCount];
        
        // Create multiple Flyway instances that try to migrate concurrently
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    Flyway concurrentFlyway = Flyway.configure()
                            .dataSource(getDataSource())
                            .locations("classpath:db/migration")
                            .schemas("hr_concurrent_test_" + threadId)
                            .createSchemas(true)
                            .load();
                    
                    // This should work without conflicts since each uses different schema
                    concurrentFlyway.migrate();
                    
                    // Verify migration success
                    MigrationInfoService infoService = concurrentFlyway.info();
                    MigrationInfo[] migrations = infoService.all();
                    for (MigrationInfo migration : migrations) {
                        if (migration.getState() != MigrationState.SUCCESS) {
                            throw new RuntimeException("Migration failed: " + migration.getVersion());
                        }
                    }
                    
                } catch (Exception e) {
                    exceptions[threadId] = e;
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for completion
        for (Thread thread : threads) {
            thread.join(30000); // 30 second timeout
        }
        
        // Check for exceptions
        for (int i = 0; i < threadCount; i++) {
            if (exceptions[i] != null) {
                fail("Concurrent migration thread " + i + " failed: " + exceptions[i].getMessage());
            }
        }
    }
}