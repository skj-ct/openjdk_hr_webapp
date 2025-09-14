package com.hrapp.jdbc.samples.integration;

import com.hrapp.jdbc.samples.bean.JdbcBean;
import com.hrapp.jdbc.samples.bean.JdbcBeanImpl;
import com.hrapp.jdbc.samples.entity.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for complete CRUD workflows using actual PostgreSQL database.
 * Tests Requirements: 7.1, 7.2, 9.3
 */
@DisplayName("CRUD Workflow Integration Tests")
class CrudWorkflowIntegrationTest extends BaseIntegrationTest {

    private JdbcBean jdbcBean;

    @BeforeEach
    void setUp() throws SQLException {
        // Initialize JdbcBean with default ConnectionFactory
        jdbcBean = new JdbcBeanImpl();
        
        // Insert test employees for testing
        insertTestEmployees();
    }

    @Test
    @DisplayName("Should perform complete employee lifecycle - Create, Read, Update, Delete")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testCompleteEmployeeLifecycle() throws SQLException {
        // CREATE - Add new employee
        Employee newEmployee = new Employee();
        newEmployee.setFirstName("Integration");
        newEmployee.setLastName("Test");
        newEmployee.setEmail("integration.test@anyco.com");
        newEmployee.setPhoneNumber("555-9999");
        newEmployee.setJobId("IT_PROG");
        newEmployee.setSalary(new BigDecimal("80000.00"));

        Employee result = jdbcBean.createEmployee(newEmployee);
        assertNotNull(result, "Employee should be created successfully");

        // READ - Verify employee was created
        List<Employee> allEmployees = jdbcBean.getEmployees();
        assertFalse(allEmployees.isEmpty(), "Should have at least one employee");
        
        Employee createdEmployee = allEmployees.stream()
                .filter(emp -> "integration.test@anyco.com".equals(emp.getEmail()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(createdEmployee, "Created employee should be found");
        assertEquals("Integration", createdEmployee.getFirstName());
        assertEquals("Test", createdEmployee.getLastName());
        assertEquals("integration.test@anyco.com", createdEmployee.getEmail());
        assertEquals("555-9999", createdEmployee.getPhoneNumber());
        assertEquals("IT_PROG", createdEmployee.getJobId());
        assertEquals(new BigDecimal("80000.00"), createdEmployee.getSalary());
        assertTrue(createdEmployee.getEmployeeId() > 0, "Employee ID should be assigned");

        // READ BY ID - Test specific employee retrieval
        int employeeId = createdEmployee.getEmployeeId();
        List<Employee> foundEmployees = jdbcBean.getEmployee(employeeId);
        Employee foundEmployee = foundEmployees.isEmpty() ? null : foundEmployees.get(0);
        assertNotNull(foundEmployee, "Employee should be found by ID");
        assertEquals(employeeId, foundEmployee.getEmployeeId());
        assertEquals("Integration", foundEmployee.getFirstName());

        // UPDATE - Modify employee details
        foundEmployee.setFirstName("Updated");
        foundEmployee.setLastName("Employee");
        foundEmployee.setSalary(new BigDecimal("85000.00"));
        foundEmployee.setPhoneNumber("555-8888");

        Employee updateResult = jdbcBean.updateEmployee(foundEmployee);
        assertNotNull(updateResult, "Employee should be updated successfully");

        // READ AFTER UPDATE - Verify changes
        List<Employee> updatedEmployees = jdbcBean.getEmployee(employeeId);
        Employee updatedEmployee = updatedEmployees.isEmpty() ? null : updatedEmployees.get(0);
        assertNotNull(updatedEmployee, "Updated employee should still exist");
        assertEquals("Updated", updatedEmployee.getFirstName());
        assertEquals("Employee", updatedEmployee.getLastName());
        assertEquals(new BigDecimal("85000.00"), updatedEmployee.getSalary());
        assertEquals("555-8888", updatedEmployee.getPhoneNumber());
        // Email and job_id should remain unchanged
        assertEquals("integration.test@anyco.com", updatedEmployee.getEmail());
        assertEquals("IT_PROG", updatedEmployee.getJobId());

        // DELETE - Remove employee
        boolean deleteResult = jdbcBean.deleteEmployee(employeeId);
        assertTrue(deleteResult, "Employee should be deleted successfully");

        // READ AFTER DELETE - Verify employee is gone
        List<Employee> deletedEmployees = jdbcBean.getEmployee(employeeId);
        Employee deletedEmployee = deletedEmployees.isEmpty() ? null : deletedEmployees.get(0);
        assertNull(deletedEmployee, "Employee should not exist after deletion");

        // Verify employee is not in the list
        List<Employee> finalEmployees = jdbcBean.getEmployees();
        boolean employeeExists = finalEmployees.stream()
                .anyMatch(emp -> emp.getEmployeeId() == employeeId);
        assertFalse(employeeExists, "Deleted employee should not appear in employee list");
    }

    @Test
    @DisplayName("Should handle bulk operations efficiently")
    void testBulkOperations() throws SQLException {
        // Insert multiple employees
        Employee[] employees = {
            createTestEmployee("Bulk1", "Test1", "bulk1@anyco.com", "IT_PROG", "70000.00"),
            createTestEmployee("Bulk2", "Test2", "bulk2@anyco.com", "HR_REP", "65000.00"),
            createTestEmployee("Bulk3", "Test3", "bulk3@anyco.com", "SA_MAN", "80000.00"),
            createTestEmployee("Bulk4", "Test4", "bulk4@anyco.com", "IT_PROG", "72000.00"),
            createTestEmployee("Bulk5", "Test5", "bulk5@anyco.com", "HR_REP", "68000.00")
        };

        // Add all employees
        for (Employee emp : employees) {
            Employee result = jdbcBean.createEmployee(emp);
            assertNotNull(result, "Employee should be created successfully");
        }

        // Verify all employees exist
        List<Employee> allEmployees = jdbcBean.getEmployees();
        assertTrue(allEmployees.size() >= 5, "Should have at least 5 employees");

        // Test salary increment on all employees
        List<Employee> incrementedEmployees = jdbcBean.incrementSalary(10);
        assertNotNull(incrementedEmployees, "Increment salary should return employee list");
        assertFalse(incrementedEmployees.isEmpty(), "Should have employees after increment");

        // Verify salary increases
        for (Employee emp : incrementedEmployees) {
            Employee original = findEmployeeByEmail(employees, emp.getEmail());
            if (original != null) {
                BigDecimal expectedSalary = original.getSalary().multiply(new BigDecimal("1.10"));
                assertEquals(0, expectedSalary.compareTo(emp.getSalary()), 
                    "Salary should be increased by 10% for " + emp.getEmail());
            }
        }

        // Clean up - delete all test employees
        for (Employee emp : incrementedEmployees) {
            if (emp.getEmail().startsWith("bulk")) {
                jdbcBean.deleteEmployee(emp.getEmployeeId());
            }
        }
    }

    @Test
    @DisplayName("Should handle concurrent CRUD operations")
    void testConcurrentCrudOperations() throws InterruptedException {
        final int threadCount = 5;
        final int operationsPerThread = 3;
        Thread[] threads = new Thread[threadCount];
        final Exception[] exceptions = new Exception[threadCount];

        // Create threads that perform CRUD operations concurrently
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    JdbcBean threadJdbcBean = new JdbcBeanImpl();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        // Create employee
                        Employee emp = createTestEmployee(
                            "Concurrent" + threadId,
                            "Test" + j,
                            "concurrent" + threadId + "_" + j + "@anyco.com",
                            "IT_PROG",
                            "75000.00"
                        );
                        
                        Employee addResult = threadJdbcBean.createEmployee(emp);
                        assertNotNull(addResult, "Employee should be created successfully");
                        
                        // Read all employees
                        List<Employee> employees = threadJdbcBean.getEmployees();
                        assertFalse(employees.isEmpty());
                        
                        // Find and update the employee
                        Employee created = employees.stream()
                            .filter(e -> e.getEmail().equals(emp.getEmail()))
                            .findFirst()
                            .orElse(null);
                        
                        if (created != null) {
                            created.setSalary(new BigDecimal("80000.00"));
                            Employee updateResult = threadJdbcBean.updateEmployee(created);
                            assertNotNull(updateResult, "Employee should be updated successfully");
                            
                            // Delete the employee
                            boolean deleteResult = threadJdbcBean.deleteEmployee(created.getEmployeeId());
                            assertTrue(deleteResult, "Employee should be deleted successfully");
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

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(10000); // 10 second timeout per thread
        }

        // Check for exceptions
        for (int i = 0; i < threadCount; i++) {
            if (exceptions[i] != null) {
                fail("Thread " + i + " threw exception: " + exceptions[i].getMessage());
            }
        }
    }

    @Test
    @DisplayName("Should handle search operations correctly")
    void testSearchOperations() throws SQLException {
        // Test search by first name
        List<Employee> johnEmployees = jdbcBean.getEmployeeByFn("John");
        assertNotNull(johnEmployees, "Search result should not be null");
        
        // All returned employees should have first names starting with "John"
        for (Employee emp : johnEmployees) {
            assertTrue(emp.getFirstName().startsWith("John"), 
                "Employee first name should start with 'John': " + emp.getFirstName());
        }

        // Test search with no results
        List<Employee> noResults = jdbcBean.getEmployeeByFn("NonExistent");
        assertNotNull(noResults, "Search result should not be null even when no matches");
        assertTrue(noResults.isEmpty(), "Should return empty list for non-existent names");

        // Test search with partial name
        List<Employee> partialResults = jdbcBean.getEmployeeByFn("J");
        assertNotNull(partialResults, "Partial search should work");
        
        for (Employee emp : partialResults) {
            assertTrue(emp.getFirstName().startsWith("J"), 
                "Employee first name should start with 'J': " + emp.getFirstName());
        }
    }

    @Test
    @DisplayName("Should handle salary increment operations")
    void testSalaryIncrementOperations() throws SQLException {
        // Get initial employee list
        List<Employee> initialEmployees = jdbcBean.getEmployees();
        assertFalse(initialEmployees.isEmpty(), "Should have employees for salary increment test");

        // Apply 15% salary increment
        List<Employee> incrementedEmployees = jdbcBean.incrementSalary(15);
        assertNotNull(incrementedEmployees, "Increment result should not be null");
        assertEquals(initialEmployees.size(), incrementedEmployees.size(), 
            "Should return same number of employees after increment");

        // Verify salary increases
        for (Employee incrementedEmp : incrementedEmployees) {
            Employee originalEmp = initialEmployees.stream()
                .filter(emp -> emp.getEmployeeId() == incrementedEmp.getEmployeeId())
                .findFirst()
                .orElse(null);
            
            if (originalEmp != null) {
                BigDecimal expectedSalary = originalEmp.getSalary().multiply(new BigDecimal("1.15"));
                assertEquals(0, expectedSalary.compareTo(incrementedEmp.getSalary()),
                    "Salary should be increased by 15% for employee " + incrementedEmp.getEmployeeId());
            }
        }

        // Test negative increment (salary decrease)
        List<Employee> decreasedEmployees = jdbcBean.incrementSalary(-10);
        assertNotNull(decreasedEmployees, "Decrement result should not be null");
        
        for (Employee decreasedEmp : decreasedEmployees) {
            Employee incrementedEmp = incrementedEmployees.stream()
                .filter(emp -> emp.getEmployeeId() == decreasedEmp.getEmployeeId())
                .findFirst()
                .orElse(null);
            
            if (incrementedEmp != null) {
                BigDecimal expectedSalary = incrementedEmp.getSalary().multiply(new BigDecimal("0.90"));
                assertEquals(0, expectedSalary.compareTo(decreasedEmp.getSalary()),
                    "Salary should be decreased by 10% for employee " + decreasedEmp.getEmployeeId());
            }
        }
    }

    @Test
    @DisplayName("Should handle connection health checks")
    void testConnectionHealthCheck() {
        // Test connection health
        boolean isHealthy = jdbcBean.isConnectionHealthy();
        assertTrue(isHealthy, "Database connection should be healthy in integration test environment");
    }

    // Helper methods

    private Employee createTestEmployee(String firstName, String lastName, String email, String jobId, String salary) {
        Employee emp = new Employee();
        emp.setFirstName(firstName);
        emp.setLastName(lastName);
        emp.setEmail(email);
        emp.setPhoneNumber("555-0000");
        emp.setJobId(jobId);
        emp.setSalary(new BigDecimal(salary));
        return emp;
    }

    private Employee findEmployeeByEmail(Employee[] employees, String email) {
        for (Employee emp : employees) {
            if (emp.getEmail().equals(email)) {
                return emp;
            }
        }
        return null;
    }
}