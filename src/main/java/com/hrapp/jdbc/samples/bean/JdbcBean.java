/*
 * JdbcBean interface for HR Web Application
 * 
 * This interface defines the contract for database operations on Employee entities.
 * Updated for PostgreSQL compatibility and enhanced functionality.
 * 
 * Original author: nirmala.sundarappa@oracle.com
 * Migration author: OpenJDK Migration Team
 */
package com.hrapp.jdbc.samples.bean;

import java.util.List;
import com.hrapp.jdbc.samples.entity.Employee;

/**
 * Interface defining database operations for Employee management.
 * 
 * This interface provides methods for CRUD operations on Employee entities,
 * supporting both read-only access (for HRStaff) and full access (for HRAdmin).
 * 
 * All methods are designed to work with PostgreSQL database backend and
 * HikariCP connection pooling.
 */
public interface JdbcBean {
    
    /**
     * Get a list of all Employees from the database.
     * 
     * @return List of all employees, empty list if no employees found
     * @throws RuntimeException if database operation fails
     */
    public List<Employee> getEmployees();

    /**
     * Get List of employee based on empId. This will always return one row
     * but returning a List to make signatures consistent with other methods.
     * 
     * @param empId Employee ID to search for
     * @return List containing the employee with the specified ID, empty list if not found
     * @throws RuntimeException if database operation fails
     */
    public List<Employee> getEmployee(int empId);

    /**
     * Update employee based on employee-id. Returns the updated record.
     * 
     * Note: This method signature is maintained for compatibility with the original
     * implementation. The actual update parameters are expected to be handled
     * through the Employee object state or additional context.
     * 
     * @param empId Employee ID to update
     * @return Updated employee record
     * @throws RuntimeException if database operation fails or employee not found
     */
    public Employee updateEmployee(int empId);

    /**
     * Get List of employees by First Name pattern.
     * 
     * Searches for employees whose first name starts with the given pattern.
     * 
     * @param fn First name pattern to search for
     * @return List of employees with first names matching the given pattern
     * @throws RuntimeException if database operation fails
     */
    public List<Employee> getEmployeeByFn(String fn);

    /**
     * Increment salary by a percentage for all employees.
     * 
     * This method applies a percentage increase or decrease to all employee salaries.
     * Positive values increase salaries, negative values decrease them.
     * 
     * @param incrementPct Percentage to increment (positive) or decrement (negative)
     * @return List of all employees with updated salaries
     * @throws RuntimeException if database operation fails
     */
    public List<Employee> incrementSalary(int incrementPct);

    /**
     * Delete employee based on employee ID.
     * 
     * This method provides the delete functionality required by HRAdmin users.
     * It removes the employee record from the database permanently.
     * 
     * @param empId Employee ID to delete
     * @return true if employee was successfully deleted, false if employee not found
     * @throws RuntimeException if database operation fails
     */
    public boolean deleteEmployee(int empId);

    /**
     * Create a new employee record in the database.
     * 
     * This method adds enhanced functionality for creating new employee records,
     * which may be needed for complete CRUD operations.
     * 
     * @param employee Employee object containing the data to insert
     * @return The created employee with generated ID, or null if creation failed
     * @throws RuntimeException if database operation fails
     */
    public Employee createEmployee(Employee employee);

    /**
     * Update an existing employee record with new data.
     * 
     * This method provides a more comprehensive update functionality compared
     * to updateEmployee(int), allowing full control over which fields to update.
     * 
     * @param employee Employee object containing updated data (must have valid ID)
     * @return The updated employee record, or null if employee not found
     * @throws RuntimeException if database operation fails
     */
    public Employee updateEmployee(Employee employee);

    /**
     * Check if the database connection is healthy and available.
     * 
     * This method provides a way to verify database connectivity, which is
     * useful for health checks and monitoring in the PostgreSQL environment.
     * 
     * @return true if database connection is healthy, false otherwise
     */
    public boolean isConnectionHealthy();
}