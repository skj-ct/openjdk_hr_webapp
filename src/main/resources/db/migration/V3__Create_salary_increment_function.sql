-- V3__Create_salary_increment_function.sql
-- Create PostgreSQL function to replace Oracle stored procedure for salary increment
-- This function provides the same functionality as the Oracle SalaryHikeSP

-- Set the search path to use the hr schema
SET search_path TO hr;

-- Create function to increment salary by percentage
-- This replaces the Oracle stored procedure functionality
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
    -- Validate input parameter
    IF percentage_change IS NULL THEN
        RAISE EXCEPTION 'Percentage change cannot be NULL';
    END IF;
    
    -- Log the operation (optional, can be removed in production)
    RAISE NOTICE 'Applying salary change of % to all employees', percentage_change;
    
    -- Update salaries and return the results
    RETURN QUERY
    WITH salary_updates AS (
        UPDATE hr.employees 
        SET salary = ROUND(salary * (1 + percentage_change / 100.0), 2)
        WHERE salary > 0  -- Safety check to avoid negative salaries
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

-- Create a simpler version that just updates salaries without returning data
-- This is useful for batch operations where you don't need the result set
CREATE OR REPLACE FUNCTION hr.update_all_salaries_by_percentage(
    percentage_change NUMERIC
) 
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    affected_rows INTEGER;
BEGIN
    -- Validate input parameter
    IF percentage_change IS NULL THEN
        RAISE EXCEPTION 'Percentage change cannot be NULL';
    END IF;
    
    -- Prevent extreme salary changes (optional safety check)
    IF ABS(percentage_change) > 100 THEN
        RAISE EXCEPTION 'Percentage change cannot exceed 100%% (was: %)', percentage_change;
    END IF;
    
    -- Update salaries
    UPDATE hr.employees 
    SET salary = ROUND(salary * (1 + percentage_change / 100.0), 2)
    WHERE salary > 0;
    
    GET DIAGNOSTICS affected_rows = ROW_COUNT;
    
    RAISE NOTICE 'Updated % employee salaries by % percent', affected_rows, percentage_change;
    
    RETURN affected_rows;
END;
$$;

-- Create function to get all employees (equivalent to SELECT * FROM employees)
-- This provides a consistent interface for the application
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

-- Add comments for documentation
COMMENT ON FUNCTION hr.increment_salary_by_percentage(NUMERIC) IS 
'Increments all employee salaries by the specified percentage and returns the updated records with old and new salary values';

COMMENT ON FUNCTION hr.update_all_salaries_by_percentage(NUMERIC) IS 
'Updates all employee salaries by the specified percentage and returns the number of affected rows';

COMMENT ON FUNCTION hr.get_all_employees() IS 
'Returns all employees ordered by employee_id';

-- Grant execute permissions (adjust as needed for your environment)
-- GRANT EXECUTE ON FUNCTION hr.increment_salary_by_percentage(NUMERIC) TO hr_user;
-- GRANT EXECUTE ON FUNCTION hr.update_all_salaries_by_percentage(NUMERIC) TO hr_user;
-- GRANT EXECUTE ON FUNCTION hr.get_all_employees() TO hr_user;