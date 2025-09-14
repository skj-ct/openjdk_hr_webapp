-- init-dev-database.sql
-- Development database initialization script
-- Run this script to set up the development database and user

-- Connect as superuser (postgres) to create database and user
-- This script should be run manually or through database setup scripts

-- Create database
-- CREATE DATABASE hrdb WITH ENCODING 'UTF8' LC_COLLATE='en_US.UTF-8' LC_CTYPE='en_US.UTF-8';

-- Create user
-- CREATE USER hr_user WITH PASSWORD 'hr_password';

-- Grant privileges
-- GRANT ALL PRIVILEGES ON DATABASE hrdb TO hr_user;

-- Connect to hrdb database
-- \c hrdb

-- Grant schema creation privileges
-- GRANT CREATE ON DATABASE hrdb TO hr_user;

-- Create hr schema
-- CREATE SCHEMA IF NOT EXISTS hr AUTHORIZATION hr_user;

-- Grant all privileges on hr schema
-- GRANT ALL ON SCHEMA hr TO hr_user;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA hr TO hr_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA hr TO hr_user;
-- GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA hr TO hr_user;

-- Set default privileges for future objects
-- ALTER DEFAULT PRIVILEGES IN SCHEMA hr GRANT ALL ON TABLES TO hr_user;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA hr GRANT ALL ON SEQUENCES TO hr_user;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA hr GRANT ALL ON FUNCTIONS TO hr_user;

-- Note: The above commands are commented out because they require superuser privileges
-- Run them manually when setting up the database for the first time

-- For Docker/TestContainers, these permissions are typically handled automatically