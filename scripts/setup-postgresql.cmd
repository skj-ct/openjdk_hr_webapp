@echo off
REM PostgreSQL Database Setup Script for Windows
REM This script sets up the PostgreSQL database for the HR Web Application

echo ========================================
echo PostgreSQL Database Setup for HR App
echo ========================================

REM Check if PostgreSQL is installed
where psql >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: PostgreSQL is not installed or not in PATH
    echo Please install PostgreSQL 17+ and ensure psql is in your PATH
    echo Download from: https://www.postgresql.org/download/windows/
    pause
    exit /b 1
)

REM Set database connection parameters
set PGHOST=localhost
set PGPORT=5432
set PGUSER=postgres
set DB_NAME=hrdb
set HR_USER=hr_user
set HR_PASSWORD=hr_password

echo.
echo Database Configuration:
echo - Host: %PGHOST%
echo - Port: %PGPORT%
echo - Database: %DB_NAME%
echo - HR User: %HR_USER%
echo.

REM Prompt for PostgreSQL admin password
set /p PGPASSWORD="Enter PostgreSQL admin (postgres) password: "

echo.
echo Creating database and user...

REM Create database
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -c "CREATE DATABASE %DB_NAME%;" 2>nul
if %ERRORLEVEL% EQU 0 (
    echo ✓ Database '%DB_NAME%' created successfully
) else (
    echo ⚠ Database '%DB_NAME%' may already exist
)

REM Create user
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -c "CREATE USER %HR_USER% WITH PASSWORD '%HR_PASSWORD%';" 2>nul
if %ERRORLEVEL% EQU 0 (
    echo ✓ User '%HR_USER%' created successfully
) else (
    echo ⚠ User '%HR_USER%' may already exist
)

REM Grant privileges
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -c "GRANT ALL PRIVILEGES ON DATABASE %DB_NAME% TO %HR_USER%;" 2>nul
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %DB_NAME% -c "GRANT ALL ON SCHEMA public TO %HR_USER%;" 2>nul
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %DB_NAME% -c "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO %HR_USER%;" 2>nul
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %DB_NAME% -c "GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO %HR_USER%;" 2>nul
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %DB_NAME% -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO %HR_USER%;" 2>nul
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %DB_NAME% -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO %HR_USER%;" 2>nul

echo ✓ Privileges granted to '%HR_USER%'

REM Test connection
echo.
echo Testing connection...
set PGPASSWORD=%HR_PASSWORD%
psql -h %PGHOST% -p %PGPORT% -U %HR_USER% -d %DB_NAME% -c "SELECT version();" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo ✓ Connection test successful
) else (
    echo ✗ Connection test failed
    pause
    exit /b 1
)

echo.
echo ========================================
echo PostgreSQL setup completed successfully!
echo ========================================
echo.
echo Database Details:
echo - Database: %DB_NAME%
echo - User: %HR_USER%
echo - Password: %HR_PASSWORD%
echo - Connection: jdbc:postgresql://%PGHOST%:%PGPORT%/%DB_NAME%
echo.
echo Next steps:
echo 1. Run 'mvn flyway:migrate' to create the schema
echo 2. Run 'mvn clean package' to build the application
echo.
pause