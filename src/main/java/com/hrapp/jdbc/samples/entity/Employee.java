package com.hrapp.jdbc.samples.entity;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Employee entity class for the HR Web Application.
 * Migrated from Oracle-specific implementation to PostgreSQL-compatible version.
 * 
 * Key changes from Oracle version:
 * - Updated package from com.oracle.jdbc.samples to com.hrapp.jdbc.samples
 * - Changed field names to camelCase convention while maintaining backward compatibility
 * - Changed salary field from int to BigDecimal for better precision
 * - Added comprehensive constructors and utility methods
 * - Enhanced PostgreSQL ResultSet mapping support
 * 
 * @author HR Application Team (migrated from Oracle implementation)
 */
public class Employee {
    
    // Primary fields using camelCase convention
    private Integer employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String jobId;
    private BigDecimal salary;
    
    /**
     * Default constructor
     */
    public Employee() {
    }
    
    /**
     * Constructor from PostgreSQL ResultSet
     * Maps ResultSet columns to Employee fields
     * 
     * @param resultSet PostgreSQL ResultSet containing employee data
     * @throws SQLException if ResultSet access fails
     */
    public Employee(ResultSet resultSet) throws SQLException {
        this.employeeId = resultSet.getInt("employee_id");
        this.firstName = resultSet.getString("first_name");
        this.lastName = resultSet.getString("last_name");
        this.email = resultSet.getString("email");
        this.phoneNumber = resultSet.getString("phone_number");
        this.jobId = resultSet.getString("job_id");
        this.salary = resultSet.getBigDecimal("salary");
    }
    
    /**
     * Constructor from ResultSet using column indices (for backward compatibility)
     * 
     * @param resultSet ResultSet containing employee data
     * @param useColumnIndex if true, use column indices; if false, use column names
     * @throws SQLException if ResultSet access fails
     */
    public Employee(ResultSet resultSet, boolean useColumnIndex) throws SQLException {
        if (useColumnIndex) {
            this.employeeId = resultSet.getInt(1);
            this.firstName = resultSet.getString(2);
            this.lastName = resultSet.getString(3);
            this.email = resultSet.getString(4);
            this.phoneNumber = resultSet.getString(5);
            this.jobId = resultSet.getString(6);
            this.salary = resultSet.getBigDecimal(7);
        } else {
            // Use column names
            this.employeeId = resultSet.getInt("employee_id");
            this.firstName = resultSet.getString("first_name");
            this.lastName = resultSet.getString("last_name");
            this.email = resultSet.getString("email");
            this.phoneNumber = resultSet.getString("phone_number");
            this.jobId = resultSet.getString("job_id");
            this.salary = resultSet.getBigDecimal("salary");
        }
    }
    
    /**
     * Full constructor for creating Employee instances
     * 
     * @param employeeId Employee ID
     * @param firstName First name
     * @param lastName Last name
     * @param email Email address
     * @param phoneNumber Phone number
     * @param jobId Job ID
     * @param salary Salary as BigDecimal
     */
    public Employee(Integer employeeId, String firstName, String lastName, 
                   String email, String phoneNumber, String jobId, BigDecimal salary) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.jobId = jobId;
        this.salary = salary;
    }
    
    /**
     * Constructor with int salary for backward compatibility
     * 
     * @param employeeId Employee ID
     * @param firstName First name
     * @param lastName Last name
     * @param email Email address
     * @param phoneNumber Phone number
     * @param jobId Job ID
     * @param salary Salary as int (converted to BigDecimal)
     */
    public Employee(int employeeId, String firstName, String lastName, 
                   String email, String phoneNumber, String jobId, int salary) {
        this(employeeId, firstName, lastName, email, phoneNumber, jobId, 
             BigDecimal.valueOf(salary));
    }
    
    // Getters and Setters with camelCase naming
    
    public Integer getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getJobId() {
        return jobId;
    }
    
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
    
    public BigDecimal getSalary() {
        return salary;
    }
    
    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }
    
    // Backward compatibility methods (Oracle-style naming)
    
    /**
     * @deprecated Use getEmployeeId() instead
     */
    @Deprecated
    public int getEmployee_Id() {
        return employeeId != null ? employeeId : 0;
    }
    
    /**
     * @deprecated Use setEmployeeId() instead
     */
    @Deprecated
    public void setEmployee_Id(int employeeId) {
        this.employeeId = employeeId;
    }
    
    /**
     * @deprecated Use getFirstName() instead
     */
    @Deprecated
    public String getFirst_Name() {
        return firstName;
    }
    
    /**
     * @deprecated Use setFirstName() instead
     */
    @Deprecated
    public void setFirst_Name(String firstName) {
        this.firstName = firstName;
    }
    
    /**
     * @deprecated Use getLastName() instead
     */
    @Deprecated
    public String getLast_Name() {
        return lastName;
    }
    
    /**
     * @deprecated Use setLastName() instead
     */
    @Deprecated
    public void setLast_Name(String lastName) {
        this.lastName = lastName;
    }
    
    /**
     * @deprecated Use getPhoneNumber() instead
     */
    @Deprecated
    public String getPhone_Number() {
        return phoneNumber;
    }
    
    /**
     * @deprecated Use setPhoneNumber() instead
     */
    @Deprecated
    public void setPhone_Number(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    /**
     * @deprecated Use getJobId() instead
     */
    @Deprecated
    public String getJob_Id() {
        return jobId;
    }
    
    /**
     * @deprecated Use setJobId() instead
     */
    @Deprecated
    public void setJob_Id(String jobId) {
        this.jobId = jobId;
    }
    
    /**
     * Get salary as int for backward compatibility
     * @deprecated Use getSalary() which returns BigDecimal instead
     */
    @Deprecated
    public int getSalaryAsInt() {
        return salary != null ? salary.intValue() : 0;
    }
    
    /**
     * Set salary from int for backward compatibility
     * @deprecated Use setSalary(BigDecimal) instead
     */
    @Deprecated
    public void setSalary(int salary) {
        this.salary = BigDecimal.valueOf(salary);
    }
    
    // Utility methods
    
    /**
     * Get full name (first + last)
     * 
     * @return Full name as "firstName lastName"
     */
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
    
    /**
     * Check if employee has valid data
     * 
     * @return true if employee has minimum required fields
     */
    public boolean isValid() {
        return employeeId != null && employeeId > 0 
               && firstName != null && !firstName.trim().isEmpty()
               && lastName != null && !lastName.trim().isEmpty()
               && email != null && !email.trim().isEmpty();
    }
    
    /**
     * Apply salary increment by percentage
     * 
     * @param percentage Increment percentage (e.g., 10 for 10%)
     * @return New salary after increment
     */
    public BigDecimal applySalaryIncrement(BigDecimal percentage) {
        if (salary == null || percentage == null) {
            return salary;
        }
        
        BigDecimal increment = salary.multiply(percentage).divide(BigDecimal.valueOf(100));
        BigDecimal newSalary = salary.add(increment);
        setSalary(newSalary);
        return newSalary;
    }
    
    /**
     * Apply salary increment by percentage (int version for compatibility)
     * 
     * @param percentage Increment percentage as int
     * @return New salary after increment
     */
    public BigDecimal applySalaryIncrement(int percentage) {
        return applySalaryIncrement(BigDecimal.valueOf(percentage));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Employee employee = (Employee) obj;
        return Objects.equals(employeeId, employee.employeeId) &&
               Objects.equals(email, employee.email);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(employeeId, email);
    }
    
    @Override
    public String toString() {
        return "Employee{" +
               "employeeId=" + employeeId +
               ", firstName='" + firstName + '\'' +
               ", lastName='" + lastName + '\'' +
               ", email='" + email + '\'' +
               ", phoneNumber='" + phoneNumber + '\'' +
               ", jobId='" + jobId + '\'' +
               ", salary=" + salary +
               '}';
    }
    
    /**
     * Create a copy of this employee
     * 
     * @return New Employee instance with same data
     */
    public Employee copy() {
        return new Employee(employeeId, firstName, lastName, email, phoneNumber, jobId, salary);
    }
    
    /**
     * Convert to JSON-like string representation
     * 
     * @return JSON-style string
     */
    public String toJson() {
        return "{" +
               "\"employeeId\":" + employeeId + "," +
               "\"firstName\":\"" + firstName + "\"," +
               "\"lastName\":\"" + lastName + "\"," +
               "\"email\":\"" + email + "\"," +
               "\"phoneNumber\":\"" + phoneNumber + "\"," +
               "\"jobId\":\"" + jobId + "\"," +
               "\"salary\":" + salary +
               "}";
    }
}