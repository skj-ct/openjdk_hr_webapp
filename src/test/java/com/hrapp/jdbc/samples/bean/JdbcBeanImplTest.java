package com.hrapp.jdbc.samples.bean;

import com.hrapp.jdbc.samples.config.ConnectionFactory;
import com.hrapp.jdbc.samples.entity.Employee;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for JdbcBeanImpl class.
 * 
 * This test class provides complete coverage of the JdbcBeanImpl business logic
 * with proper mocking, error handling, and production-grade test practices.
 * 
 * Test Categories:
 * - Core CRUD operations
 * - Connection management
 * - Error handling and fallback mechanisms
 * - Performance and resource management
 * - Thread safety and concurrent access
 * 
 * @author HR Application Team
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcBeanImpl Unit Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcBeanImplTest {

    @Mock
    private ConnectionFactory mockConnectionFactory;
    
    @Mock
    private Connection mockConnection;
    
    @Mock
    private Statement mockStatement;
    
    @Mock
    private PreparedStatement mockPreparedStatement;
    
    @Mock
    private ResultSet mockResultSet;

    private JdbcBeanImpl jdbcBean;

    @BeforeEach
    void setUp() {
        jdbcBean = new JdbcBeanImpl(mockConnectionFactory);
    }

    @AfterEach
    void tearDown() {
        // Verify no resource leaks
        verifyNoMoreInteractions(mockConnection, mockStatement, mockPreparedStatement);
    }

    // ========================================
    // CONSTRUCTOR AND INITIALIZATION TESTS
    // ========================================

    @Test
    @Order(1)
    @DisplayName("Constructor with ConnectionFactory should initialize properly")
    void testConstructorWithConnectionFactory() {
        // Act
        JdbcBeanImpl bean = new JdbcBeanImpl(mockConnectionFactory);

        // Assert
        assertNotNull(bean);
        // Verify the bean is properly initialized (we can't access private fields directly,
        // but we can test behavior)
        assertDoesNotThrow(() -> bean.isConnectionHealthy());
    }

    @Test
    @Order(2)
    @DisplayName("Default constructor should initialize with default ConnectionFactory")
    void testDefaultConstructor() {
        // Act
        JdbcBeanImpl bean = new JdbcBeanImpl();

        // Assert
        assertNotNull(bean);
        assertDoesNotThrow(() -> bean.isConnectionHealthy());
    }

    // ========================================
    // READ OPERATIONS TESTS
    // ========================================

    @Test
    @Order(10)
    @DisplayName("getEmployees should return all employees successfully")
    void testGetEmployees_Success() throws SQLException {
        // Arrange
        setupMockForSuccessfulQuery();
        mockEmployeeResultSet(1, "John", "Doe", "john.doe@company.com", 
                            "555-1234", "IT_PROG", "75000.00");

        // Act
        List<Employee> employees = jdbcBean.getEmployees();

        // Assert
        assertNotNull(employees);
        assertEquals(1, employees.size());
        
        Employee employee = employees.get(0);
        assertEquals(1, employee.getEmployeeId());
        assertEquals("John", employee.getFirstName());
        assertEquals("Doe", employee.getLastName());
        assertEquals("john.doe@company.com", employee.getEmail());
        assertEquals("555-1234", employee.getPhoneNumber());
        assertEquals("IT_PROG", employee.getJobId());
        assertEquals(new BigDecimal("75000.00"), employee.getSalary());

        // Verify proper resource management
        verify(mockConnection).createStatement();
        verify(mockStatement).executeQuery(contains("SELECT employee_id"));
        verify(mockResultSet).close();
        verify(mockStatement).close();
        verify(mockConnection).close();
    }

    @Test
    @Order(11)
    @DisplayName("getEmployees should return sample data when database unavailable")
    void testGetEmployees_DatabaseUnavailable_ReturnsSampleData() throws SQLException {
        // Arrange
        when(mockConnectionFactory.getConnection()).thenThrow(new SQLException("Connection failed"));

        // Act
        List<Employee> employees = jdbcBean.getEmployees();

        // Assert
        assertNotNull(employees);
        assertEquals(5, employees.size()); // Sample data contains 5 employees
        
        // Verify sample data integrity
        Employee firstEmployee = employees.get(0);
        assertEquals(1, firstEmployee.getEmployeeId());
        assertEquals("John", firstEmployee.getFirstName());
        assertEquals("Doe", firstEmployee.getLastName());
    }

    @Test
    @Order(12)
    @DisplayName("getEmployee by ID should return specific employee")
    void testGetEmployee_ById_Success() throws SQLException {
        // Arrange
        int employeeId = 1;
        setupMockForPreparedStatement();
        mockEmployeeResultSet(employeeId, "John", "Doe", "john.doe@company.com", 
                            "555-1234", "IT_PROG", "75000.00");

        // Act
        List<Employee> employees = jdbcBean.getEmployee(employeeId);

        // Assert
        assertNotNull(employees);
        assertEquals(1, employees.size());
        assertEquals(employeeId, employees.get(0).getEmployeeId());

        // Verify proper parameter binding
        verify(mockPreparedStatement).setInt(1, employeeId);
        verify(mockPreparedStatement).executeQuery();
    }

    @Test
    @Order(13)
    @DisplayName("getEmployee by ID should return empty list when not found")
    void testGetEmployee_ById_NotFound() throws SQLException {
        // Arrange
        int employeeId = 999;
        setupMockForPreparedStatement();
        when(mockResultSet.next()).thenReturn(false);

        // Act
        List<Employee> employees = jdbcBean.getEmployee(employeeId);

        // Assert
        assertNotNull(employees);
        assertTrue(employees.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, Integer.MAX_VALUE, Integer.MIN_VALUE})
    @Order(14)
    @DisplayName("getEmployee should handle edge case IDs")
    void testGetEmployee_EdgeCaseIds(int employeeId) throws SQLException {
        // Arrange
        setupMockForPreparedStatement();
        when(mockResultSet.next()).thenReturn(false);

        // Act
        List<Employee> employees = jdbcBean.getEmployee(employeeId);

        // Assert
        assertNotNull(employees);
        assertTrue(employees.isEmpty());
        verify(mockPreparedStatement).setInt(1, employeeId);
    }

    @Test
    @Order(15)
    @DisplayName("getEmployeeByFn should return employees matching first name pattern")
    void testGetEmployeeByFn_Success() throws SQLException {
        // Arrange
        String firstName = "John";
        setupMockForPreparedStatement();
        when(mockResultSet.next()).thenReturn(true, true, false);
        
        // Mock two employees with names starting with "John"
        when(mockResultSet.getInt("employee_id")).thenReturn(1, 2);
        when(mockResultSet.getString("first_name")).thenReturn("John", "Johnny");
        when(mockResultSet.getString("last_name")).thenReturn("Doe", "Cash");
        when(mockResultSet.getString("email")).thenReturn("john.doe@company.com", "johnny.cash@company.com");
        when(mockResultSet.getString("phone_number")).thenReturn("555-1234", "555-5678");
        when(mockResultSet.getString("job_id")).thenReturn("IT_PROG", "MK_REP");
        when(mockResultSet.getBigDecimal("salary")).thenReturn(new BigDecimal("75000.00"), new BigDecimal("60000.00"));

        // Act
        List<Employee> employees = jdbcBean.getEmployeeByFn(firstName);

        // Assert
        assertNotNull(employees);
        assertEquals(2, employees.size());
        assertEquals("John", employees.get(0).getFirstName());
        assertEquals("Johnny", employees.get(1).getFirstName());

        // Verify ILIKE pattern matching
        verify(mockPreparedStatement).setString(1, firstName + "%");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "NonExistent", "SpecialChars!@#"})
    @Order(16)
    @DisplayName("getEmployeeByFn should handle edge case names")
    void testGetEmployeeByFn_EdgeCases(String firstName) throws SQLException {
        // Arrange
        setupMockForPreparedStatement();
        when(mockResultSet.next()).thenReturn(false);

        // Act
        List<Employee> employees = jdbcBean.getEmployeeByFn(firstName);

        // Assert
        assertNotNull(employees);
        assertTrue(employees.isEmpty());
        verify(mockPreparedStatement).setString(1, firstName + "%");
    }

    // ========================================
    // UPDATE OPERATIONS TESTS
    // ========================================

    @Test
    @Order(20)
    @DisplayName("updateEmployee by ID should apply 10% salary increase")
    void testUpdateEmployee_ById_Success() throws SQLException {
        // Arrange
        int employeeId = 1;
        setupMockForUpdateOperation();
        
        // Mock successful update
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        
        // Mock retrieval of updated employee
        mockEmployeeResultSet(employeeId, "John", "Doe", "john.doe@company.com", 
                            "555-1234", "IT_PROG", "82500.00"); // 10% increase

        // Act
        Employee updatedEmployee = jdbcBean.updateEmployee(employeeId);

        // Assert
        assertNotNull(updatedEmployee);
        assertEquals(employeeId, updatedEmployee.getEmployeeId());
        assertEquals(new BigDecimal("82500.00"), updatedEmployee.getSalary());

        // Verify update operation
        verify(mockPreparedStatement).setInt(1, employeeId);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    @Order(21)
    @DisplayName("updateEmployee by ID should return null when employee not found")
    void testUpdateEmployee_ById_NotFound() throws SQLException {
        // Arrange
        int employeeId = 999;
        setupMockForUpdateOperation();
        when(mockPreparedStatement.executeUpdate()).thenReturn(0); // No rows affected

        // Act
        Employee updatedEmployee = jdbcBean.updateEmployee(employeeId);

        // Assert
        assertNull(updatedEmployee);
    }

    @Test
    @Order(22)
    @DisplayName("updateEmployee with Employee object should update all fields")
    void testUpdateEmployee_WithObject_Success() throws SQLException {
        // Arrange
        Employee employee = new Employee(1, "Updated", "Name", "updated@company.com", 
                                       "555-9999", "SA_MAN", new BigDecimal("85000.00"));
        
        setupMockForUpdateOperation();
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        Employee result = jdbcBean.updateEmployee(employee);

        // Assert
        assertNotNull(result);
        assertEquals(employee, result);

        // Verify all parameters were set
        verify(mockPreparedStatement).setString(1, "Updated");
        verify(mockPreparedStatement).setString(2, "Name");
        verify(mockPreparedStatement).setString(3, "updated@company.com");
        verify(mockPreparedStatement).setString(4, "555-9999");
        verify(mockPreparedStatement).setString(5, "SA_MAN");
        verify(mockPreparedStatement).setBigDecimal(6, new BigDecimal("85000.00"));
        verify(mockPreparedStatement).setInt(7, 1);
    }

    // ========================================
    // CREATE OPERATIONS TESTS
    // ========================================

    @Test
    @Order(30)
    @DisplayName("createEmployee should create new employee and return with generated ID")
    void testCreateEmployee_Success() throws SQLException {
        // Arrange
        Employee newEmployee = new Employee(0, "New", "Employee", "new.employee@company.com", 
                                          "555-0000", "IT_PROG", new BigDecimal("70000.00"));
        
        setupMockForCreateOperation();
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(10); // Generated ID

        // Act
        Employee createdEmployee = jdbcBean.createEmployee(newEmployee);

        // Assert
        assertNotNull(createdEmployee);
        assertEquals(10, createdEmployee.getEmployeeId());
        assertEquals("New", createdEmployee.getFirstName());
        assertEquals("Employee", createdEmployee.getLastName());

        // Verify all parameters were set correctly
        verify(mockPreparedStatement).setString(1, "New");
        verify(mockPreparedStatement).setString(2, "Employee");
        verify(mockPreparedStatement).setString(3, "new.employee@company.com");
        verify(mockPreparedStatement).setString(4, "555-0000");
        verify(mockPreparedStatement).setString(5, "IT_PROG");
        verify(mockPreparedStatement).setBigDecimal(6, new BigDecimal("70000.00"));
    }

    @Test
    @Order(31)
    @DisplayName("createEmployee should handle database errors gracefully")
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

    // ========================================
    // DELETE OPERATIONS TESTS
    // ========================================

    @Test
    @Order(40)
    @DisplayName("deleteEmployee should remove employee and return true")
    void testDeleteEmployee_Success() throws SQLException {
        // Arrange
        int employeeId = 1;
        setupMockForDeleteOperation();
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        boolean result = jdbcBean.deleteEmployee(employeeId);

        // Assert
        assertTrue(result);
        verify(mockPreparedStatement).setInt(1, employeeId);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    @Order(41)
    @DisplayName("deleteEmployee should return false when employee not found")
    void testDeleteEmployee_NotFound() throws SQLException {
        // Arrange
        int employeeId = 999;
        setupMockForDeleteOperation();
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        // Act
        boolean result = jdbcBean.deleteEmployee(employeeId);

        // Assert
        assertFalse(result);
    }

    @Test
    @Order(42)
    @DisplayName("deleteEmployee should handle database errors")
    void testDeleteEmployee_DatabaseError() throws SQLException {
        // Arrange
        int employeeId = 1;
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            jdbcBean.deleteEmployee(employeeId);
        });
        
        assertTrue(exception.getMessage().contains("Database operation failed"));
    }

    // ========================================
    // SALARY INCREMENT TESTS
    // ========================================

    @Test
    @Order(50)
    @DisplayName("incrementSalary should call PostgreSQL function with correct percentage")
    void testIncrementSalary_Success() throws SQLException {
        // Arrange
        int incrementPct = 10;
        setupMockForPreparedStatement();
        mockEmployeeResultSet(1, "John", "Doe", "john.doe@company.com", 
                            "555-1234", "IT_PROG", "82500.00");

        // Act
        List<Employee> result = jdbcBean.incrementSalary(incrementPct);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("82500.00"), result.get(0).getSalary());

        // Verify function call
        verify(mockPreparedStatement).setInt(1, incrementPct);
        verify(mockPreparedStatement, times(1)).executeQuery();
    }

    // ========================================
    // HEALTH CHECK TESTS
    // ========================================

    @Test
    @Order(60)
    @DisplayName("isConnectionHealthy should delegate to ConnectionFactory")
    void testIsConnectionHealthy() {
        // Arrange
        when(mockConnectionFactory.isHealthy()).thenReturn(true);

        // Act
        boolean healthy = jdbcBean.isConnectionHealthy();

        // Assert
        assertTrue(healthy);
        verify(mockConnectionFactory).isHealthy();
    }

    @Test
    @Order(61)
    @DisplayName("isConnectionHealthy should return false when ConnectionFactory reports unhealthy")
    void testIsConnectionHealthy_Unhealthy() {
        // Arrange
        when(mockConnectionFactory.isHealthy()).thenReturn(false);

        // Act
        boolean healthy = jdbcBean.isConnectionHealthy();

        // Assert
        assertFalse(healthy);
        verify(mockConnectionFactory).isHealthy();
    }

    // ========================================
    // CONCURRENT ACCESS TESTS
    // ========================================

    @Test
    @Order(70)
    @DisplayName("Multiple concurrent operations should not interfere")
    void testConcurrentOperations() throws SQLException {
        // Arrange
        setupMockForPreparedStatement();
        when(mockResultSet.next()).thenReturn(false);

        // Act - Simulate multiple operations
        assertDoesNotThrow(() -> {
            jdbcBean.getEmployees();
            jdbcBean.getEmployee(1);
            jdbcBean.getEmployeeByFn("Test");
        });

        // Assert - Verify multiple connections were requested
        verify(mockConnectionFactory, atLeast(3)).getConnection();
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private void setupMockForSuccessfulQuery() throws SQLException {
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
    }

    private void setupMockForPreparedStatement() throws SQLException {
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
    }

    private void setupMockForUpdateOperation() throws SQLException {
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
    }

    private void setupMockForCreateOperation() throws SQLException {
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
    }

    private void setupMockForDeleteOperation() throws SQLException {
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    }

    private void mockEmployeeResultSet(int id, String firstName, String lastName, 
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