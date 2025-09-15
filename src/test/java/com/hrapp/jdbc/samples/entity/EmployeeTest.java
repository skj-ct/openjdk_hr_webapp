package com.hrapp.jdbc.samples.entity;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for Employee entity class.
 * 
 * This test class provides complete coverage of the Employee entity
 * with proper testing of all constructors, methods, and edge cases
 * following production-grade test practices.
 * 
 * Test Categories:
 * - Constructor validation
 * - Getter/Setter functionality
 * - Backward compatibility
 * - Business logic methods
 * - Equals and HashCode contracts
 * - ToString and JSON functionality
 * - Edge cases and boundary conditions
 * - Input validation and security
 * 
 * @author HR Application Team
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Employee Entity Comprehensive Unit Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmployeeTest {
    
    @Mock
    private ResultSet mockResultSet;
    
    private Employee employee;
    
    @BeforeEach
    void setUp() {
        employee = new Employee(1, "John", "Doe", "john.doe@company.com", 
                               "555-1234", "DEV", new BigDecimal("75000.00"));
    }

    // ========================================
    // CONSTRUCTOR TESTS
    // ========================================
    
    @Test
    @Order(1)
    @DisplayName("Default constructor should initialize with null values")
    void testDefaultConstructor() {
        // Act
        Employee emp = new Employee();
        
        // Assert
        assertNotNull(emp);
        assertNull(emp.getEmployeeId());
        assertNull(emp.getFirstName());
        assertNull(emp.getLastName());
        assertNull(emp.getEmail());
        assertNull(emp.getPhoneNumber());
        assertNull(emp.getJobId());
        assertNull(emp.getSalary());
    }
    
    @Test
    @Order(2)
    @DisplayName("Constructor with BigDecimal salary should initialize correctly")
    void testConstructorWithBigDecimalSalary() {
        // Assert
        assertEquals(Integer.valueOf(1), employee.getEmployeeId());
        assertEquals("John", employee.getFirstName());
        assertEquals("Doe", employee.getLastName());
        assertEquals("john.doe@company.com", employee.getEmail());
        assertEquals("555-1234", employee.getPhoneNumber());
        assertEquals("DEV", employee.getJobId());
        assertEquals(0, new BigDecimal("75000.00").compareTo(employee.getSalary()));
    }
    
    @Test
    @Order(3)
    @DisplayName("Constructor with int salary should convert to BigDecimal")
    void testConstructorWithIntSalary() {
        // Act
        Employee emp = new Employee(2, "Jane", "Smith", "jane.smith@company.com", 
                                   "555-5678", "MGR", 85000);
        
        // Assert
        assertEquals(Integer.valueOf(2), emp.getEmployeeId());
        assertEquals("Jane", emp.getFirstName());
        assertEquals("Smith", emp.getLastName());
        assertEquals("jane.smith@company.com", emp.getEmail());
        assertEquals("555-5678", emp.getPhoneNumber());
        assertEquals("MGR", emp.getJobId());
        assertEquals(0, new BigDecimal("85000").compareTo(emp.getSalary()));
    }

    @Test
    @Order(4)
    @DisplayName("Constructor from ResultSet should map all fields correctly")
    void testConstructorFromResultSet() throws SQLException {
        // Arrange
        when(mockResultSet.getInt("employee_id")).thenReturn(1);
        when(mockResultSet.getString("first_name")).thenReturn("John");
        when(mockResultSet.getString("last_name")).thenReturn("Doe");
        when(mockResultSet.getString("email")).thenReturn("john.doe@example.com");
        when(mockResultSet.getString("phone_number")).thenReturn("555-1234");
        when(mockResultSet.getString("job_id")).thenReturn("IT_PROG");
        when(mockResultSet.getBigDecimal("salary")).thenReturn(new BigDecimal("75000.00"));

        // Act
        Employee emp = new Employee(mockResultSet);

        // Assert
        assertEquals(Integer.valueOf(1), emp.getEmployeeId());
        assertEquals("John", emp.getFirstName());
        assertEquals("Doe", emp.getLastName());
        assertEquals("john.doe@example.com", emp.getEmail());
        assertEquals("555-1234", emp.getPhoneNumber());
        assertEquals("IT_PROG", emp.getJobId());
        assertEquals(0, new BigDecimal("75000.00").compareTo(emp.getSalary()));
    }

    @Test
    @Order(5)
    @DisplayName("Constructor from ResultSet with column index should work")
    void testConstructorFromResultSetWithColumnIndex() throws SQLException {
        // Arrange
        when(mockResultSet.getInt(1)).thenReturn(1);
        when(mockResultSet.getString(2)).thenReturn("John");
        when(mockResultSet.getString(3)).thenReturn("Doe");
        when(mockResultSet.getString(4)).thenReturn("john.doe@example.com");
        when(mockResultSet.getString(5)).thenReturn("555-1234");
        when(mockResultSet.getString(6)).thenReturn("IT_PROG");
        when(mockResultSet.getBigDecimal(7)).thenReturn(new BigDecimal("75000.00"));

        // Act
        Employee emp = new Employee(mockResultSet, true);

        // Assert
        assertEquals(Integer.valueOf(1), emp.getEmployeeId());
        assertEquals("John", emp.getFirstName());
        assertEquals("Doe", emp.getLastName());
        assertEquals("john.doe@example.com", emp.getEmail());
        assertEquals("555-1234", emp.getPhoneNumber());
        assertEquals("IT_PROG", emp.getJobId());
        assertEquals(0, new BigDecimal("75000.00").compareTo(emp.getSalary()));
    }

    // ========================================
    // GETTER AND SETTER TESTS
    // ========================================
    
    @Test
    @Order(10)
    @DisplayName("Setters and getters should work correctly")
    void testSettersAndGetters() {
        // Act
        employee.setEmployeeId(99);
        employee.setFirstName("Updated");
        employee.setLastName("Name");
        employee.setEmail("updated@company.com");
        employee.setPhoneNumber("555-9999");
        employee.setJobId("ADMIN");
        employee.setSalary(new BigDecimal("90000.00"));
        
        // Assert
        assertEquals(Integer.valueOf(99), employee.getEmployeeId());
        assertEquals("Updated", employee.getFirstName());
        assertEquals("Name", employee.getLastName());
        assertEquals("updated@company.com", employee.getEmail());
        assertEquals("555-9999", employee.getPhoneNumber());
        assertEquals("ADMIN", employee.getJobId());
        assertEquals(0, new BigDecimal("90000.00").compareTo(employee.getSalary()));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE})
    @Order(11)
    @DisplayName("Employee ID should handle various integer values")
    void testEmployeeId_VariousValues(int employeeId) {
        // Act
        employee.setEmployeeId(employeeId);

        // Assert
        assertEquals(Integer.valueOf(employeeId), employee.getEmployeeId());
    }

    // ========================================
    // BACKWARD COMPATIBILITY TESTS
    // ========================================
    
    @Test
    @Order(20)
    @DisplayName("Deprecated Oracle-style methods should work correctly")
    void testBackwardCompatibilityMethods() {
        // Assert - Test deprecated Oracle-style getters
        assertEquals(1, employee.getEmployee_Id());
        assertEquals("John", employee.getFirst_Name());
        assertEquals("Doe", employee.getLast_Name());
        assertEquals("555-1234", employee.getPhone_Number());
        assertEquals("DEV", employee.getJob_Id());
        assertEquals(75000, employee.getSalaryAsInt());
        
        // Act - Test deprecated Oracle-style setters
        employee.setEmployee_Id(2);
        employee.setFirst_Name("Jane");
        employee.setLast_Name("Smith");
        employee.setPhone_Number("555-5678");
        employee.setJob_Id("MGR");
        employee.setSalary(85000);
        
        // Assert - Verify new methods also work
        assertEquals(Integer.valueOf(2), employee.getEmployeeId());
        assertEquals("Jane", employee.getFirstName());
        assertEquals("Smith", employee.getLastName());
        assertEquals("555-5678", employee.getPhoneNumber());
        assertEquals("MGR", employee.getJobId());
        assertEquals(new BigDecimal("85000"), employee.getSalary());
    }

    @Test
    @Order(21)
    @DisplayName("Mixed usage of old and new methods should work")
    void testMixedOldNewMethods() {
        // Act - Mix old and new setters
        employee.setEmployeeId(3);
        employee.setFirst_Name("Alice");
        employee.setLastName("Johnson");
        employee.setPhone_Number("555-3456");
        employee.setJobId("FI_ACCOUNT");

        // Assert - Mix old and new getters
        assertEquals(3, employee.getEmployee_Id());
        assertEquals("Alice", employee.getFirstName());
        assertEquals("Johnson", employee.getLast_Name());
        assertEquals("555-3456", employee.getPhoneNumber());
        assertEquals("FI_ACCOUNT", employee.getJob_Id());
    }

    // ========================================
    // BUSINESS LOGIC TESTS
    // ========================================
    
    @Test
    @Order(30)
    @DisplayName("getFullName should concatenate first and last names")
    void testGetFullName() {
        // Assert - Normal case
        assertEquals("John Doe", employee.getFullName());
        
        // Test with null first name
        employee.setFirstName(null);
        assertEquals("Doe", employee.getFullName());
        
        // Test with null last name
        employee.setFirstName("John");
        employee.setLastName(null);
        assertEquals("John", employee.getFullName());
        
        // Test with both null
        employee.setFirstName(null);
        assertNull(employee.getFullName());
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = {" ", "  ", "\t", "\n"})
    @Order(31)
    @DisplayName("getFullName should handle various empty/null values")
    void testGetFullName_EdgeCases(String emptyValue) {
        // Act
        employee.setFirstName(emptyValue);
        employee.setLastName("Doe");

        // Assert
        String result = employee.getFullName();
        if (emptyValue == null) {
            assertEquals("Doe", result);
        } else {
            assertTrue(result.contains("Doe"));
        }
    }
    
    @Test
    @Order(32)
    @DisplayName("isValid should check required fields")
    void testIsValid() {
        // Assert - Valid employee
        assertTrue(employee.isValid());
        
        // Test with null employee ID
        employee.setEmployeeId(null);
        assertFalse(employee.isValid());
        
        // Test with zero employee ID
        employee.setEmployeeId(0);
        assertFalse(employee.isValid());
        
        // Test with negative employee ID
        employee.setEmployeeId(-1);
        assertFalse(employee.isValid());
        
        // Reset and test first name
        employee.setEmployeeId(1);
        employee.setFirstName("");
        assertFalse(employee.isValid());
        
        employee.setFirstName("   ");
        assertFalse(employee.isValid());
        
        // Reset and test email
        employee.setFirstName("John");
        employee.setEmail(null);
        assertFalse(employee.isValid());
        
        employee.setEmail("");
        assertFalse(employee.isValid());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100, 999, Integer.MAX_VALUE})
    @Order(33)
    @DisplayName("isValid should accept valid employee IDs")
    void testIsValid_ValidEmployeeIds(int validId) {
        // Act
        employee.setEmployeeId(validId);

        // Assert
        assertTrue(employee.isValid());
    }
    
    @Test
    @Order(34)
    @DisplayName("applySalaryIncrement with BigDecimal should calculate correctly")
    void testSalaryIncrementWithBigDecimal() {
        // Act
        BigDecimal newSalary = employee.applySalaryIncrement(new BigDecimal("10"));
        
        // Assert
        assertEquals(new BigDecimal("82500.00"), newSalary);
        assertEquals(newSalary, employee.getSalary());
        
        // Test with different percentage
        employee.setSalary(new BigDecimal("100000"));
        BigDecimal incrementedSalary = employee.applySalaryIncrement(new BigDecimal("5"));
        assertEquals(new BigDecimal("105000"), incrementedSalary);
    }

    @Test
    @Order(35)
    @DisplayName("applySalaryIncrement with int should calculate correctly")
    void testSalaryIncrementWithInt() {
        // Act
        BigDecimal newSalary = employee.applySalaryIncrement(10);
        
        // Assert
        assertEquals(new BigDecimal("82500.00"), newSalary);
        assertEquals(newSalary, employee.getSalary());
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 15, 25, 50, -5, -10, 0, 100})
    @Order(36)
    @DisplayName("applySalaryIncrement should handle various percentages")
    void testSalaryIncrement_VariousPercentages(int percentage) {
        // Arrange
        BigDecimal originalSalary = new BigDecimal("100000");
        employee.setSalary(originalSalary);
        
        // Act
        BigDecimal newSalary = employee.applySalaryIncrement(percentage);
        
        // Assert
        BigDecimal expectedMultiplier = BigDecimal.ONE.add(BigDecimal.valueOf(percentage).divide(new BigDecimal("100")));
        BigDecimal expectedSalary = originalSalary.multiply(expectedMultiplier);
        assertEquals(expectedSalary, newSalary);
        assertEquals(newSalary, employee.getSalary());
    }
    
    @Test
    @Order(37)
    @DisplayName("applySalaryIncrement should handle null values")
    void testSalaryIncrementWithNulls() {
        // Test with null salary
        employee.setSalary(null);
        assertNull(employee.applySalaryIncrement(new BigDecimal("10")));
        
        // Test with null percentage
        employee.setSalary(new BigDecimal("50000"));
        assertEquals(new BigDecimal("50000"), employee.applySalaryIncrement((BigDecimal) null));
    }

    // ========================================
    // EQUALS AND HASHCODE TESTS
    // ========================================
    
    @Test
    @Order(40)
    @DisplayName("equals should work based on employee ID and email")
    void testEqualsAndHashCode() {
        // Arrange
        Employee emp1 = new Employee(1, "John", "Doe", "john.doe@company.com", 
                                    "555-1234", "DEV", new BigDecimal("75000"));
        Employee emp2 = new Employee(1, "Jane", "Smith", "john.doe@company.com", 
                                    "555-5678", "MGR", new BigDecimal("85000"));
        Employee emp3 = new Employee(2, "John", "Doe", "jane.smith@company.com", 
                                    "555-1234", "DEV", new BigDecimal("75000"));
        
        // Assert - Same ID and email should be equal
        assertEquals(emp1, emp2);
        assertEquals(emp1.hashCode(), emp2.hashCode());
        
        // Assert - Different ID and email should not be equal
        assertNotEquals(emp1, emp3);
    }

    @Test
    @Order(41)
    @DisplayName("equals should handle null and different class objects")
    void testEquals_NullAndDifferentClass() {
        // Assert
        assertNotEquals(employee, null);
        assertNotEquals(employee, "not an employee");
        assertEquals(employee, employee); // Reflexive
    }

    @Test
    @Order(42)
    @DisplayName("hashCode should be consistent with equals")
    void testHashCode_Consistency() {
        // Arrange
        Employee emp1 = new Employee(5, "Test", "User", "test@example.com", 
                                   "555-0000", "IT_PROG", new BigDecimal("50000.00"));
        Employee emp2 = new Employee(5, "Different", "Name", "test@example.com", 
                                   "555-9999", "HR_REP", new BigDecimal("60000.00"));

        // Assert - Equal objects must have equal hash codes
        assertEquals(emp1, emp2);
        assertEquals(emp1.hashCode(), emp2.hashCode());
    }

    // ========================================
    // STRING REPRESENTATION TESTS
    // ========================================
    
    @Test
    @Order(50)
    @DisplayName("toString should contain all field values")
    void testToString() {
        // Act
        String str = employee.toString();
        
        // Assert
        assertTrue(str.contains("employeeId=1"));
        assertTrue(str.contains("firstName='John'"));
        assertTrue(str.contains("lastName='Doe'"));
        assertTrue(str.contains("email='john.doe@company.com'"));
        assertTrue(str.contains("phoneNumber='555-1234'"));
        assertTrue(str.contains("jobId='DEV'"));
        assertTrue(str.contains("salary=75000.00"));
    }

    @Test
    @Order(51)
    @DisplayName("toString should handle null values")
    void testToString_NullValues() {
        // Arrange
        Employee emp = new Employee(1, null, null, null, null, null, null);

        // Act
        String str = emp.toString();

        // Assert
        assertNotNull(str);
        assertTrue(str.contains("employeeId=1"));
        assertTrue(str.contains("firstName=null"));
        assertTrue(str.contains("lastName=null"));
        assertTrue(str.contains("email=null"));
        assertTrue(str.contains("phoneNumber=null"));
        assertTrue(str.contains("jobId=null"));
        assertTrue(str.contains("salary=null"));
    }
    
    @Test
    @Order(52)
    @DisplayName("toJson should generate valid JSON-like string")
    void testToJson() {
        // Act
        String json = employee.toJson();
        
        // Assert
        assertTrue(json.contains("\"employeeId\":1"));
        assertTrue(json.contains("\"firstName\":\"John\""));
        assertTrue(json.contains("\"lastName\":\"Doe\""));
        assertTrue(json.contains("\"email\":\"john.doe@company.com\""));
        assertTrue(json.contains("\"phoneNumber\":\"555-1234\""));
        assertTrue(json.contains("\"jobId\":\"DEV\""));
        assertTrue(json.contains("\"salary\":75000.00"));
    }

    @Test
    @Order(53)
    @DisplayName("toJson should handle null values")
    void testToJson_NullValues() {
        // Arrange
        Employee emp = new Employee();
        emp.setEmployeeId(1);

        // Act
        String json = emp.toJson();

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"employeeId\":1"));
        assertTrue(json.contains("\"firstName\":null"));
        assertTrue(json.contains("\"lastName\":null"));
    }

    // ========================================
    // UTILITY METHOD TESTS
    // ========================================
    
    @Test
    @Order(60)
    @DisplayName("copy should create identical but separate instance")
    void testCopy() {
        // Act
        Employee copy = employee.copy();
        
        // Assert - Same data
        assertEquals(employee.getEmployeeId(), copy.getEmployeeId());
        assertEquals(employee.getFirstName(), copy.getFirstName());
        assertEquals(employee.getLastName(), copy.getLastName());
        assertEquals(employee.getEmail(), copy.getEmail());
        assertEquals(employee.getPhoneNumber(), copy.getPhoneNumber());
        assertEquals(employee.getJobId(), copy.getJobId());
        assertEquals(employee.getSalary(), copy.getSalary());
        
        // Assert - Different objects
        assertNotSame(employee, copy);
        
        // Assert - Modifications don't affect original
        copy.setFirstName("Modified");
        assertEquals("John", employee.getFirstName());
        assertEquals("Modified", copy.getFirstName());
    }

    @Test
    @Order(61)
    @DisplayName("copy should handle null values")
    void testCopy_NullValues() {
        // Arrange
        Employee emp = new Employee();
        emp.setEmployeeId(1);

        // Act
        Employee copy = emp.copy();

        // Assert
        assertEquals(emp.getEmployeeId(), copy.getEmployeeId());
        assertNull(copy.getFirstName());
        assertNull(copy.getLastName());
        assertNotSame(emp, copy);
    }

    // ========================================
    // EDGE CASES AND BOUNDARY CONDITIONS
    // ========================================

    @Test
    @Order(70)
    @DisplayName("Fields should handle special characters")
    void testSpecialCharacters() {
        // Act
        employee.setFirstName("José-María");
        employee.setLastName("O'Connor-Smith");
        employee.setEmail("josé.maría@company.com");

        // Assert
        assertEquals("José-María", employee.getFirstName());
        assertEquals("O'Connor-Smith", employee.getLastName());
        assertEquals("josé.maría@company.com", employee.getEmail());
    }

    @Test
    @Order(71)
    @DisplayName("Fields should handle maximum length values")
    void testMaximumLengthValues() {
        // Arrange
        String longName = "A".repeat(100);
        String longEmail = "test" + "x".repeat(90) + "@company.com";

        // Act
        employee.setFirstName(longName);
        employee.setLastName(longName);
        employee.setEmail(longEmail);

        // Assert
        assertEquals(longName, employee.getFirstName());
        assertEquals(longName, employee.getLastName());
        assertEquals(longEmail, employee.getEmail());
    }

    @Test
    @Order(72)
    @DisplayName("Salary should handle high precision values")
    void testSalaryPrecision() {
        // Act
        employee.setSalary(new BigDecimal("123456.789"));

        // Assert
        assertEquals(new BigDecimal("123456.789"), employee.getSalary());
    }

    @Test
    @Order(73)
    @DisplayName("Constructor from ResultSet should handle SQLException")
    void testResultSetConstructor_SQLException() throws SQLException {
        // Arrange
        when(mockResultSet.getInt("employee_id")).thenThrow(new SQLException("Database error"));

        // Act & Assert
        assertThrows(SQLException.class, () -> new Employee(mockResultSet));
    }
}