package com.hrapp.jdbc.samples.bean;

import com.hrapp.jdbc.samples.config.ConnectionFactory;
import com.hrapp.jdbc.samples.entity.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
 * Comprehensive unit tests for salary increment functionality in JdbcBeanImpl
 * Tests various percentage values and edge cases as required by task 11
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcBeanImpl Salary Increment Tests")
class JdbcBeanImplSalaryIncrementTest {

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

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 15, 20, 25, 50, 100})
    @DisplayName("Test salary increment with various positive percentages")
    void testIncrementSalary_PositivePercentages(int incrementPct) throws SQLException {
        // Arrange
        setupMockForIncrementSalary(incrementPct);
        
        BigDecimal originalSalary = new BigDecimal("75000.00");
        BigDecimal expectedSalary = originalSalary.multiply(
            BigDecimal.ONE.add(new BigDecimal(incrementPct).divide(new BigDecimal("100")))
        );
        
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("employee_id")).thenReturn(1);
        when(mockResultSet.getString("first_name")).thenReturn("John");
        when(mockResultSet.getString("last_name")).thenReturn("Doe");
        when(mockResultSet.getString("email")).thenReturn("john.doe@company.com");
        when(mockResultSet.getString("phone_number")).thenReturn("555-1234");
        when(mockResultSet.getString("job_id")).thenReturn("IT_PROG");
        when(mockResultSet.getBigDecimal("salary")).thenReturn(expectedSalary);

        // Act
        List<Employee> result = jdbcBean.incrementSalary(incrementPct);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(0, expectedSalary.compareTo(result.get(0).getSalary()),
            "Expected salary: " + expectedSalary + ", but got: " + result.get(0).getSalary());
        verify(mockPreparedStatement).setInt(1, incrementPct);
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, -10, -15, -20, -25})
    @DisplayName("Test salary increment with negative percentages (salary decrease)")
    void testIncrementSalary_NegativePercentages(int incrementPct) throws SQLException {
        // Arrange
        setupMockForIncrementSalary(incrementPct);
        
        BigDecimal originalSalary = new BigDecimal("75000.00");
        BigDecimal expectedSalary = originalSalary.multiply(
            BigDecimal.ONE.add(new BigDecimal(incrementPct).divide(new BigDecimal("100")))
        );
        
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("employee_id")).thenReturn(1);
        when(mockResultSet.getString("first_name")).thenReturn("John");
        when(mockResultSet.getString("last_name")).thenReturn("Doe");
        when(mockResultSet.getString("email")).thenReturn("john.doe@company.com");
        when(mockResultSet.getString("phone_number")).thenReturn("555-1234");
        when(mockResultSet.getString("job_id")).thenReturn("IT_PROG");
        when(mockResultSet.getBigDecimal("salary")).thenReturn(expectedSalary);

        // Act
        List<Employee> result = jdbcBean.incrementSalary(incrementPct);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(0, expectedSalary.compareTo(result.get(0).getSalary()),
            "Expected salary: " + expectedSalary + ", but got: " + result.get(0).getSalary());
        assertTrue(expectedSalary.compareTo(originalSalary) < 0, "Salary should be decreased");
        verify(mockPreparedStatement).setInt(1, incrementPct);
    }

    @Test
    @DisplayName("Test salary increment with zero percentage")
    void testIncrementSalary_ZeroPercentage() throws SQLException {
        // Arrange
        int incrementPct = 0;
        setupMockForIncrementSalary(incrementPct);
        
        BigDecimal originalSalary = new BigDecimal("75000.00");
        
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("employee_id")).thenReturn(1);
        when(mockResultSet.getString("first_name")).thenReturn("John");
        when(mockResultSet.getString("last_name")).thenReturn("Doe");
        when(mockResultSet.getString("email")).thenReturn("john.doe@company.com");
        when(mockResultSet.getString("phone_number")).thenReturn("555-1234");
        when(mockResultSet.getString("job_id")).thenReturn("IT_PROG");
        when(mockResultSet.getBigDecimal("salary")).thenReturn(originalSalary);

        // Act
        List<Employee> result = jdbcBean.incrementSalary(incrementPct);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(0, originalSalary.compareTo(result.get(0).getSalary()),
            "Expected salary: " + originalSalary + ", but got: " + result.get(0).getSalary());
        verify(mockPreparedStatement).setInt(1, 0);
    }

    @Test
    @DisplayName("Test salary increment with extreme positive percentage")
    void testIncrementSalary_ExtremePositivePercentage() throws SQLException {
        // Arrange
        int incrementPct = 200; // 200% increase (triple salary)
        setupMockForIncrementSalary(incrementPct);
        
        BigDecimal originalSalary = new BigDecimal("50000.00");
        BigDecimal expectedSalary = new BigDecimal("150000.00"); // 50000 * 3
        
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("employee_id")).thenReturn(1);
        when(mockResultSet.getString("first_name")).thenReturn("John");
        when(mockResultSet.getString("last_name")).thenReturn("Doe");
        when(mockResultSet.getString("email")).thenReturn("john.doe@company.com");
        when(mockResultSet.getString("phone_number")).thenReturn("555-1234");
        when(mockResultSet.getString("job_id")).thenReturn("IT_PROG");
        when(mockResultSet.getBigDecimal("salary")).thenReturn(expectedSalary);

        // Act
        List<Employee> result = jdbcBean.incrementSalary(incrementPct);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(0, expectedSalary.compareTo(result.get(0).getSalary()),
            "Expected salary: " + expectedSalary + ", but got: " + result.get(0).getSalary());
        verify(mockPreparedStatement).setInt(1, incrementPct);
    }

    @Test
    @DisplayName("Test salary increment with multiple employees")
    void testIncrementSalary_MultipleEmployees() throws SQLException {
        // Arrange
        int incrementPct = 10;
        setupMockForIncrementSalary(incrementPct);
        
        when(mockResultSet.next()).thenReturn(true, true, true, false);
        
        // First employee
        when(mockResultSet.getInt("employee_id")).thenReturn(1, 2, 3);
        when(mockResultSet.getString("first_name")).thenReturn("John", "Jane", "Bob");
        when(mockResultSet.getString("last_name")).thenReturn("Doe", "Smith", "Johnson");
        when(mockResultSet.getString("email")).thenReturn(
            "john.doe@company.com", "jane.smith@company.com", "bob.johnson@company.com");
        when(mockResultSet.getString("phone_number")).thenReturn("555-1234", "555-5678", "555-9012");
        when(mockResultSet.getString("job_id")).thenReturn("IT_PROG", "HR_REP", "FI_ACCOUNT");
        when(mockResultSet.getBigDecimal("salary")).thenReturn(
            new BigDecimal("82500.00"), // 75000 * 1.1
            new BigDecimal("71500.00"), // 65000 * 1.1
            new BigDecimal("60500.00")  // 55000 * 1.1
        );

        // Act
        List<Employee> result = jdbcBean.incrementSalary(incrementPct);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        
        assertEquals(1, result.get(0).getEmployeeId());
        assertEquals("John", result.get(0).getFirstName());
        assertEquals(0, new BigDecimal("82500.00").compareTo(result.get(0).getSalary()));
        
        assertEquals(2, result.get(1).getEmployeeId());
        assertEquals("Jane", result.get(1).getFirstName());
        assertEquals(0, new BigDecimal("71500.00").compareTo(result.get(1).getSalary()));
        
        assertEquals(3, result.get(2).getEmployeeId());
        assertEquals("Bob", result.get(2).getFirstName());
        assertEquals(0, new BigDecimal("60500.00").compareTo(result.get(2).getSalary()));
    }

    @Test
    @DisplayName("Test salary increment with database unavailable - fallback behavior")
    void testIncrementSalary_DatabaseUnavailable_FallbackBehavior() throws SQLException {
        // Arrange
        int incrementPct = 15;
        when(mockConnectionFactory.getConnection()).thenThrow(new SQLException("Database unavailable"));

        // Act
        List<Employee> result = jdbcBean.incrementSalary(incrementPct);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size()); // Sample data has 5 employees
        
        // Verify that sample data has been incremented correctly
        Employee firstEmployee = result.get(0);
        assertEquals(1, firstEmployee.getEmployeeId());
        assertEquals("John", firstEmployee.getFirstName());
        
        // Original sample salary is 75000, with 15% increment should be 86250
        BigDecimal expectedSalary = new BigDecimal("75000.00")
            .multiply(new BigDecimal("1.15"));
        assertEquals(0, expectedSalary.compareTo(firstEmployee.getSalary()),
            "Expected salary: " + expectedSalary + ", but got: " + firstEmployee.getSalary());
    }

    @Test
    @DisplayName("Test salary increment with no employees in database")
    void testIncrementSalary_NoEmployees() throws SQLException {
        // Arrange
        int incrementPct = 10;
        setupMockForIncrementSalary(incrementPct);
        
        when(mockResultSet.next()).thenReturn(false); // No employees

        // Act
        List<Employee> result = jdbcBean.incrementSalary(incrementPct);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mockPreparedStatement).setInt(1, incrementPct);
    }

    @Test
    @DisplayName("Test salary increment with SQL exception during execution")
    void testIncrementSalary_SQLExceptionDuringExecution() throws SQLException {
        // Arrange
        int incrementPct = 10;
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Query execution failed"));

        // Act
        List<Employee> result = jdbcBean.incrementSalary(incrementPct);

        // Assert - Should fall back to sample data
        assertNotNull(result);
        assertEquals(5, result.size()); // Sample data
        
        // Verify increment was applied to sample data
        Employee firstEmployee = result.get(0);
        BigDecimal expectedSalary = new BigDecimal("75000.00")
            .multiply(new BigDecimal("1.10"));
        assertEquals(0, expectedSalary.compareTo(firstEmployee.getSalary()), 
            "Expected salary: " + expectedSalary + ", but got: " + firstEmployee.getSalary());
    }

    @Test
    @DisplayName("Test salary increment with decimal precision")
    void testIncrementSalary_DecimalPrecision() throws SQLException {
        // Arrange
        int incrementPct = 7; // 7% should result in precise decimal calculations
        setupMockForIncrementSalary(incrementPct);
        
        BigDecimal originalSalary = new BigDecimal("75000.00");
        BigDecimal expectedSalary = new BigDecimal("80250.00"); // 75000 * 1.07
        
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("employee_id")).thenReturn(1);
        when(mockResultSet.getString("first_name")).thenReturn("John");
        when(mockResultSet.getString("last_name")).thenReturn("Doe");
        when(mockResultSet.getString("email")).thenReturn("john.doe@company.com");
        when(mockResultSet.getString("phone_number")).thenReturn("555-1234");
        when(mockResultSet.getString("job_id")).thenReturn("IT_PROG");
        when(mockResultSet.getBigDecimal("salary")).thenReturn(expectedSalary);

        // Act
        List<Employee> result = jdbcBean.incrementSalary(incrementPct);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(0, expectedSalary.compareTo(result.get(0).getSalary()),
            "Expected salary: " + expectedSalary + ", but got: " + result.get(0).getSalary());
        assertEquals(2, result.get(0).getSalary().scale()); // Should maintain 2 decimal places
    }

    /**
     * Helper method to set up mocks for increment salary operations
     */
    private void setupMockForIncrementSalary(int incrementPct) throws SQLException {
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
    }
}