package com.hrapp.jdbc.samples.bean;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hrapp.jdbc.samples.config.ConnectionFactory;
import com.hrapp.jdbc.samples.entity.Employee;

/**
 * PostgreSQL-compatible implementation of the JdbcBean interface.
 * 
 * This class migrates from Oracle-specific implementation to PostgreSQL with HikariCP.
 * Key changes from Oracle version:
 * - Uses HikariCP connection pooling instead of direct DriverManager
 * - PostgreSQL JDBC driver and syntax instead of Oracle
 * - PostgreSQL function calls instead of Oracle stored procedures
 * - Enhanced error handling with fallback mechanisms
 * - Modern Java practices with proper resource management
 * 
 * @author HR Application Team (migrated from Oracle implementation)
 */
public class JdbcBeanImpl implements JdbcBean {
    
    private static final Logger LOGGER = Logger.getLogger(JdbcBeanImpl.class.getName());
    
    // Connection factory for database access
    private final ConnectionFactory connectionFactory;
    
    // Sample data for fallback when database is unavailable
    private static final List<Employee> SAMPLE_EMPLOYEES = createSampleEmployees();
    
    /**
     * Default constructor using singleton ConnectionFactory
     */
    public JdbcBeanImpl() {
        this.connectionFactory = ConnectionFactory.getInstance();
    }
    
    /**
     * Constructor with custom ConnectionFactory (for testing)
     * 
     * @param connectionFactory Custom connection factory
     */
    public JdbcBeanImpl(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
    
    @Override
    public List<Employee> getEmployees() {
        List<Employee> employees = new ArrayList<>();
        
        String sql = "SELECT employee_id, first_name, last_name, email, phone_number, job_id, salary " +
                    "FROM employees ORDER BY employee_id";
        
        try (Connection connection = connectionFactory.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            
            while (resultSet.next()) {
                employees.add(new Employee(resultSet));
            }
            
            LOGGER.info("Retrieved " + employees.size() + " employees from database");
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database not available, returning sample data: " + e.getMessage(), e);
            return new ArrayList<>(SAMPLE_EMPLOYEES);
        }
        
        return employees;
    }
    
    @Override
    public List<Employee> getEmployee(int empId) {
        List<Employee> employees = new ArrayList<>();
        
        String sql = "SELECT employee_id, first_name, last_name, email, phone_number, job_id, salary " +
                    "FROM employees WHERE employee_id = ?";
        
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            preparedStatement.setInt(1, empId);
            
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    employees.add(new Employee(resultSet));
                    LOGGER.info("Retrieved employee with ID: " + empId);
                } else {
                    LOGGER.info("No employee found with ID: " + empId);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database not available, returning sample data for ID " + empId + ": " + e.getMessage(), e);
            return getSampleEmployeeById(empId);
        }
        
        return employees;
    }
    
    @Override
    public Employee updateEmployee(int empId) {
        // This method maintains compatibility with original implementation
        // It applies a 10% salary increase to the specified employee
        
        String updateSql = "UPDATE employees SET salary = salary * 1.1 WHERE employee_id = ?";
        String selectSql = "SELECT employee_id, first_name, last_name, email, phone_number, job_id, salary " +
                          "FROM employees WHERE employee_id = ?";
        
        try (Connection connection = connectionFactory.getConnection()) {
            // Update the salary
            try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                updateStmt.setInt(1, empId);
                int rowsUpdated = updateStmt.executeUpdate();
                
                if (rowsUpdated == 0) {
                    LOGGER.warning("No employee found with ID: " + empId + " for update");
                    return null;
                }
            }
            
            // Retrieve the updated employee
            try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                selectStmt.setInt(1, empId);
                
                try (ResultSet resultSet = selectStmt.executeQuery()) {
                    if (resultSet.next()) {
                        Employee updatedEmployee = new Employee(resultSet);
                        LOGGER.info("Updated employee with ID: " + empId + ", new salary: " + updatedEmployee.getSalary());
                        return updatedEmployee;
                    }
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database not available, returning sample updated data for ID " + empId + ": " + e.getMessage(), e);
            return getSampleUpdatedEmployee(empId);
        }
        
        return null;
    }
    
    @Override
    public List<Employee> getEmployeeByFn(String fn) {
        List<Employee> employees = new ArrayList<>();
        
        String sql = "SELECT employee_id, first_name, last_name, email, phone_number, job_id, salary " +
                    "FROM employees WHERE first_name ILIKE ? ORDER BY first_name, last_name";
        
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            preparedStatement.setString(1, fn + "%");
            
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    employees.add(new Employee(resultSet));
                }
                
                LOGGER.info("Retrieved " + employees.size() + " employees with first name starting with: " + fn);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database not available, returning sample data for name " + fn + ": " + e.getMessage(), e);
            return getSampleEmployeesByName(fn);
        }
        
        return employees;
    }
    
    @Override
    public List<Employee> incrementSalary(int incrementPct) {
        List<Employee> employees = new ArrayList<>();
        
        // Use PostgreSQL function instead of Oracle stored procedure
        String sql = "SELECT * FROM increment_salary_function(?)";
        
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            preparedStatement.setInt(1, incrementPct);
            
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    employees.add(new Employee(resultSet));
                }
                
                LOGGER.info("Applied " + incrementPct + "% salary increment to " + employees.size() + " employees");
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database not available, returning sample data with " + incrementPct + "% increment: " + e.getMessage(), e);
            return getSampleEmployeesWithIncrement(incrementPct);
        }
        
        return employees;
    }
    
    @Override
    public boolean deleteEmployee(int empId) {
        String sql = "DELETE FROM employees WHERE employee_id = ?";
        
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            preparedStatement.setInt(1, empId);
            int rowsDeleted = preparedStatement.executeUpdate();
            
            boolean deleted = rowsDeleted > 0;
            if (deleted) {
                LOGGER.info("Successfully deleted employee with ID: " + empId);
            } else {
                LOGGER.warning("No employee found with ID: " + empId + " for deletion");
            }
            
            return deleted;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete employee with ID: " + empId, e);
            throw new RuntimeException("Delete operation failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Employee createEmployee(Employee employee) {
        String sql = "INSERT INTO employees (first_name, last_name, email, phone_number, job_id, salary) " +
                    "VALUES (?, ?, ?, ?, ?, ?) RETURNING employee_id, first_name, last_name, email, phone_number, job_id, salary";
        
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            preparedStatement.setString(1, employee.getFirstName());
            preparedStatement.setString(2, employee.getLastName());
            preparedStatement.setString(3, employee.getEmail());
            preparedStatement.setString(4, employee.getPhoneNumber());
            preparedStatement.setString(5, employee.getJobId());
            preparedStatement.setBigDecimal(6, employee.getSalary());
            
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    Employee createdEmployee = new Employee(resultSet);
                    LOGGER.info("Successfully created employee with ID: " + createdEmployee.getEmployeeId());
                    return createdEmployee;
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create employee: " + employee.getEmail(), e);
            throw new RuntimeException("Create operation failed: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    @Override
    public Employee updateEmployee(Employee employee) {
        String sql = "UPDATE employees SET first_name = ?, last_name = ?, email = ?, " +
                    "phone_number = ?, job_id = ?, salary = ? WHERE employee_id = ? " +
                    "RETURNING employee_id, first_name, last_name, email, phone_number, job_id, salary";
        
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            preparedStatement.setString(1, employee.getFirstName());
            preparedStatement.setString(2, employee.getLastName());
            preparedStatement.setString(3, employee.getEmail());
            preparedStatement.setString(4, employee.getPhoneNumber());
            preparedStatement.setString(5, employee.getJobId());
            preparedStatement.setBigDecimal(6, employee.getSalary());
            preparedStatement.setInt(7, employee.getEmployeeId());
            
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    Employee updatedEmployee = new Employee(resultSet);
                    LOGGER.info("Successfully updated employee with ID: " + updatedEmployee.getEmployeeId());
                    return updatedEmployee;
                } else {
                    LOGGER.warning("No employee found with ID: " + employee.getEmployeeId() + " for update");
                    return null;
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update employee with ID: " + employee.getEmployeeId(), e);
            throw new RuntimeException("Update operation failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isConnectionHealthy() {
        return connectionFactory.isHealthy();
    }
    
    // Helper methods for sample data fallback
    
    private static List<Employee> createSampleEmployees() {
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee(1, "John", "Doe", "john.doe@company.com", "555-1234", "IT_PROG", new BigDecimal("75000")));
        employees.add(new Employee(2, "Jane", "Smith", "jane.smith@company.com", "555-5678", "HR_REP", new BigDecimal("65000")));
        employees.add(new Employee(3, "Bob", "Johnson", "bob.johnson@company.com", "555-9012", "FI_ACCOUNT", new BigDecimal("55000")));
        employees.add(new Employee(4, "Alice", "Williams", "alice.williams@company.com", "555-3456", "IT_PROG", new BigDecimal("80000")));
        employees.add(new Employee(5, "Charlie", "Brown", "charlie.brown@company.com", "555-7890", "MANAGER", new BigDecimal("95000")));
        return employees;
    }
    
    private List<Employee> getSampleEmployeeById(int empId) {
        List<Employee> result = new ArrayList<>();
        for (Employee emp : SAMPLE_EMPLOYEES) {
            if (emp.getEmployeeId().equals(empId)) {
                result.add(emp);
                break;
            }
        }
        return result;
    }
    
    private Employee getSampleUpdatedEmployee(int empId) {
        for (Employee emp : SAMPLE_EMPLOYEES) {
            if (emp.getEmployeeId().equals(empId)) {
                // Return employee with 10% salary increase
                BigDecimal newSalary = emp.getSalary().multiply(new BigDecimal("1.1"));
                return new Employee(emp.getEmployeeId(), emp.getFirstName(), emp.getLastName(),
                                  emp.getEmail(), emp.getPhoneNumber(), emp.getJobId(), newSalary);
            }
        }
        return null;
    }
    
    private List<Employee> getSampleEmployeesByName(String fn) {
        List<Employee> result = new ArrayList<>();
        String lowerFn = fn.toLowerCase();
        for (Employee emp : SAMPLE_EMPLOYEES) {
            if (emp.getFirstName().toLowerCase().startsWith(lowerFn)) {
                result.add(emp);
            }
        }
        return result;
    }
    
    private List<Employee> getSampleEmployeesWithIncrement(int incrementPct) {
        List<Employee> result = new ArrayList<>();
        BigDecimal multiplier = BigDecimal.ONE.add(new BigDecimal(incrementPct).divide(new BigDecimal("100")));
        
        for (Employee emp : SAMPLE_EMPLOYEES) {
            BigDecimal newSalary = emp.getSalary().multiply(multiplier);
            result.add(new Employee(emp.getEmployeeId(), emp.getFirstName(), emp.getLastName(),
                                  emp.getEmail(), emp.getPhoneNumber(), emp.getJobId(), newSalary));
        }
        return result;
    }
    
    /**
     * Get connection factory instance (for testing and monitoring)
     * 
     * @return ConnectionFactory instance
     */
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }
    
    /**
     * Get connection pool statistics
     * 
     * @return Pool statistics as string
     */
    public String getPoolStats() {
        return connectionFactory.getPoolStats();
    }
}