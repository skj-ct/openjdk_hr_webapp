package com.hrapp.jdbc.samples;

import com.hrapp.jdbc.samples.entity.Employee;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Factory class for creating test data objects.
 * 
 * This class provides standardized test data creation methods
 * to ensure consistency across all test classes.
 * 
 * Features:
 * - Standardized test employee creation
 * - Realistic test data values
 * - Edge case test data
 * - Bulk test data generation
 * 
 * @author HR Application Team
 * @version 1.0
 * @since 1.0
 */
public class TestDataFactory {
    
    // Standard test employees
    public static final Employee JOHN_DOE = createEmployee(1, "John", "Doe", "john.doe@company.com", 
                                                          "555-1234", "IT_PROG", "75000.00");
    
    public static final Employee JANE_SMITH = createEmployee(2, "Jane", "Smith", "jane.smith@company.com", 
                                                           "555-5678", "HR_REP", "65000.00");
    
    public static final Employee BOB_JOHNSON = createEmployee(3, "Bob", "Johnson", "bob.johnson@company.com", 
                                                            "555-9012", "FI_ACCOUNT", "55000.00");
    
    public static final Employee ALICE_WILLIAMS = createEmployee(4, "Alice", "Williams", "alice.williams@company.com", 
                                                               "555-3456", "IT_PROG", "80000.00");
    
    public static final Employee CHARLIE_BROWN = createEmployee(5, "Charlie", "Brown", "charlie.brown@company.com", 
                                                              "555-7890", "MK_REP", "60000.00");
    
    /**
     * Creates a standard test employee with the given parameters
     */
    public static Employee createEmployee(int id, String firstName, String lastName, 
                                        String email, String phone, String jobId, String salary) {
        return new Employee(id, firstName, lastName, email, phone, jobId, new BigDecimal(salary));
    }
    
    /**
     * Creates a test employee with int salary (for backward compatibility testing)
     */
    public static Employee createEmployeeWithIntSalary(int id, String firstName, String lastName, 
                                                     String email, String phone, String jobId, int salary) {
        return new Employee(id, firstName, lastName, email, phone, jobId, salary);
    }
    
    /**
     * Creates an employee with minimal valid data
     */
    public static Employee createMinimalEmployee() {
        return new Employee(1, "A", "B", "a@b.c", "1", "J", new BigDecimal("0.01"));
    }
    
    /**
     * Creates an employee with maximum field lengths
     */
    public static Employee createMaximalEmployee() {
        return new Employee(
            Integer.MAX_VALUE,
            "A".repeat(50),
            "B".repeat(50),
            "test" + "x".repeat(90) + "@company.com",
            "1".repeat(20),
            "J".repeat(10),
            new BigDecimal("999999.99")
        );
    }
    
    /**
     * Creates an employee with null values (except ID)
     */
    public static Employee createEmployeeWithNulls() {
        Employee emp = new Employee();
        emp.setEmployeeId(1);
        return emp;
    }
    
    /**
     * Creates an employee with special characters
     */
    public static Employee createEmployeeWithSpecialCharacters() {
        return new Employee(1, "José-María", "O'Connor-Smith", "jose.maria@company.com", 
                          "+1-555-123-4567", "IT_PROG", new BigDecimal("75000.00"));
    }
    
    /**
     * Creates an employee with Unicode characters
     */
    public static Employee createEmployeeWithUnicode() {
        return new Employee(1, "张三", "李四", "zhang.san@company.com", 
                          "555-1234", "IT_PROG", new BigDecimal("75000.00"));
    }
    
    /**
     * Creates a list of standard test employees
     */
    public static List<Employee> createStandardEmployeeList() {
        return Arrays.asList(JOHN_DOE, JANE_SMITH, BOB_JOHNSON, ALICE_WILLIAMS, CHARLIE_BROWN);
    }
    
    /**
     * Creates a list of employees for salary increment testing
     */
    public static List<Employee> createSalaryIncrementTestEmployees() {
        return Arrays.asList(
            createEmployee(1, "Test1", "User1", "test1@company.com", "555-0001", "IT_PROG", "50000.00"),
            createEmployee(2, "Test2", "User2", "test2@company.com", "555-0002", "HR_REP", "60000.00"),
            createEmployee(3, "Test3", "User3", "test3@company.com", "555-0003", "FI_ACCOUNT", "70000.00")
        );
    }
    
    /**
     * Creates employees with incremented salaries for testing
     */
    public static List<Employee> createIncrementedSalaryEmployees(int percentage) {
        BigDecimal multiplier = BigDecimal.ONE.add(new BigDecimal(percentage).divide(new BigDecimal("100")));
        
        return Arrays.asList(
            createEmployee(1, "Test1", "User1", "test1@company.com", "555-0001", "IT_PROG", 
                         new BigDecimal("50000.00").multiply(multiplier).toString()),
            createEmployee(2, "Test2", "User2", "test2@company.com", "555-0002", "HR_REP", 
                         new BigDecimal("60000.00").multiply(multiplier).toString()),
            createEmployee(3, "Test3", "User3", "test3@company.com", "555-0003", "FI_ACCOUNT", 
                         new BigDecimal("70000.00").multiply(multiplier).toString())
        );
    }
    
    /**
     * Creates an employee for CRUD testing
     */
    public static Employee createCrudTestEmployee() {
        return createEmployee(999, "CRUD", "Test", "crud.test@company.com", 
                            "555-CRUD", "TEST_JOB", "50000.00");
    }
    
    /**
     * Creates an updated version of an employee for update testing
     */
    public static Employee createUpdatedEmployee(Employee original) {
        return new Employee(
            original.getEmployeeId(),
            "Updated" + original.getFirstName(),
            "Updated" + original.getLastName(),
            "updated." + original.getEmail(),
            "555-UPDT",
            "UPD_JOB",
            original.getSalary().add(new BigDecimal("1000.00"))
        );
    }
    
    /**
     * Creates employees for concurrent testing
     */
    public static List<Employee> createConcurrentTestEmployees(int count) {
        List<Employee> employees = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            employees.add(createEmployee(
                i,
                "Concurrent" + i,
                "Test" + i,
                "concurrent" + i + "@company.com",
                "555-" + String.format("%04d", i),
                "TEST_JOB",
                "50000.00"
            ));
        }
        return employees;
    }
    
    /**
     * Creates employees for performance testing
     */
    public static List<Employee> createPerformanceTestEmployees(int count) {
        List<Employee> employees = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            employees.add(createEmployee(
                i,
                "Perf" + i,
                "Test" + i,
                "perf" + i + "@company.com",
                "555-" + String.format("%04d", i),
                i % 2 == 0 ? "IT_PROG" : "HR_REP",
                String.valueOf(50000 + (i * 1000))
            ));
        }
        return employees;
    }
    
    /**
     * Creates an employee copy for testing immutability
     */
    public static Employee copyEmployee(Employee original) {
        return new Employee(
            original.getEmployeeId(),
            original.getFirstName(),
            original.getLastName(),
            original.getEmail(),
            original.getPhoneNumber(),
            original.getJobId(),
            original.getSalary()
        );
    }
}