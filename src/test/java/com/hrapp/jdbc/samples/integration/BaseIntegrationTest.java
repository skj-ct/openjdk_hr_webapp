package com.hrapp.jdbc.samples.integration;

import com.hrapp.jdbc.samples.config.ConnectionFactory;
import com.hrapp.jdbc.samples.config.DatabaseConfig;
import com.hrapp.jdbc.samples.entity.Employee;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for integration tests using TestContainers.
 * Provides common setup for PostgreSQL database testing.
 */
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("hrdb_test")
            .withUsername("hr_test")
            .withPassword("hr_test_password")
            .withInitScript("test-schema.sql");

    protected static DataSource dataSource;
    protected static Flyway flyway;

    @BeforeAll
    static void setUpDatabase() {
        postgres.start();
        
        // Create test data source
        dataSource = DatabaseConfig.createDataSource(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword()
        );

        // Set up Flyway for migrations
        flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();

        // Run migrations
        flyway.migrate();
    }

    @AfterAll
    static void tearDownDatabase() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @BeforeEach
    void cleanDatabase() {
        // Clean and re-migrate for each test
        flyway.clean();
        flyway.migrate();
    }

    /**
     * Get a database connection for testing
     */
    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Get the test data source
     */
    protected DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Insert test employees into the database
     */
    protected void insertTestEmployees() throws SQLException {
        List<Employee> testEmployees = createTestEmployees();
        
        String sql = "INSERT INTO employees (employee_id, first_name, last_name, email, phone_number, job_id, salary) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (Employee emp : testEmployees) {
                stmt.setInt(1, emp.getEmployeeId());
                stmt.setString(2, emp.getFirstName());
                stmt.setString(3, emp.getLastName());
                stmt.setString(4, emp.getEmail());
                stmt.setString(5, emp.getPhoneNumber());
                stmt.setString(6, emp.getJobId());
                stmt.setBigDecimal(7, emp.getSalary());
                stmt.addBatch();
            }
            
            stmt.executeBatch();
        }
    }

    /**
     * Create test employee data
     */
    protected List<Employee> createTestEmployees() {
        List<Employee> employees = new ArrayList<>();
        
        employees.add(new Employee(1, "John", "Doe", "john.doe@anyco.com", "555-0101", "IT_PROG", new BigDecimal("75000")));
        employees.add(new Employee(2, "Jane", "Smith", "jane.smith@anyco.com", "555-0102", "HR_REP", new BigDecimal("65000")));
        employees.add(new Employee(3, "Bob", "Johnson", "bob.johnson@anyco.com", "555-0103", "SA_MAN", new BigDecimal("85000")));
        employees.add(new Employee(4, "Alice", "Brown", "alice.brown@anyco.com", "555-0104", "IT_PROG", new BigDecimal("70000")));
        employees.add(new Employee(5, "Charlie", "Wilson", "charlie.wilson@anyco.com", "555-0105", "HR_REP", new BigDecimal("60000")));
        
        return employees;
    }

    /**
     * Get the PostgreSQL container for direct access
     */
    protected PostgreSQLContainer<?> getPostgresContainer() {
        return postgres;
    }

    /**
     * Execute SQL and return the number of affected rows
     */
    protected int executeUpdate(String sql) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return stmt.executeUpdate();
        }
    }

    /**
     * Check if a table exists in the database
     */
    protected boolean tableExists(String tableName) throws SQLException {
        try (Connection conn = getConnection()) {
            return conn.getMetaData().getTables(null, null, tableName, null).next();
        }
    }
}