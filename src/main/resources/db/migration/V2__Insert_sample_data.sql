-- V2__Insert_sample_data.sql
-- Insert sample employee data for HR Web Application
-- This provides initial test data similar to Oracle HR schema

-- Set the search path to use the hr schema
SET search_path TO hr;

-- Insert sample employee data
INSERT INTO hr.employees (first_name, last_name, email, phone_number, job_id, salary) VALUES
('John', 'Doe', 'john.doe@anyco.com', '555-0101', 'IT_PROG', 75000.00),
('Jane', 'Smith', 'jane.smith@anyco.com', '555-0102', 'HR_REP', 65000.00),
('Bob', 'Johnson', 'bob.johnson@anyco.com', '555-0103', 'SA_MAN', 85000.00),
('Alice', 'Williams', 'alice.williams@anyco.com', '555-0104', 'IT_PROG', 72000.00),
('Charlie', 'Brown', 'charlie.brown@anyco.com', '555-0105', 'HR_REP', 68000.00),
('Diana', 'Davis', 'diana.davis@anyco.com', '555-0106', 'SA_REP', 55000.00),
('Edward', 'Miller', 'edward.miller@anyco.com', '555-0107', 'IT_PROG', 78000.00),
('Fiona', 'Wilson', 'fiona.wilson@anyco.com', '555-0108', 'HR_MAN', 95000.00),
('George', 'Moore', 'george.moore@anyco.com', '555-0109', 'SA_MAN', 88000.00),
('Helen', 'Taylor', 'helen.taylor@anyco.com', '555-0110', 'IT_PROG', 74000.00),
('Ian', 'Anderson', 'ian.anderson@anyco.com', '555-0111', 'SA_REP', 58000.00),
('Julia', 'Thomas', 'julia.thomas@anyco.com', '555-0112', 'HR_REP', 66000.00),
('Kevin', 'Jackson', 'kevin.jackson@anyco.com', '555-0113', 'IT_PROG', 76000.00),
('Linda', 'White', 'linda.white@anyco.com', '555-0114', 'SA_MAN', 90000.00),
('Michael', 'Harris', 'michael.harris@anyco.com', '555-0115', 'HR_REP', 67000.00);

-- Verify the data was inserted correctly
-- SELECT COUNT(*) as total_employees FROM hr.employees;
-- SELECT job_id, COUNT(*) as count, AVG(salary) as avg_salary 
-- FROM hr.employees 
-- GROUP BY job_id 
-- ORDER BY job_id;