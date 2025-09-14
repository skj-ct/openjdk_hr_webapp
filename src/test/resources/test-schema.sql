-- test-schema.sql
-- Test database schema for integration testing with TestContainers
-- This creates a minimal schema for testing purposes

-- Create the hr schema for testing
CREATE SCHEMA IF NOT EXISTS hr;

-- Set the search path
SET search_path TO hr;

-- Create employees table (simplified version for testing)
CREATE TABLE IF NOT EXISTS hr.employees (
    employee_id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    job_id VARCHAR(10) NOT NULL,
    salary NUMERIC(8,2) NOT NULL CHECK (salary >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create basic indexes
CREATE INDEX IF NOT EXISTS idx_test_employees_email ON hr.employees(email);
CREATE INDEX IF NOT EXISTS idx_test_employees_job_id ON hr.employees(job_id);

-- Create the salary increment function for testing
CREATE OR REPLACE FUNCTION hr.increment_salary_by_percentage(
    percentage_change NUMERIC
) 
RETURNS TABLE(
    employee_id INTEGER,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email VARCHAR(100),
    phone_number VARCHAR(20),
    job_id VARCHAR(10),
    old_salary NUMERIC(8,2),
    new_salary NUMERIC(8,2)
) 
LANGUAGE plpgsql
AS $$
BEGIN
    IF percentage_change IS NULL THEN
        RAISE EXCEPTION 'Percentage change cannot be NULL';
    END IF;
    
    RETURN QUERY
    WITH salary_updates AS (
        UPDATE hr.employees 
        SET salary = ROUND(salary * (1 + percentage_change / 100.0), 2)
        WHERE salary > 0
        RETURNING 
            employees.employee_id,
            employees.first_name,
            employees.last_name,
            employees.email,
            employees.phone_number,
            employees.job_id,
            ROUND(salary / (1 + percentage_change / 100.0), 2) AS old_salary,
            employees.salary AS new_salary
    )
    SELECT * FROM salary_updates
    ORDER BY salary_updates.employee_id;
END;
$$;

-- Create simple get all employees function
CREATE OR REPLACE FUNCTION hr.get_all_employees()
RETURNS TABLE(
    employee_id INTEGER,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email VARCHAR(100),
    phone_number VARCHAR(20),
    job_id VARCHAR(10),
    salary NUMERIC(8,2)
)
LANGUAGE sql
STABLE
AS $$
    SELECT 
        e.employee_id,
        e.first_name,
        e.last_name,
        e.email,
        e.phone_number,
        e.job_id,
        e.salary
    FROM hr.employees e
    ORDER BY e.employee_id;
$$;