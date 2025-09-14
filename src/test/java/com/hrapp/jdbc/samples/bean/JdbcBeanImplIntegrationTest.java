package com.hrapp.jdbc.samples.bean;

import com.hrapp.jdbc.samples.config.DatabaseConfig;
import com.hrapp.jdbc.samples.entity.Employee;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for JdbcBeanImpl using TestContainers with PostgreSQL
 * 
 * These tests verify the actual database operations against a real PostgreSQL instance.
 */
@Testcontainers
class JdbcBeanImplIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("hrdb")
            .withUsername("hr_user")
            .withPassword("hr_password")
            .withInitScript("test-schema.sql");

    private JdbcBeanImpl jdbcBean;
    private DatabaseConfig dbConfig;

    @BeforeAll
    static void setUpContainer() {
        // Set system properties for database configuration
        System.setProperty("DB_URL", postgres.getJdbcUrl());
        System.setProperty("DB_USERNAME", postgres.getUsername());
        System.setProperty("DB_PASSWORD", postgres.getPassword());
        System.setProperty("DB_SCHEMA", "hr");
    }

    @BeforeEach
    void setUp() throws SQLException {
        // Create a fresh database configuration for each test
        dbConfig = DatabaseConfig.getInstance();
        jdbcBean = new JdbcBeanImpl(dbConfig);
        
        // Clean and set up test data
        setupTestData();
    }

    @AfterAll
    static void tearDown() {
        // Clean up system properties
        System.clearProperty("DB_URL");
        System.clearProperty("DB_USERNAME");
        System.clearProperty("DB_PASSWORD");
        System.clearProperty("DB_SCHEMA");
    }

    private void setupTestData() throws SQLException {
        try (Connection connection = dbConfig.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Clean existing data
            statement.execute("DELETE FROM employees");
            
            // Reset sequence
            statement.execute("ALTER SEQUENCE employees_employee_id_seq RESTART WITH 1");
            
            // Insert test data
            statement.execute(
                "INSERT INTO employees (first_name, last_name, email, phone_number, job_id, salary, created_at, updated_at) VALUES " +
                "('John', 'Doe', 'john.doe@company.com', '555-1234', 'IT_PROG', 75000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), " +
                "('Jane', 'Smith', 'jane.smith@company.com', '555-5678', 'HR_REP', 65000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), " +
                "('Bob', 'Johnson', 'bob.johnson@company.com', '555-9012', 'FI_ACCOUNT', 55000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), " +
                "('Alice', 'Williams', 'alice.williams@company.com', '555-3456', 'IT_PROG', 80000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), " +
                "('Charlie', 'Brown', 'charlie.brown@company.com', '555-7890', 'MK_REP', 60000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            );
        }
    }

    @Test
    void testGetEmployees() {
        // Act
        List<Employee> employees = jdbcBean.getEmployees();

        // Assert
        assertNotNull(employees);
        assertEquals(5, employees.size());
        
        // Verify first employee
        Employee firstEmployee = employees.get(0);
        assertEquals(1, firstEmployee.getEmployeeId());
        assertEquals("John", firstEmployee.getFirstName());
        assertEquals("Doe", firstEmployee.getLastName());
        assertEquals("john.doe@company.com", firstEmployee.getEmail());
        assertEquals("555-1234", firstEmployee.getPhoneNumber());
        assertEquals("IT_PROG", firstEmployee.getJobId());
        assertEquals(new BigDecimal("75000.00"), firstEmployee.getSalary());
    }

    @Test
    void testGetEmployee_ExistingId() {
        // Act
        List<Employee> employees = jdbcBean.getEmployee(1);

        // Assert
        assertNotNull(employees);
        assertEquals(1, employees.size());
        
        Employee employee = employees.get(0);
        assertEquals(1, employee.getEmployeeId());
        assertEquals("John", employee.getFirstName());
        assertEquals("Doe", employee.getLastName());
    }

    @Test
    void testGetEmployee_NonExistentId() {
        // Act
        List<Employee> employees = jdbcBean.getEmployee(999);

        // Assert
        assertNotNull(employees);
        assertTrue(employees.isEmpty());
    }

    @Test
    void testUpdateEmployee() {
        // Act
        Employee updatedEmployee = jdbcBean.updateEmployee(1);

        // Assert
        assertNotNull(updatedEmployee);
        assertEquals(1, updatedEmployee.getEmployeeId());
        
        // Verify 10% salary increase: 75000 * 1.1 = 82500
        assertEquals(new BigDecimal("82500.00"), updatedEmployee.getSalary());
    }

    @Test
    void testGetEmployeeByFn() {
        // Act - search for names starting with "J"
        List<Employee> employees = jdbcBean.getEmployeeByFn("J");

        // Assert
        assertNotNull(employees);
        assertEquals(2, employees.size()); // John and Jane
        
        assertTrue(employees.stream().allMatch(emp -> emp.getFirstName().startsWith("J")));
        
        // Verify specific employees
        Employee john = employees.stream()
                .filter(emp -> "John".equals(emp.getFirstName()))
                .findFirst()
                .orElse(null);
        assertNotNull(john);
        assertEquals("Doe", john.getLastName());
        
        Employee jane = employees.stream()
                .filter(emp -> "Jane".equals(emp.getFirstName()))
                .findFirst()
                .orElse(null);
        assertNotNull(jane);
        assertEquals("Smith", jane.getLastName());
    }

    @Test
    void testGetEmployeeByFn_CaseInsensitive() {
        // Act - search with lowercase
        List<Employee> employees = jdbcBean.getEmployeeByFn("j");

        // Assert
        assertNotNull(employees);
        assertEquals(2, employees.size()); // Should still find John and Jane
    }

    @Test
    void testIncrementSalary_PositiveIncrement() {
        // Act - 10% increase
        List<Employee> employees = jdbcBean.incrementSalary(10);

        // Assert
        assertNotNull(employees);
        assertEquals(5, employees.size());
        
        // Verify salary increases
        Employee john = employees.stream()
                .filter(emp -> "John".equals(emp.getFirstName()))
                .findFirst()
                .orElse(null);
        assertNotNull(john);
        assertEquals(new BigDecimal("82500.00"), john.getSalary()); // 75000 * 1.1
        
        Employee jane = employees.stream()
                .filter(emp -> "Jane".equals(emp.getFirstName()))
                .findFirst()
                .orElse(null);
        assertNotNull(jane);
        assertEquals(new BigDecimal("71500.00"), jane.getSalary()); // 65000 * 1.1
    }

    @Test
    void testIncrementSalary_NegativeIncrement() {
        // Act - 5% decrease
        List<Employee> employees = jdbcBean.incrementSalary(-5);

        // Assert
        assertNotNull(employees);
        assertEquals(5, employees.size());
        
        // Verify salary decreases
        Employee john = employees.stream()
                .filter(emp -> "John".equals(emp.getFirstName()))
                .findFirst()
                .orElse(null);
        assertNotNull(john);
        assertEquals(new BigDecimal("71250.00"), john.getSalary()); // 75000 * 0.95
    }

    @Test
    void testCreateEmployee() {
        // Arrange
        Employee newEmployee = new Employee(0, "Test", "User", "test.user@company.com", 
                                          "555-0000", "IT_PROG", new BigDecimal("70000.00"));

        // Act
        Employee createdEmployee = jdbcBean.createEmployee(newEmployee);

        // Assert
        assertNotNull(createdEmployee);
        assertTrue(createdEmployee.getEmployeeId() > 0); // Should have generated ID
        assertEquals("Test", createdEmployee.getFirstName());
        assertEquals("User", createdEmployee.getLastName());
        assertEquals("test.user@company.com", createdEmployee.getEmail());
        assertEquals("555-0000", createdEmployee.getPhoneNumber());
        assertEquals("IT_PROG", createdEmployee.getJobId());
        assertEquals(new BigDecimal("70000.00"), createdEmployee.getSalary());
        
        // Verify employee was actually created in database
        List<Employee> allEmployees = jdbcBean.getEmployees();
        assertEquals(6, allEmployees.size()); // Original 5 + 1 new
    }

    @Test
    void testUpdateEmployeeObject() {
        // Arrange - get existing employee and modify
        List<Employee> employees = jdbcBean.getEmployee(1);
        assertFalse(employees.isEmpty());
        
        Employee employee = employees.get(0);
        employee.setFirstName("Updated");
        employee.setLastName("Name");
        employee.setEmail("updated@company.com");
        employee.setPhoneNumber("555-9999");
        employee.setSalary(new BigDecimal("85000.00"));

        // Act
        Employee updatedEmployee = jdbcBean.updateEmployee(employee);

        // Assert
        assertNotNull(updatedEmployee);
        assertEquals(1, updatedEmployee.getEmployeeId());
        assertEquals("Updated", updatedEmployee.getFirstName());
        assertEquals("Name", updatedEmployee.getLastName());
        assertEquals("updated@company.com", updatedEmployee.getEmail());
        assertEquals("555-9999", updatedEmployee.getPhoneNumber());
        assertEquals(new BigDecimal("85000.00"), updatedEmployee.getSalary());
        
        // Verify changes persisted in database
        List<Employee> retrievedEmployees = jdbcBean.getEmployee(1);
        assertFalse(retrievedEmployees.isEmpty());
        Employee retrievedEmployee = retrievedEmployees.get(0);
        assertEquals("Updated", retrievedEmployee.getFirstName());
        assertEquals("Name", retrievedEmployee.getLastName());
    }

    @Test
    void testDeleteEmployee() {
        // Verify employee exists
        List<Employee> beforeDelete = jdbcBean.getEmployee(1);
        assertFalse(beforeDelete.isEmpty());

        // Act
        boolean deleted = jdbcBean.deleteEmployee(1);

        // Assert
        assertTrue(deleted);
        
        // Verify employee no longer exists
        List<Employee> afterDelete = jdbcBean.getEmployee(1);
        assertTrue(afterDelete.isEmpty());
        
        // Verify total count decreased
        List<Employee> allEmployees = jdbcBean.getEmployees();
        assertEquals(4, allEmployees.size()); // Original 5 - 1 deleted
    }

    @Test
    void testDeleteEmployee_NonExistent() {
        // Act
        boolean deleted = jdbcBean.deleteEmployee(999);

        // Assert
        assertFalse(deleted);
        
        // Verify no employees were affected
        List<Employee> allEmployees = jdbcBean.getEmployees();
        assertEquals(5, allEmployees.size());
    }

    @Test
    void testIsConnectionHealthy() {
        // Act
        boolean healthy = jdbcBean.isConnectionHealthy();

        // Assert
        assertTrue(healthy);
    }

    @Test
    void testCompleteWorkflow() {
        // Test a complete CRUD workflow
        
        // 1. Create a new employee
        Employee newEmployee = new Employee(0, "Workflow", "Test", "workflow.test@company.com", 
                                          "555-1111", "IT_PROG", new BigDecimal("72000.00"));
        Employee created = jdbcBean.createEmployee(newEmployee);
        assertNotNull(created);
        int newId = created.getEmployeeId();
        
        // 2. Read the created employee
        List<Employee> retrieved = jdbcBean.getEmployee(newId);
        assertFalse(retrieved.isEmpty());
        assertEquals("Workflow", retrieved.get(0).getFirstName());
        
        // 3. Update the employee
        Employee toUpdate = retrieved.get(0);
        toUpdate.setSalary(new BigDecimal("80000.00"));
        Employee updated = jdbcBean.updateEmployee(toUpdate);
        assertNotNull(updated);
        assertEquals(new BigDecimal("80000.00"), updated.getSalary());
        
        // 4. Apply salary increment
        List<Employee> afterIncrement = jdbcBean.incrementSalary(5);
        Employee incrementedEmployee = afterIncrement.stream()
                .filter(emp -> emp.getEmployeeId() == newId)
                .findFirst()
                .orElse(null);
        assertNotNull(incrementedEmployee);
        assertEquals(new BigDecimal("84000.00"), incrementedEmployee.getSalary()); // 80000 * 1.05
        
        // 5. Delete the employee
        boolean deleted = jdbcBean.deleteEmployee(newId);
        assertTrue(deleted);
        
        // 6. Verify deletion
        List<Employee> afterDelete = jdbcBean.getEmployee(newId);
        assertTrue(afterDelete.isEmpty());
    }
}