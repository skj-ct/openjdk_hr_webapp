package com.hrapp.jdbc.samples.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class specifically for PostgreSQL ResultSet mapping functionality
 * Verifies that Employee can be properly constructed from PostgreSQL query results
 */
class EmployeePostgreSQLMappingTest {

    @Mock
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testPostgreSQLResultSetMapping() throws SQLException {
        // Given - Mock PostgreSQL ResultSet with column names (not indices)
        when(mockResultSet.getInt("employee_id")).thenReturn(200);
        when(mockResultSet.getString("first_name")).thenReturn("PostgreSQL");
        when(mockResultSet.getString("last_name")).thenReturn("User");
        when(mockResultSet.getString("email")).thenReturn("postgres.user@hrapp.com");
        when(mockResultSet.getString("phone_number")).thenReturn("555-PSQL");
        when(mockResultSet.getString("job_id")).thenReturn("DBA");
        when(mockResultSet.getBigDecimal("salary")).thenReturn(new BigDecimal("95000.50"));

        // When
        Employee employee = new Employee(mockResultSet);

        // Then - Verify all fields are correctly mapped
        assertEquals(200, employee.getEmployeeId());
        assertEquals("PostgreSQL", employee.getFirstName());
        assertEquals("User", employee.getLastName());
        assertEquals("postgres.user@hrapp.com", employee.getEmail());
        assertEquals("555-PSQL", employee.getPhoneNumber());
        assertEquals("DBA", employee.getJobId());
        assertEquals(new BigDecimal("95000.50"), employee.getSalary());

        // Verify ResultSet methods were called with correct column names
        verify(mockResultSet).getInt("employee_id");
        verify(mockResultSet).getString("first_name");
        verify(mockResultSet).getString("last_name");
        verify(mockResultSet).getString("email");
        verify(mockResultSet).getString("phone_number");
        verify(mockResultSet).getString("job_id");
        verify(mockResultSet).getBigDecimal("salary");
    }

    @Test
    void testBigDecimalPrecisionHandling() throws SQLException {
        // Given - Test with high precision salary values
        when(mockResultSet.getInt("employee_id")).thenReturn(201);
        when(mockResultSet.getString("first_name")).thenReturn("Precision");
        when(mockResultSet.getString("last_name")).thenReturn("Test");
        when(mockResultSet.getString("email")).thenReturn("precision@hrapp.com");
        when(mockResultSet.getString("phone_number")).thenReturn("555-PREC");
        when(mockResultSet.getString("job_id")).thenReturn("ANALYST");
        when(mockResultSet.getBigDecimal("salary")).thenReturn(new BigDecimal("123456.789"));

        // When
        Employee employee = new Employee(mockResultSet);

        // Then - Verify BigDecimal precision is maintained
        assertEquals(new BigDecimal("123456.789"), employee.getSalary());
        assertEquals("123456.789", employee.getSalary().toString());
    }

    @Test
    void testNullValueHandling() throws SQLException {
        // Given - Test with null values (except non-nullable fields)
        when(mockResultSet.getInt("employee_id")).thenReturn(202);
        when(mockResultSet.getString("first_name")).thenReturn("Null");
        when(mockResultSet.getString("last_name")).thenReturn("Test");
        when(mockResultSet.getString("email")).thenReturn("null.test@hrapp.com");
        when(mockResultSet.getString("phone_number")).thenReturn(null); // Nullable field
        when(mockResultSet.getString("job_id")).thenReturn("TEMP");
        when(mockResultSet.getBigDecimal("salary")).thenReturn(new BigDecimal("50000.00"));

        // When
        Employee employee = new Employee(mockResultSet);

        // Then - Verify null handling
        assertEquals(202, employee.getEmployeeId());
        assertEquals("Null", employee.getFirstName());
        assertEquals("Test", employee.getLastName());
        assertEquals("null.test@hrapp.com", employee.getEmail());
        assertNull(employee.getPhoneNumber()); // Should handle null gracefully
        assertEquals("TEMP", employee.getJobId());
        assertEquals(new BigDecimal("50000.00"), employee.getSalary());
    }

    @Test
    void testBackwardCompatibilityWithNewMapping() throws SQLException {
        // Given - Create employee from PostgreSQL ResultSet
        when(mockResultSet.getInt("employee_id")).thenReturn(203);
        when(mockResultSet.getString("first_name")).thenReturn("Backward");
        when(mockResultSet.getString("last_name")).thenReturn("Compatible");
        when(mockResultSet.getString("email")).thenReturn("backward@hrapp.com");
        when(mockResultSet.getString("phone_number")).thenReturn("555-BACK");
        when(mockResultSet.getString("job_id")).thenReturn("LEGACY");
        when(mockResultSet.getBigDecimal("salary")).thenReturn(new BigDecimal("70000.00"));

        // When
        Employee employee = new Employee(mockResultSet);

        // Then - Verify both new and old method names work
        assertEquals(203, employee.getEmployeeId());
        assertEquals(203, employee.getEmployee_Id()); // Backward compatibility

        assertEquals("Backward", employee.getFirstName());
        assertEquals("Backward", employee.getFirst_Name()); // Backward compatibility

        assertEquals("Compatible", employee.getLastName());
        assertEquals("Compatible", employee.getLast_Name()); // Backward compatibility

        assertEquals("555-BACK", employee.getPhoneNumber());
        assertEquals("555-BACK", employee.getPhone_Number()); // Backward compatibility

        assertEquals("LEGACY", employee.getJobId());
        assertEquals("LEGACY", employee.getJob_Id()); // Backward compatibility
    }

    @Test
    void testSalaryTypeConversion() {
        // Given - Test both BigDecimal and int salary handling
        Employee employee1 = new Employee();
        Employee employee2 = new Employee();

        // When - Set salary using different types
        employee1.setSalary(new BigDecimal("85000.75"));
        employee2.setSalary(85000); // int version

        // Then - Verify proper type handling
        assertEquals(new BigDecimal("85000.75"), employee1.getSalary());
        assertEquals(BigDecimal.valueOf(85000), employee2.getSalary());
        assertEquals(new BigDecimal("85000"), employee2.getSalary());
    }

    @Test
    void testEmployeeEqualsAndHashCodeWithDifferentSalaryTypes() {
        // Given - Two employees with same ID but different salary representations
        Employee employee1 = new Employee(204, "Same", "Person", "same@hrapp.com", 
                                        "555-SAME", "EQUAL", new BigDecimal("60000.00"));
        Employee employee2 = new Employee(204, "Same", "Person", "same@hrapp.com", 
                                        "555-SAME", "EQUAL", 60000); // int salary

        // Then - Should be equal based on ID
        assertEquals(employee1, employee2);
        assertEquals(employee1.hashCode(), employee2.hashCode());
    }
}