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
    void setUp() {
        // Initialize JdbcBean with TestContainer data source
        jdbcBean = new JdbcBeanImpl(getDataSource());
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

        String result = jdbcBean.addEmployee(newEmployee);
        assertEquals("Employee added successfully", result, "Employee should be added successfully");

        // READ - Verify employee was created
        List<Employee> allEmployees = jdbcBean.getAllEmployees();
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
        Employee foundEmployee = jdbcBean.getEmployeeById(employeeId);
        assertNotNull(foundEmployee, "Employee should be found by ID");
        assertEquals(employeeId, foundEmployee.getEmployeeId());
        assertEquals("Integration", foundEmployee.getFirstName());

        // UPDATE - Modify employee details
        foundEmployee.setFirstName("Updated");
        foundEmployee.setLastName("Employee");
        foundEmployee.setSalary(new BigDecimal("85000.00"));
        foundEmployee.setPhoneNumber("555-8888");

        String updateResult = jdbcBean.updateEmployee(foundEmployee);
        assertEquals("Employee updated successfully", updateResult, "Employee should be updated successfully");

        // READ AFTER UPDATE - Verify changes
        Employee updatedEmployee = jdbcBean.getEmployeeById(employeeId);
        assertNotNull(updatedEmployee, "Updated employee should still exist");
        assertEquals("Updated", updatedEmployee.getFirstName());
        assertEquals("Employee", updatedEmployee.getLastName());
        assertEquals(new BigDecimal("85000.00"), updatedEmployee.getSalary());
        assertEquals("555-8888", updatedEmployee.getPhoneNumber());
        // Email and job_id should remain unchanged
        assertEquals("integration.test@anyco.com", updatedEmployee.getEmail());
        assertEquals("IT_PROG", updatedEmployee.getJobId());

        // DELETE - Remove employee
        String deleteResult = jdbcBean.deleteEmployee(employeeId);
        assertEquals("Employee deleted successfully", deleteResult, "Employee should be deleted successfully");

        // READ AFTER DELETE - Verify employee is gone
        Employee deletedEmployee = jdbcBean.getEmployeeById(employeeId);
        assertNull(deletedEmployee, "Employee should not exist after deletion");

        // Verify employee is not in the list
        List<Employee> finalEmployees = jdbcBean.getAllEmployees();
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
            String result = jdbcBean.addEmployee(emp);
            assertEquals("Employee added successfully", result);
        }

        // Verify all employees exist
        List<Employee> allEmployees = jdbcBean.getAllEmployees();
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
                    JdbcBean threadJdbcBean = new JdbcBeanImpl(getDataSource());
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        // Create employee
                        Employee emp = createTestEmployee(
                            "Concurrent" + threadId,
                            "Test" + j,
                            "concurrent" + threadId + "_" + j + "@anyco.com",
                            "IT_PROG",
                            "75000.00"
                        );
                        
                        String addResult = threadJdbcBean.addEmployee(emp);
                        assertEquals("Employee added successfully", addResult);
                        
                        // Read all employees
                        List<Employee> employees = threadJdbcBean.getAllEmployees();
                        assertFalse(employees.isEmpty());
                        
                        // Find and update the employee
                        Employee created = employees.stream()
                            .filter(e -> e.getEmail().equals(emp.getEmail()))
                            .findFirst()
                            .orElse(null);
                        
                        if (created != null) {
                            created.setSalary(new BigDecimal("80000.00"));
                            String updateResult = threadJdbcBean.updateEmployee(created);
                            assertEquals("Employee updated successfully", updateResult);
                            
                            // Delete the employee
                            String deleteResult = threadJdbcBean.deleteEmployee(created.getEmployeeId());
                            assertEquals("Employee deleted successfully", deleteResult);
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
            thread.join(10000); // 10 second timeout
        }

        // Check for exceptions
        for (int i = 0; i < threadCount; i++) {
            if (exceptions[i] != null) {
                fail("Thread " + i + " failed with exception: " + exceptions[i].getMessage());
            }
        }
    }

    @Test
    @DisplayName("Should maintain data integrity during complex operations")
    void testDataIntegrityDuringComplexOperations() throws SQLException {
        // Insert test data
        insertTestEmployees();
        
        List<Employee> initialEmployees = jdbcBean.getAllEmployees();
        int initialCount = initialEmployees.size();
        assertTrue(initialCount >= 5, "Should have at least 5 test employees");

        // Perform salary increment
        List<Employee> afterIncrement = jdbcBean.incrementSalary(15);
        assertEquals(initialCount, afterIncrement.size(), 
            "Employee count should remain same after salary increment");

        // Verify all salaries were increased
        for (Employee emp : afterIncrement) {
            Employee original = findEmployeeById(initialEmployees, emp.getEmployeeId());
            assertNotNull(original, "Original employee should exist");
            
            BigDecimal expectedSalary = original.getSalary().multiply(new BigDecimal("1.15"));
            assertEquals(0, expectedSalary.compareTo(emp.getSalary()), 
                "Salary should be increased by 15% for employee " + emp.getEmployeeId());
        }

        // Test transaction rollback scenario by attempting invalid operation
        Employee invalidEmployee = new Employee();
        invalidEmployee.setFirstName("Test");
        invalidEmployee.setLastName("Invalid");
        invalidEmployee.setEmail(""); // Invalid empty email
        invalidEmployee.setPhoneNumber("555-0000");
        invalidEmployee.setJobId("INVALID_JOB");
        invalidEmployee.setSalary(new BigDecimal("-1000.00")); // Invalid negative salary

        // This should fail and not affect existing data
        try {
            jdbcBean.addEmployee(invalidEmployee);
        } catch (Exception e) {
            // Expected to fail
        }

        // Verify data integrity - count should remain same
        List<Employee> finalEmployees = jdbcBean.getAllEmployees();
        assertEquals(initialCount, finalEmployees.size(), 
            "Employee count should remain unchanged after failed operation");
    }

    @Test
    @DisplayName("Should handle edge cases in CRUD operations")
    void testCrudEdgeCases() throws SQLException {
        // Test retrieving non-existent employee
        Employee nonExistent = jdbcBean.getEmployeeById(99999);
        assertNull(nonExistent, "Non-existent employee should return null");

        // Test deleting non-existent employee
        String deleteResult = jdbcBean.deleteEmployee(99999);
        assertEquals("Employee not found", deleteResult, 
            "Deleting non-existent employee should return appropriate message");

        // Test updating non-existent employee
        Employee fakeEmployee = new Employee();
        fakeEmployee.setEmployeeId(99999);
        fakeEmployee.setFirstName("Fake");
        fakeEmployee.setLastName("Employee");
        fakeEmployee.setEmail("fake@anyco.com");
        fakeEmployee.setPhoneNumber("555-0000");
        fakeEmployee.setJobId("IT_PROG");
        fakeEmployee.setSalary(new BigDecimal("50000.00"));

        String updateResult = jdbcBean.updateEmployee(fakeEmployee);
        assertEquals("Employee not found", updateResult, 
            "Updating non-existent employee should return appropriate message");

        // Test empty employee list scenario
        // First, clean all employees
        List<Employee> allEmployees = jdbcBean.getAllEmployees();
        for (Employee emp : allEmployees) {
            jdbcBean.deleteEmployee(emp.getEmployeeId());
        }

        List<Employee> emptyList = jdbcBean.getAllEmployees();
        assertNotNull(emptyList, "Employee list should not be null even when empty");
        assertTrue(emptyList.isEmpty(), "Employee list should be empty");

        // Test salary increment on empty database
        List<Employee> incrementResult = jdbcBean.incrementSalary(10);
        assertNotNull(incrementResult, "Increment result should not be null");
        assertTrue(incrementResult.isEmpty(), "Increment result should be empty when no employees exist");
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

    private Employee findEmployeeById(List<Employee> employees, int employeeId) {
        return employees.stream()
                .filter(emp -> emp.getEmployeeId() == employeeId)
                .findFirst()
                .orElse(null);
    }
}