package com.hrapp.jdbc.samples.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for database connectivity and schema validation using TestContainers.
 * Tests Requirements: 7.1, 7.2, 9.3
 */
@DisplayName("Database Connectivity Integration Tests")
class DatabaseConnectivityIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should establish PostgreSQL connection successfully")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testDatabaseConnection() throws SQLException {
        try (Connection conn = getConnection()) {
            assertNotNull(conn, "Connection should not be null");
            assertTrue(conn.isValid(5), "Connection should be valid");
            assertFalse(conn.isClosed(), "Connection should not be closed");
            
            // Verify PostgreSQL version
            DatabaseMetaData metaData = conn.getMetaData();
            String productName = metaData.getDatabaseProductName();
            String productVersion = metaData.getDatabaseProductVersion();
            
            assertEquals("PostgreSQL", productName, "Should be connected to PostgreSQL");
            assertTrue(productVersion.startsWith("17"), 
                "Should be using PostgreSQL 17.x, but got: " + productVersion);
        }
    }

    @Test
    @DisplayName("Should validate HR schema exists and is accessible")
    void testSchemaValidation() throws SQLException {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Check if HR schema exists
            try (ResultSet schemas = metaData.getSchemas()) {
                boolean hrSchemaFound = false;
                while (schemas.next()) {
                    String schemaName = schemas.getString("TABLE_SCHEM");
                    if ("hr".equals(schemaName)) {
                        hrSchemaFound = true;
                        break;
                    }
                }
                assertTrue(hrSchemaFound, "HR schema should exist");
            }
        }
    }

    @Test
    @DisplayName("Should validate employees table structure")
    void testEmployeesTableStructure() throws SQLException {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Check if employees table exists
            try (ResultSet tables = metaData.getTables(null, "hr", "employees", new String[]{"TABLE"})) {
                assertTrue(tables.next(), "Employees table should exist in HR schema");
            }
            
            // Validate table columns
            try (ResultSet columns = metaData.getColumns(null, "hr", "employees", null)) {
                boolean hasEmployeeId = false;
                boolean hasFirstName = false;
                boolean hasLastName = false;
                boolean hasEmail = false;
                boolean hasPhoneNumber = false;
                boolean hasJobId = false;
                boolean hasSalary = false;
                
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String dataType = columns.getString("TYPE_NAME");
                    
                    switch (columnName) {
                        case "employee_id":
                            hasEmployeeId = true;
                            assertEquals("serial", dataType.toLowerCase(), 
                                "employee_id should be SERIAL type");
                            break;
                        case "first_name":
                            hasFirstName = true;
                            assertEquals("varchar", dataType.toLowerCase(), 
                                "first_name should be VARCHAR type");
                            break;
                        case "last_name":
                            hasLastName = true;
                            assertEquals("varchar", dataType.toLowerCase(), 
                                "last_name should be VARCHAR type");
                            break;
                        case "email":
                            hasEmail = true;
                            assertEquals("varchar", dataType.toLowerCase(), 
                                "email should be VARCHAR type");
                            break;
                        case "phone_number":
                            hasPhoneNumber = true;
                            assertEquals("varchar", dataType.toLowerCase(), 
                                "phone_number should be VARCHAR type");
                            break;
                        case "job_id":
                            hasJobId = true;
                            assertEquals("varchar", dataType.toLowerCase(), 
                                "job_id should be VARCHAR type");
                            break;
                        case "salary":
                            hasSalary = true;
                            assertEquals("numeric", dataType.toLowerCase(), 
                                "salary should be NUMERIC type");
                            break;
                    }
                }
                
                assertTrue(hasEmployeeId, "Table should have employee_id column");
                assertTrue(hasFirstName, "Table should have first_name column");
                assertTrue(hasLastName, "Table should have last_name column");
                assertTrue(hasEmail, "Table should have email column");
                assertTrue(hasPhoneNumber, "Table should have phone_number column");
                assertTrue(hasJobId, "Table should have job_id column");
                assertTrue(hasSalary, "Table should have salary column");
            }
        }
    }

    @Test
    @DisplayName("Should validate primary key and constraints")
    void testTableConstraints() throws SQLException {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Check primary key
            try (ResultSet primaryKeys = metaData.getPrimaryKeys(null, "hr", "employees")) {
                assertTrue(primaryKeys.next(), "Employees table should have a primary key");
                assertEquals("employee_id", primaryKeys.getString("COLUMN_NAME"), 
                    "Primary key should be employee_id");
            }
            
            // Check unique constraints (email should be unique)
            try (ResultSet indexes = metaData.getIndexInfo(null, "hr", "employees", true, false)) {
                boolean hasEmailUniqueIndex = false;
                while (indexes.next()) {
                    String columnName = indexes.getString("COLUMN_NAME");
                    if ("email".equals(columnName)) {
                        hasEmailUniqueIndex = true;
                        break;
                    }
                }
                assertTrue(hasEmailUniqueIndex, "Email column should have unique constraint");
            }
        }
    }

    @Test
    @DisplayName("Should validate salary increment function exists")
    void testSalaryIncrementFunction() throws SQLException {
        try (Connection conn = getConnection()) {
            // Check if the salary increment function exists
            String checkFunctionSql = """
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.routines 
                    WHERE routine_schema = 'hr' 
                    AND routine_name = 'increment_salary_function'
                    AND routine_type = 'FUNCTION'
                )
                """;
            
            try (var stmt = conn.prepareStatement(checkFunctionSql);
                 var rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Query should return a result");
                assertTrue(rs.getBoolean(1), "Salary increment function should exist");
            }
        }
    }

    @Test
    @DisplayName("Should validate Flyway migration history")
    void testFlywayMigrationHistory() throws SQLException {
        try (Connection conn = getConnection()) {
            // Check if flyway_schema_history table exists
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet tables = metaData.getTables(null, "hr", "flyway_schema_history", new String[]{"TABLE"})) {
                assertTrue(tables.next(), "Flyway schema history table should exist");
            }
            
            // Verify migrations were applied
            String migrationSql = "SELECT version, description, success FROM hr.flyway_schema_history ORDER BY installed_rank";
            try (var stmt = conn.prepareStatement(migrationSql);
                 var rs = stmt.executeQuery()) {
                
                // Should have at least 3 migrations (V1, V2, V3)
                int migrationCount = 0;
                while (rs.next()) {
                    migrationCount++;
                    assertTrue(rs.getBoolean("success"), 
                        "Migration " + rs.getString("version") + " should be successful");
                }
                
                assertTrue(migrationCount >= 3, 
                    "Should have at least 3 migrations, but found: " + migrationCount);
            }
        }
    }

    @Test
    @DisplayName("Should handle connection timeouts gracefully")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testConnectionTimeout() throws SQLException {
        // Test that connections can be obtained within reasonable time
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = getConnection()) {
            long connectionTime = System.currentTimeMillis() - startTime;
            assertTrue(connectionTime < 5000, 
                "Connection should be established within 5 seconds, took: " + connectionTime + "ms");
            
            assertTrue(conn.isValid(1), "Connection should be valid");
        }
    }

    @Test
    @DisplayName("Should support concurrent connections")
    void testConcurrentConnections() throws SQLException {
        // Test multiple concurrent connections
        Connection[] connections = new Connection[5];
        
        try {
            // Open multiple connections
            for (int i = 0; i < connections.length; i++) {
                connections[i] = getConnection();
                assertTrue(connections[i].isValid(1), 
                    "Connection " + i + " should be valid");
            }
            
            // Verify all connections are independent
            for (int i = 0; i < connections.length; i++) {
                try (var stmt = connections[i].createStatement();
                     var rs = stmt.executeQuery("SELECT current_database()")) {
                    assertTrue(rs.next(), "Query should return result on connection " + i);
                    assertEquals("hrdb_test", rs.getString(1), 
                        "Should be connected to test database");
                }
            }
            
        } finally {
            // Clean up connections
            for (Connection conn : connections) {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            }
        }
    }
}