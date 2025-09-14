-- V4__Add_additional_constraints_and_indexes.sql
-- Add additional constraints, indexes, and optimizations for better performance and data integrity

-- Set the search path to use the hr schema
SET search_path TO hr;

-- Add additional constraints for data validation
ALTER TABLE hr.employees 
ADD CONSTRAINT chk_employees_first_name_not_empty 
CHECK (LENGTH(TRIM(first_name)) > 0);

ALTER TABLE hr.employees 
ADD CONSTRAINT chk_employees_last_name_not_empty 
CHECK (LENGTH(TRIM(last_name)) > 0);

ALTER TABLE hr.employees 
ADD CONSTRAINT chk_employees_email_format 
CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

ALTER TABLE hr.employees 
ADD CONSTRAINT chk_employees_salary_reasonable 
CHECK (salary BETWEEN 0 AND 1000000);

-- Add job_id validation (common job codes)
ALTER TABLE hr.employees 
ADD CONSTRAINT chk_employees_job_id_valid 
CHECK (job_id IN ('IT_PROG', 'HR_REP', 'HR_MAN', 'SA_REP', 'SA_MAN', 'FI_ACCOUNT', 'FI_MGR', 'AD_ASST', 'AD_VP', 'AD_PRES'));

-- Create additional indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_employees_salary ON hr.employees(salary);
CREATE INDEX IF NOT EXISTS idx_employees_salary_desc ON hr.employees(salary DESC);

-- Create composite index for name searches
CREATE INDEX IF NOT EXISTS idx_employees_full_name ON hr.employees(first_name, last_name);

-- Create partial index for high-salary employees (optimization for management queries)
CREATE INDEX IF NOT EXISTS idx_employees_high_salary ON hr.employees(salary) 
WHERE salary > 80000;

-- Add audit columns (optional - for future use)
ALTER TABLE hr.employees 
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- Create function to automatically update the updated_at timestamp
CREATE OR REPLACE FUNCTION hr.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update updated_at on row changes
CREATE TRIGGER tr_employees_updated_at
    BEFORE UPDATE ON hr.employees
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_updated_at_column();

-- Update existing records to have proper timestamps
UPDATE hr.employees 
SET created_at = CURRENT_TIMESTAMP - INTERVAL '1 day',
    updated_at = CURRENT_TIMESTAMP - INTERVAL '1 day'
WHERE created_at IS NULL;

-- Add comments for new columns
COMMENT ON COLUMN hr.employees.created_at IS 'Timestamp when the employee record was created';
COMMENT ON COLUMN hr.employees.updated_at IS 'Timestamp when the employee record was last updated';

-- Create view for employee summary (useful for reporting)
CREATE OR REPLACE VIEW hr.employee_summary AS
SELECT 
    job_id,
    COUNT(*) as employee_count,
    AVG(salary) as average_salary,
    MIN(salary) as min_salary,
    MAX(salary) as max_salary,
    SUM(salary) as total_salary
FROM hr.employees
GROUP BY job_id
ORDER BY job_id;

COMMENT ON VIEW hr.employee_summary IS 'Summary statistics of employees grouped by job_id';

-- Grant permissions on new objects (adjust as needed)
-- GRANT SELECT ON hr.employee_summary TO hr_user;
-- GRANT EXECUTE ON FUNCTION hr.update_updated_at_column() TO hr_user;