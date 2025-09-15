package com.hrapp.jdbc.samples.bean;

import com.hrapp.jdbc.samples.config.ConnectionFactory;
import com.hrapp.jdbc.samples.entity.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive CRUD operation tests for JdbcBeanImpl
 * Tests all Create, Read, Update, Delete operations as required by task 11
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcBeanImpl CRUD Operations Tests")
class JdbcBeanImplCrudTest {

    @Mock
    private ConnectionFactory mockConnectionFactory;
    
    @Mock
    private Connection mockConnection;
    
    @Mock
    private PreparedStatement mockPreparedStatement;
    
    @Mock
    private ResultSet mockResultSet;

    private JdbcBeanImpl jdbcBean;

    @BeforeEach
    void setUp() {
        jdbcBean = new JdbcBeanImpl(mockConnectionFactory);
    }

    // CREATE OPERATION TESTS

    @Test
    @DisplayName("Create Employee - Success")
    void testCreateEmployee_Success() throws SQLException {
        // Arrange
        Employee newEmployee = new Employee(0, "Alice", "Johnson", "alice.johnson@company.com", 
                                          "555-7777", "IT_PROG", new BigDecimal("70000.00"));
        
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(10); // Generated ID

        // Act
        Employee createdEmployee = jdbcBean.createEmployee(newEmployee);

        // Assert
        assertNotNull(createdEmployee);
        assertEquals(10, createdEmployee.getEmployeeId());
        assertEquals("Alice", createdEmployee.getFirstName());
        assertEquals("Johnson", createdEmployee.getLastName());
        assertEquals("alice.johnson@company.com", createdEmployee.getEmail());
        assertEquals("555-7777", createdEmployee.getPhoneNumber());
        assertEquals("IT_PROG", createdEmployee.getJobId());
        assertEquals(new BigDecimal("70000.00"), createdEmployee.getSalary());
        
        // Verify SQL parameters
        verify(mockPreparedStatement).setString(1, "Alice");
        verify(mockPreparedStatement).setString(2, "Johnson");
        verify(mockPreparedStatement).setString(3, "alice.johnson@company.com");
        verify(mockPreparedStatement).setString(4, "555-7777");
        verify(mockPreparedStatement).setString(5, "IT_PROG");
        verify(mockPreparedStatement).setBigDecimal(6, new BigDecimal("70000.00"));
    }

    @Test
    @DisplayName("Create Employee - Database Error")
    void testCreateEmployee_DatabaseError() throws SQLException {
        // Arrange
        Employee newEmployee = new Employee(0, "Test", "User", "test@company.com", 
                                          "555-0000", "IT_PROG", new BigDecimal("50000.00"));
        
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            jdbcBean.createEmployee(newEmployee);
        });
        
        assertTrue(exception.getMessage().contains("Database operation failed"));
        assertTrue(exception.getCause() instanceof SQLException);
    }

    @Test
    @DisplayName("Create Employee - No Generated ID Returned")
    void testCreateEmployee_NoGeneratedId() throws SQLException {
        // Arrange
        Employee newEmployee = new Employee(0, "Test", "User", "test@company.com", 
                                          "555-0000", "IT_PROG", new BigDecimal("50000.00"));
        
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // No generated ID

        // Act
        Employee createdEmployee = jdbcBean.createEmployee(newEmployee);

        // Assert
        assertNull(createdEmployee);
    }

    // READ OPERATION TESTS

    @Test
    @DisplayName("Read All Employees - Success")
    void testGetEmployees_Success() throws SQLException {
        // Arrange
        setupMockForReadOperations();
        
        when(mockResultSet.next()).thenReturn(true, true, false);
        setupEmployeeResultSet(1, "John", "Doe", "john.doe@company.com", "555-1234", "IT_PROG", "75000.00");
        setupEmployeeResultSet(2, "Jane", "Smith", "jane.smith@company.com", "555-5678", "HR_REP", "65000.00");

        // Act
        List<Employee> employees = jdbcBean.getEmployees();

        // Assert
        assertNotNull(employees);
        assertEquals(2, employees.size());
        
        Employee firstEmployee = employees.get(0);
        assertEquals(1, firstEmployee.getEmployeeId());
        assertEquals("John", firstEmployee.getFirstName());
        assertEquals("Doe", firstEmployee.getLastName());
    }

    @Test
    @DisplayName("Read Employee by ID - Found")
    void testGetEmployee_Found() throws SQLException {
        // Arrange
        int empId = 1;
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        
        when(mockResultSet.next()).thenReturn(true);
        setupEmployeeResultSet(empId, "John", "Doe", "john.doe@company.com", "555-1234", "IT_PROG", "75000.00");

        // Act
        List<Employee> employees = jdbcBean.getEmployee(empId);

        // Assert
        assertNotNull(employees);
        assertEquals(1, employees.size());
        assertEquals(empId, employees.get(0).getEmployeeId());
        verify(mockPreparedStatement).setInt(1, empId);
    }

    @Test
    @DisplayName("Read Employee by ID - Not Found")
    void testGetEmployee_NotFound() throws SQLException {
        // Arrange
        int empId = 999;
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        // Act
        List<Employee> employees = jdbcBean.getEmployee(empId);

        // Assert
        assertNotNull(employees);
        assertTrue(employees.isEmpty());
    }

    @Test
    @DisplayName("Read Employees by First Name - Multiple Results")
    void testGetEmployeeByFn_MultipleResults() throws SQLException {
        // Arrange
        String firstName = "J";
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        
        when(mockResultSet.next()).thenReturn(true, true, false);
        setupEmployeeResultSet(1, "John", "Doe", "john.doe@company.com", "555-1234", "IT_PROG", "75000.00");
        setupEmployeeResultSet(2, "Jane", "Smith", "jane.smith@company.com", "555-5678", "HR_REP", "65000.00");

        // Act
        List<Employee> employees = jdbcBean.getEmployeeByFn(firstName);

        // Assert
        assertNotNull(employees);
        assertEquals(2, employees.size());
        assertTrue(employees.get(0).getFirstName().startsWith("J"));
        assertTrue(employees.get(1).getFirstName().startsWith("J"));
        verify(mockPreparedStatement).setString(1, firstName + "%");
    }

    @Test
    @DisplayName("Read Employees by First Name - No Results")
    void testGetEmployeeByFn_NoResults() throws SQLException {
        // Arrange
        String firstName = "Z";
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        // Act
        List<Employee> employees = jdbcBean.getEmployeeByFn(firstName);

        // Assert
        assertNotNull(employees);
        assertTrue(employees.isEmpty());
    }

    // UPDATE OPERATION TESTS

    @Test
    @DisplayName("Update Employee by ID - Success")
    void testUpdateEmployeeById_Success() throws SQLException {
        // Arrange
        int empId = 1;
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        
        // Mock update operation
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        
        // Mock select operation for returning updated employee
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        setupEmployeeResultSet(empId, "John", "Doe", "john.doe@company.com", "555-1234", "IT_PROG", "82500.00");

        // Act
        Employee updatedEmployee = jdbcBean.updateEmployee(empId);

        // Assert
        assertNotNull(updatedEmployee);
        assertEquals(empId, updatedEmployee.getEmployeeId());
        assertEquals(new BigDecimal("82500.00"), updatedEmployee.getSalary());
    }

    @Test
    @DisplayName("Update Employee by ID - Employee Not Found")
    void testUpdateEmployeeById_NotFound() throws SQLException {
        // Arrange
        int empId = 999;
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0); // No rows updated

        // Act
        Employee updatedEmployee = jdbcBean.updateEmployee(empId);

        // Assert
        assertNull(updatedEmployee);
    }

    @Test
    @DisplayName("Update Employee Object - Success")
    void testUpdateEmployeeObject_Success() throws SQLException {
        // Arrange
        Employee employee = new Employee(1, "Updated", "Name", "updated@company.com", 
                                       "555-9999", "SA_MAN", new BigDecimal("85000.00"));
        
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        Employee updatedEmployee = jdbcBean.updateEmployee(employee);

        // Assert
        assertNotNull(updatedEmployee);
        assertEquals(employee, updatedEmployee);
        
        // Verify all parameters were set correctly
        verify(mockPreparedStatement).setString(1, "Updated");
        verify(mockPreparedStatement).setString(2, "Name");
        verify(mockPreparedStatement).setString(3, "updated@company.com");
        verify(mockPreparedStatement).setString(4, "555-9999");
        verify(mockPreparedStatement).setString(5, "SA_MAN");
        verify(mockPreparedStatement).setBigDecimal(6, new BigDecimal("85000.00"));
        verify(mockPreparedStatement).setInt(7, 1);
    }

    @Test
    @DisplayName("Update Employee Object - Employee Not Found")
    void testUpdateEmployeeObject_NotFound() throws SQLException {
        // Arrange
        Employee employee = new Employee(999, "NonExistent", "User", "nonexistent@company.com", 
                                       "555-0000", "IT_PROG", new BigDecimal("50000.00"));
        
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0); // No rows updated

        // Act
        Employee updatedEmployee = jdbcBean.updateEmployee(employee);

        // Assert
        assertNull(updatedEmployee);
    }

    @Test
    @DisplayName("Update Employee Object - Database Error")
    void testUpdateEmployeeObject_DatabaseError() throws SQLException {
        // Arrange
        Employee employee = new Employee(1, "Test", "User", "test@company.com", 
                                       "555-0000", "IT_PROG", new BigDecimal("50000.00"));
        
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            jdbcBean.updateEmployee(employee);
        });
        
        assertTrue(exception.getMessage().contains("Database operation failed"));
    }

    // DELETE OPERATION TESTS

    @Test
    @DisplayName("Delete Employee - Success")
    void testDeleteEmployee_Success() throws SQLException {
        // Arrange
        int empId = 1;
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        boolean result = jdbcBean.deleteEmployee(empId);

        // Assert
        assertTrue(result);
        verify(mockPreparedStatement).setInt(1, empId);
    }

    @Test
    @DisplayName("Delete Employee - Employee Not Found")
    void testDeleteEmployee_NotFound() throws SQLException {
        // Arrange
        int empId = 999;
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        // Act
        boolean result = jdbcBean.deleteEmployee(empId);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Delete Employee - Database Error")
    void testDeleteEmployee_DatabaseError() throws SQLException {
        // Arrange
        int empId = 1;
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            jdbcBean.deleteEmployee(empId);
        });
        
        assertTrue(exception.getMessage().contains("Database operation failed"));
    }

    // HEALTH CHECK TESTS

    @Test
    @DisplayName("Connection Health Check - Healthy")
    void testIsConnectionHealthy_Healthy() {
        // Arrange
        when(mockConnectionFactory.isHealthy()).thenReturn(true);

        // Act
        boolean healthy = jdbcBean.isConnectionHealthy();

        // Assert
        assertTrue(healthy);
        verify(mockConnectionFactory).isHealthy();
    }

    @Test
    @DisplayName("Connection Health Check - Unhealthy")
    void testIsConnectionHealthy_Unhealthy() {
        // Arrange
        when(mockConnectionFactory.isHealthy()).thenReturn(false);

        // Act
        boolean healthy = jdbcBean.isConnectionHealthy();

        // Assert
        assertFalse(healthy);
        verify(mockConnectionFactory).isHealthy();
    }

    // FALLBACK BEHAVIOR TESTS

    @Test
    @DisplayName("Read Operations - Database Unavailable - Fallback to Sample Data")
    void testReadOperations_DatabaseUnavailable_FallbackBehavior() throws SQLException {
        // Arrange
        when(mockConnectionFactory.getConnection()).thenThrow(new SQLException("Database unavailable"));

        // Act - Test all read operations
        List<Employee> allEmployees = jdbcBean.getEmployees();
        List<Employee> employeeById = jdbcBean.getEmployee(1);
        List<Employee> employeesByName = jdbcBean.getEmployeeByFn("J");

        // Assert
        assertNotNull(allEmployees);
        assertEquals(5, allEmployees.size()); // Sample data has 5 employees
        
        assertNotNull(employeeById);
        assertEquals(1, employeeById.size());
        assertEquals(1, employeeById.get(0).getEmployeeId());
        
        assertNotNull(employeesByName);
        assertEquals(2, employeesByName.size()); // John and Jane from sample data
    }

    // Helper methods

    private void setupMockForReadOperations() throws SQLException {
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        java.sql.Statement mockStatement = mock(java.sql.Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
    }

    private void setupEmployeeResultSet(int id, String firstName, String lastName, 
                                      String email, String phone, String jobId, String salary) throws SQLException {
        when(mockResultSet.getInt("employee_id")).thenReturn(id);
        when(mockResultSet.getString("first_name")).thenReturn(firstName);
        when(mockResultSet.getString("last_name")).thenReturn(lastName);
        when(mockResultSet.getString("email")).thenReturn(email);
        when(mockResultSet.getString("phone_number")).thenReturn(phone);
        when(mockResultSet.getString("job_id")).thenReturn(jobId);
        when(mockResultSet.getBigDecimal("salary")).thenReturn(new BigDecimal(salary));
    }
}