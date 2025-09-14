-- V1__Create_employees_table.sql
-- Initial database schema creation for HR Web Application
-- Migrated from Oracle Database to PostgreSQL

-- Create the hr schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS hr;

-- Set the search path to use the hr schema
SET search_path TO hr;

-- Create the employees table
CREATE TABLE hr.employees (
    employee_id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    job_id VARCHAR(10) NOT NULL,
    salary NUMERIC(8,2) NOT NULL CHECK (salary >= 0)
);

-- Create indexes for better performance
CREATE INDEX idx_employees_email ON hr.employees(email);
CREATE INDEX idx_employees_job_id ON hr.employees(job_id);
CREATE INDEX idx_employees_name ON hr.employees(last_name, first_name);

-- Add comments for documentation
COMMENT ON TABLE hr.employees IS 'Employee information table for HR Web Application';
COMMENT ON COLUMN hr.employees.employee_id IS 'Primary key - auto-generated employee ID';
COMMENT ON COLUMN hr.employees.first_name IS 'Employee first name';
COMMENT ON COLUMN hr.employees.last_name IS 'Employee last name';
COMMENT ON COLUMN hr.employees.email IS 'Employee email address - must be unique';
COMMENT ON COLUMN hr.employees.phone_number IS 'Employee phone number';
COMMENT ON COLUMN hr.employees.job_id IS 'Job identifier/code';
COMMENT ON COLUMN hr.employees.salary IS 'Employee salary in decimal format';

-- Grant necessary permissions (adjust as needed for your environment)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON hr.employees TO hr_user;
-- GRANT USAGE, SELECT ON SEQUENCE hr.employees_employee_id_seq TO hr_user;