#!/bin/bash
# PostgreSQL Database Setup Script for Linux/macOS
# This script sets up the PostgreSQL database for the HR Web Application

echo "========================================"
echo "PostgreSQL Database Setup for HR App"
echo "========================================"

# Check if PostgreSQL is installed
if ! command -v psql &> /dev/null; then
    echo "ERROR: PostgreSQL is not installed or not in PATH"
    echo "Please install PostgreSQL 17+ and ensure psql is in your PATH"
    echo "Ubuntu/Debian: sudo apt-get install postgresql postgresql-client"
    echo "CentOS/RHEL: sudo yum install postgresql postgresql-server"
    echo "macOS: brew install postgresql"
    exit 1
fi

# Set database connection parameters
PGHOST=${PGHOST:-localhost}
PGPORT=${PGPORT:-5432}
PGUSER=${PGUSER:-postgres}
DB_NAME=hrdb
HR_USER=hr_user
HR_PASSWORD=hr_password

echo
echo "Database Configuration:"
echo "- Host: $PGHOST"
echo "- Port: $PGPORT"
echo "- Database: $DB_NAME"
echo "- HR User: $HR_USER"
echo

# Prompt for PostgreSQL admin password
read -s -p "Enter PostgreSQL admin (postgres) password: " PGPASSWORD
export PGPASSWORD
echo

echo
echo "Creating database and user..."

# Create database
if psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -c "CREATE DATABASE $DB_NAME;" 2>/dev/null; then
    echo "✓ Database '$DB_NAME' created successfully"
else
    echo "⚠ Database '$DB_NAME' may already exist"
fi

# Create user
if psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -c "CREATE USER $HR_USER WITH PASSWORD '$HR_PASSWORD';" 2>/dev/null; then
    echo "✓ User '$HR_USER' created successfully"
else
    echo "⚠ User '$HR_USER' may already exist"
fi

# Grant privileges
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $HR_USER;" 2>/dev/null
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$DB_NAME" -c "GRANT ALL ON SCHEMA public TO $HR_USER;" 2>/dev/null
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$DB_NAME" -c "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $HR_USER;" 2>/dev/null
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$DB_NAME" -c "GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $HR_USER;" 2>/dev/null
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$DB_NAME" -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $HR_USER;" 2>/dev/null
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$DB_NAME" -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $HR_USER;" 2>/dev/null

echo "✓ Privileges granted to '$HR_USER'"

# Test connection
echo
echo "Testing connection..."
export PGPASSWORD="$HR_PASSWORD"
if psql -h "$PGHOST" -p "$PGPORT" -U "$HR_USER" -d "$DB_NAME" -c "SELECT version();" >/dev/null 2>&1; then
    echo "✓ Connection test successful"
else
    echo "✗ Connection test failed"
    exit 1
fi

echo
echo "========================================"
echo "PostgreSQL setup completed successfully!"
echo "========================================"
echo
echo "Database Details:"
echo "- Database: $DB_NAME"
echo "- User: $HR_USER"
echo "- Password: $HR_PASSWORD"
echo "- Connection: jdbc:postgresql://$PGHOST:$PGPORT/$DB_NAME"
echo
echo "Next steps:"
echo "1. Run 'mvn flyway:migrate' to create the schema"
echo "2. Run 'mvn clean package' to build the application"
echo