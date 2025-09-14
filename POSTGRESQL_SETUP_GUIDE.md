# PostgreSQL Setup and Configuration Guide

This guide provides comprehensive instructions for setting up and configuring PostgreSQL for the HR Web Application migration from Oracle to OpenJDK.

## Table of Contents

- [Installation](#installation)
- [Database Setup](#database-setup)
- [Configuration](#configuration)
- [Security Setup](#security-setup)
- [Performance Optimization](#performance-optimization)
- [Backup and Recovery](#backup-and-recovery)
- [Monitoring](#monitoring)
- [Migration from Oracle](#migration-from-oracle)

## Installation

### Windows Installation

#### Method 1: Official PostgreSQL Installer (Recommended)

1. **Download PostgreSQL 17+**
   - Visit: https://www.postgresql.org/download/windows/
   - Download the Windows x86-64 installer
   - File size: ~300MB

2. **Run the Installer**
   ```
   Installation Directory: C:\Program Files\PostgreSQL\17
   Data Directory: C:\Program Files\PostgreSQL\17\data
   Port: 5432 (default)
   Locale: Default locale
   ```

3. **Set Superuser Password**
   - Choose a strong password for the `postgres` user
   - Remember this password - you'll need it for administration

4. **Select Components**
   - ✅ PostgreSQL Server
   - ✅ pgAdmin 4 (GUI administration tool)
   - ✅ Stack Builder (for additional tools)
   - ✅ Command Line Tools

5. **Complete Installation**
   - The installer will initialize the database cluster
   - PostgreSQL service will start automatically

#### Method 2: Using Chocolatey
```cmd
# Install Chocolatey if not already installed
# Then install PostgreSQL
choco install postgresql17 --params '/Password:your_password'
```

#### Post-Installation Setup (Windows)

1. **Add PostgreSQL to PATH**
   ```cmd
   # Add to system PATH environment variable
   C:\Program Files\PostgreSQL\17\bin
   ```

2. **Verify Installation**
   ```cmd
   # Check version
   psql --version
   
   # Test connection
   psql -U postgres -h localhost
   ```

3. **Configure Windows Service**
   ```cmd
   # Check service status
   sc query postgresql-x64-17
   
   # Start service if stopped
   net start postgresql-x64-17
   
   # Set to start automatically
   sc config postgresql-x64-17 start= auto
   ```

### Linux Installation (Ubuntu/Debian)

#### Method 1: APT Repository (Recommended)

1. **Update Package List**
   ```bash
   sudo apt update
   ```

2. **Install PostgreSQL**
   ```bash
   # Install PostgreSQL 17 and contrib packages
   sudo apt install postgresql-17 postgresql-contrib-17
   
   # Or install latest available version
   sudo apt install postgresql postgresql-contrib
   ```

3. **Start and Enable Service**
   ```bash
   sudo systemctl start postgresql
   sudo systemctl enable postgresql
   ```

4. **Verify Installation**
   ```bash
   sudo systemctl status postgresql
   psql --version
   ```

#### Method 2: Official PostgreSQL APT Repository

1. **Add PostgreSQL APT Repository**
   ```bash
   # Import signing key
   wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
   
   # Add repository
   echo "deb http://apt.postgresql.org/pub/repos/apt/ $(lsb_release -cs)-pgdg main" | sudo tee /etc/apt/sources.list.d/pgdg.list
   
   # Update package list
   sudo apt update
   
   # Install PostgreSQL 17
   sudo apt install postgresql-17 postgresql-contrib-17
   ```

#### Post-Installation Setup (Linux)

1. **Set postgres User Password**
   ```bash
   sudo -u postgres psql
   \password postgres
   \q
   ```

2. **Configure Authentication**
   ```bash
   # Edit pg_hba.conf
   sudo nano /etc/postgresql/17/main/pg_hba.conf
   
   # Change peer to md5 for local connections
   local   all             postgres                                md5
   local   all             all                                     md5
   ```

3. **Restart PostgreSQL**
   ```bash
   sudo systemctl restart postgresql
   ```

### macOS Installation

#### Method 1: Homebrew (Recommended)

1. **Install Homebrew** (if not already installed)
   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

2. **Install PostgreSQL**
   ```bash
   # Install PostgreSQL 17
   brew install postgresql@17
   
   # Start service
   brew services start postgresql@17
   
   # Add to PATH
   echo 'export PATH="/opt/homebrew/opt/postgresql@17/bin:$PATH"' >> ~/.zshrc
   source ~/.zshrc
   ```

3. **Verify Installation**
   ```bash
   psql --version
   psql postgres
   ```

#### Method 2: Postgres.app

1. **Download Postgres.app**
   - Visit: https://postgresapp.com/
   - Download and install the application

2. **Initialize Server**
   - Launch Postgres.app
   - Click "Initialize" to create a new server
   - Server will start automatically

## Database Setup

### Create Application Database and User

#### Automated Setup Script

Create a setup script for consistent database initialization:

**setup-database.sql**
```sql
-- Create database
CREATE DATABASE hrdb
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Create application user
CREATE USER hr_user WITH
    LOGIN
    NOSUPERUSER
    CREATEDB
    NOCREATEROLE
    INHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'hr_password';

-- Grant privileges on database
GRANT CONNECT ON DATABASE hrdb TO hr_user;
GRANT USAGE ON SCHEMA public TO hr_user;
GRANT CREATE ON SCHEMA public TO hr_user;

-- Connect to hrdb and set up schema permissions
\c hrdb

-- Grant table privileges (for future tables)
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO hr_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO hr_user;

-- Create extension for additional functionality (optional)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Verify setup
SELECT current_database(), current_user;
```

#### Manual Setup Steps

1. **Connect as Superuser**
   ```bash
   # Windows
   psql -U postgres -h localhost
   
   # Linux
   sudo -u postgres psql
   
   # macOS
   psql postgres
   ```

2. **Create Database**
   ```sql
   CREATE DATABASE hrdb;
   ```

3. **Create Application User**
   ```sql
   CREATE USER hr_user WITH PASSWORD 'hr_password';
   ```

4. **Grant Permissions**
   ```sql
   -- Database-level permissions
   GRANT CONNECT ON DATABASE hrdb TO hr_user;
   
   -- Connect to the application database
   \c hrdb
   
   -- Schema-level permissions
   GRANT USAGE ON SCHEMA public TO hr_user;
   GRANT CREATE ON SCHEMA public TO hr_user;
   
   -- Future table permissions
   ALTER DEFAULT PRIVILEGES IN SCHEMA public 
   GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO hr_user;
   
   ALTER DEFAULT PRIVILEGES IN SCHEMA public 
   GRANT USAGE, SELECT ON SEQUENCES TO hr_user;
   ```

5. **Test Connection**
   ```bash
   psql -U hr_user -d hrdb -h localhost
   ```

### Environment-Specific Database Setup

#### Development Environment
```sql
-- Development database with relaxed constraints
CREATE DATABASE hrdb_dev
    WITH OWNER = hr_dev_user
    ENCODING = 'UTF8';

CREATE USER hr_dev_user WITH PASSWORD 'dev_password';
GRANT ALL PRIVILEGES ON DATABASE hrdb_dev TO hr_dev_user;
```

#### Testing Environment
```sql
-- Testing database for automated tests
CREATE DATABASE hrdb_test
    WITH OWNER = hr_test_user
    ENCODING = 'UTF8';

CREATE USER hr_test_user WITH PASSWORD 'test_password';
GRANT ALL PRIVILEGES ON DATABASE hrdb_test TO hr_test_user;
```

#### Production Environment
```sql
-- Production database with restricted permissions
CREATE DATABASE hrdb_prod
    WITH OWNER = postgres
    ENCODING = 'UTF8';

-- Create application user with minimal permissions
CREATE USER hr_prod_user WITH 
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    INHERIT
    NOREPLICATION
    CONNECTION LIMIT 50
    PASSWORD 'secure_production_password';

-- Grant only necessary permissions
GRANT CONNECT ON DATABASE hrdb_prod TO hr_prod_user;
\c hrdb_prod
GRANT USAGE ON SCHEMA public TO hr_prod_user;
-- Table-specific permissions will be granted after schema creation
```

## Configuration

### PostgreSQL Configuration Files

#### postgresql.conf

Location:
- **Windows**: `C:\Program Files\PostgreSQL\17\data\postgresql.conf`
- **Linux**: `/etc/postgresql/17/main/postgresql.conf`
- **macOS**: `/opt/homebrew/var/postgresql@17/postgresql.conf`

**Key Configuration Settings:**

```ini
# Connection Settings
listen_addresses = 'localhost'          # Addresses to listen on
port = 5432                            # Port number
max_connections = 100                  # Maximum concurrent connections

# Memory Settings
shared_buffers = 256MB                 # 25% of system RAM
effective_cache_size = 1GB            # 75% of system RAM
work_mem = 4MB                         # Memory per connection for sorting
maintenance_work_mem = 64MB            # Memory for maintenance operations

# Write Ahead Log (WAL) Settings
wal_buffers = 16MB                     # WAL buffer size
checkpoint_completion_target = 0.9     # Checkpoint completion target
max_wal_size = 1GB                     # Maximum WAL size
min_wal_size = 80MB                    # Minimum WAL size

# Query Planner Settings
random_page_cost = 1.1                 # Cost of random page access (SSD optimized)
effective_io_concurrency = 200         # Concurrent I/O operations (SSD optimized)

# Logging Settings
log_destination = 'stderr'             # Log destination
logging_collector = on                 # Enable log collector
log_directory = 'log'                  # Log directory
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'  # Log filename pattern
log_statement = 'mod'                  # Log modifications
log_min_duration_statement = 1000      # Log slow queries (1 second)
log_checkpoints = on                   # Log checkpoint activity
log_connections = on                   # Log connections
log_disconnections = on                # Log disconnections
```

#### pg_hba.conf (Host-Based Authentication)

Location: Same directory as postgresql.conf

**Configuration for HR Application:**

```ini
# TYPE  DATABASE        USER            ADDRESS                 METHOD

# Local connections
local   all             postgres                                peer
local   all             all                                     md5

# IPv4 local connections
host    all             all             127.0.0.1/32            md5
host    hrdb            hr_user         127.0.0.1/32            md5

# IPv6 local connections
host    all             all             ::1/128                 md5

# Production connections (adjust IP ranges as needed)
host    hrdb_prod       hr_prod_user    10.0.0.0/8              md5
host    hrdb_prod       hr_prod_user    192.168.0.0/16          md5

# SSL connections for production
hostssl hrdb_prod       hr_prod_user    0.0.0.0/0               md5
```

### Application Configuration

#### Database Connection Properties

**application.properties**
```properties
# Database Configuration
db.url=jdbc:postgresql://localhost:5432/hrdb
db.username=hr_user
db.password=hr_password
db.driver=org.postgresql.Driver

# Connection Pool Configuration
hikari.maximumPoolSize=10
hikari.minimumIdle=2
hikari.connectionTimeout=30000
hikari.idleTimeout=600000
hikari.maxLifetime=1800000
hikari.leakDetectionThreshold=60000

# PostgreSQL-specific JDBC parameters
db.url.params=?prepareThreshold=3&preparedStatementCacheQueries=256&preparedStatementCacheSizeMiB=5&defaultRowFetchSize=1000&ApplicationName=HRWebApp
```

#### Environment-Specific Configuration

**application-dev.properties**
```properties
db.url=jdbc:postgresql://localhost:5432/hrdb_dev
db.username=hr_dev_user
db.password=dev_password
hikari.maximumPoolSize=5
hikari.minimumIdle=1
```

**application-prod.properties**
```properties
db.url=jdbc:postgresql://prod-server:5432/hrdb_prod
db.username=hr_prod_user
db.password=${DB_PASSWORD}
hikari.maximumPoolSize=20
hikari.minimumIdle=5
hikari.connectionTimeout=20000
```

## Security Setup

### SSL/TLS Configuration

#### Generate SSL Certificates

1. **Create Certificate Authority (CA)**
   ```bash
   # Generate CA private key
   openssl genrsa -out ca-key.pem 4096
   
   # Generate CA certificate
   openssl req -new -x509 -days 365 -key ca-key.pem -out ca-cert.pem
   ```

2. **Create Server Certificate**
   ```bash
   # Generate server private key
   openssl genrsa -out server-key.pem 4096
   
   # Generate certificate signing request
   openssl req -new -key server-key.pem -out server-req.pem
   
   # Generate server certificate
   openssl x509 -req -days 365 -in server-req.pem -CA ca-cert.pem -CAkey ca-key.pem -out server-cert.pem
   ```

#### Configure PostgreSQL for SSL

**postgresql.conf**
```ini
# SSL Configuration
ssl = on
ssl_cert_file = 'server-cert.pem'
ssl_key_file = 'server-key.pem'
ssl_ca_file = 'ca-cert.pem'
ssl_ciphers = 'HIGH:MEDIUM:+3DES:!aNULL'
ssl_prefer_server_ciphers = on
```

**pg_hba.conf**
```ini
# Require SSL for production connections
hostssl hrdb_prod       hr_prod_user    0.0.0.0/0               md5
```

#### Application SSL Configuration

```properties
# SSL-enabled database connection
db.url=jdbc:postgresql://localhost:5432/hrdb?ssl=true&sslmode=require&sslcert=client-cert.pem&sslkey=client-key.pem&sslrootcert=ca-cert.pem
```

### User Security

#### Password Policies

```sql
-- Set password encryption
ALTER SYSTEM SET password_encryption = 'scram-sha-256';

-- Create role with password policy
CREATE ROLE hr_app_role WITH
    LOGIN
    PASSWORD 'SecurePassword123!'
    VALID UNTIL '2025-12-31'
    CONNECTION LIMIT 10;
```

#### Role-Based Access Control

```sql
-- Create roles for different access levels
CREATE ROLE hr_readonly;
CREATE ROLE hr_readwrite;
CREATE ROLE hr_admin;

-- Grant permissions to roles
GRANT SELECT ON ALL TABLES IN SCHEMA public TO hr_readonly;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO hr_readwrite;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO hr_admin;

-- Assign roles to users
GRANT hr_readwrite TO hr_user;
GRANT hr_admin TO hr_admin_user;
```

## Performance Optimization

### Memory Configuration

#### Calculate Optimal Settings

```bash
# For a system with 8GB RAM:
# shared_buffers = 2GB (25% of RAM)
# effective_cache_size = 6GB (75% of RAM)
# work_mem = 4MB (conservative for 100 connections)
# maintenance_work_mem = 512MB (for maintenance operations)
```

**postgresql.conf**
```ini
# Memory Settings for 8GB System
shared_buffers = 2GB
effective_cache_size = 6GB
work_mem = 4MB
maintenance_work_mem = 512MB

# For 16GB System
shared_buffers = 4GB
effective_cache_size = 12GB
work_mem = 8MB
maintenance_work_mem = 1GB
```

### Query Performance

#### Enable Query Statistics

```sql
-- Enable pg_stat_statements extension
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Configure in postgresql.conf
shared_preload_libraries = 'pg_stat_statements'
pg_stat_statements.max = 10000
pg_stat_statements.track = all
```

#### Create Performance Indexes

```sql
-- Indexes for HR application
CREATE INDEX CONCURRENTLY idx_employees_job_id ON employees(job_id);
CREATE INDEX CONCURRENTLY idx_employees_salary ON employees(salary);
CREATE INDEX CONCURRENTLY idx_employees_name ON employees(first_name, last_name);
CREATE INDEX CONCURRENTLY idx_employees_email ON employees(email);

-- Composite indexes for complex queries
CREATE INDEX CONCURRENTLY idx_employees_job_salary ON employees(job_id, salary);
CREATE INDEX CONCURRENTLY idx_employees_name_job ON employees(last_name, first_name, job_id);
```

### Connection Pooling

#### PgBouncer Configuration (Optional)

If you need connection pooling at the database level:

**pgbouncer.ini**
```ini
[databases]
hrdb = host=localhost port=5432 dbname=hrdb

[pgbouncer]
listen_port = 6432
listen_addr = 127.0.0.1
auth_type = md5
auth_file = /etc/pgbouncer/userlist.txt
pool_mode = transaction
server_reset_query = DISCARD ALL
max_client_conn = 100
default_pool_size = 20
```

## Backup and Recovery

### Automated Backup Script

**backup-hrdb.sh**
```bash
#!/bin/bash

# Configuration
DB_NAME="hrdb"
DB_USER="hr_user"
BACKUP_DIR="/var/backups/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/hrdb_backup_$DATE.sql"

# Create backup directory if it doesn't exist
mkdir -p $BACKUP_DIR

# Perform backup
pg_dump -h localhost -U $DB_USER -d $DB_NAME > $BACKUP_FILE

# Compress backup
gzip $BACKUP_FILE

# Remove backups older than 30 days
find $BACKUP_DIR -name "hrdb_backup_*.sql.gz" -mtime +30 -delete

echo "Backup completed: $BACKUP_FILE.gz"
```

**backup-hrdb.cmd** (Windows)
```cmd
@echo off
set DB_NAME=hrdb
set DB_USER=hr_user
set BACKUP_DIR=C:\backups\postgresql
set DATE=%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set BACKUP_FILE=%BACKUP_DIR%\hrdb_backup_%DATE%.sql

if not exist %BACKUP_DIR% mkdir %BACKUP_DIR%

pg_dump -h localhost -U %DB_USER% -d %DB_NAME% > %BACKUP_FILE%

echo Backup completed: %BACKUP_FILE%
```

### Recovery Procedures

#### Full Database Restore

```bash
# Stop application
# Drop existing database (if needed)
dropdb -U postgres hrdb

# Recreate database
createdb -U postgres hrdb

# Restore from backup
psql -U hr_user -d hrdb < hrdb_backup_20241214_120000.sql
```

#### Point-in-Time Recovery

```sql
-- Enable WAL archiving in postgresql.conf
archive_mode = on
archive_command = 'cp %p /var/lib/postgresql/wal_archive/%f'

-- Create base backup
SELECT pg_start_backup('base_backup');
-- Copy data directory
SELECT pg_stop_backup();
```

## Monitoring

### Performance Monitoring Queries

```sql
-- Monitor active connections
SELECT count(*), state, application_name 
FROM pg_stat_activity 
WHERE datname = 'hrdb' 
GROUP BY state, application_name;

-- Monitor slow queries
SELECT query, calls, total_time, mean_time, 
       100.0 * total_time / sum(total_time) OVER() AS percentage
FROM pg_stat_statements 
WHERE query NOT LIKE '%pg_stat_statements%'
ORDER BY total_time DESC 
LIMIT 10;

-- Monitor table statistics
SELECT schemaname, tablename, n_tup_ins, n_tup_upd, n_tup_del, 
       n_live_tup, n_dead_tup, last_vacuum, last_analyze
FROM pg_stat_user_tables 
WHERE tablename = 'employees';

-- Monitor index usage
SELECT schemaname, tablename, indexname, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes 
WHERE schemaname = 'public'
ORDER BY idx_tup_read DESC;
```

### Log Analysis

```bash
# Monitor PostgreSQL logs for errors
tail -f /var/log/postgresql/postgresql-17-main.log | grep ERROR

# Monitor slow queries
tail -f /var/log/postgresql/postgresql-17-main.log | grep "duration:"

# Monitor connections
tail -f /var/log/postgresql/postgresql-17-main.log | grep "connection"
```

## Migration from Oracle

### Data Type Mapping

| Oracle Type | PostgreSQL Type | Notes |
|-------------|-----------------|-------|
| NUMBER | NUMERIC or INTEGER | Use NUMERIC for precision |
| VARCHAR2 | VARCHAR | Similar functionality |
| DATE | TIMESTAMP or DATE | PostgreSQL DATE is date-only |
| CLOB | TEXT | PostgreSQL TEXT has no size limit |
| BLOB | BYTEA | Binary data storage |
| ROWID | SERIAL or UUID | Use SERIAL for auto-increment |

### Schema Migration

```sql
-- Oracle schema (original)
CREATE TABLE employees (
    employee_id NUMBER PRIMARY KEY,
    first_name VARCHAR2(50),
    last_name VARCHAR2(50),
    email VARCHAR2(100) UNIQUE,
    phone_number VARCHAR2(20),
    job_id VARCHAR2(10),
    salary NUMBER(8,2)
);

-- PostgreSQL schema (migrated)
CREATE TABLE employees (
    employee_id SERIAL PRIMARY KEY,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email VARCHAR(100) UNIQUE,
    phone_number VARCHAR(20),
    job_id VARCHAR(10),
    salary NUMERIC(8,2)
);
```

### Data Migration Script

```bash
#!/bin/bash
# migrate-oracle-to-postgresql.sh

# Export from Oracle
sqlplus hr/hr@localhost:1521/XE << EOF
SET PAGESIZE 0
SET FEEDBACK OFF
SET HEADING OFF
SPOOL employees_export.csv
SELECT employee_id||','||first_name||','||last_name||','||email||','||phone_number||','||job_id||','||salary
FROM employees;
SPOOL OFF
EXIT
EOF

# Import to PostgreSQL
psql -U hr_user -d hrdb << EOF
\COPY employees(employee_id, first_name, last_name, email, phone_number, job_id, salary) 
FROM 'employees_export.csv' DELIMITER ',' CSV;
EOF

echo "Migration completed successfully"
```

### Stored Procedure Migration

**Oracle PL/SQL (Original)**
```sql
CREATE OR REPLACE PACKAGE refcur_pkg AS
  TYPE refcur_t IS REF CURSOR;
  PROCEDURE incrementsalary(increment_pct IN NUMBER, emp_refcur OUT refcur_t);
END refcur_pkg;
```

**PostgreSQL Function (Migrated)**
```sql
CREATE OR REPLACE FUNCTION increment_salary_function(increment_pct INTEGER)
RETURNS TABLE(employee_id INTEGER, first_name VARCHAR, last_name VARCHAR, 
              email VARCHAR, phone_number VARCHAR, job_id VARCHAR, salary NUMERIC)
AS $$
BEGIN
    -- Update salaries
    UPDATE employees 
    SET salary = salary * (1 + increment_pct / 100.0);
    
    -- Return updated records
    RETURN QUERY 
    SELECT e.employee_id, e.first_name, e.last_name, e.email, 
           e.phone_number, e.job_id, e.salary
    FROM employees e
    ORDER BY e.employee_id;
END;
$$ LANGUAGE plpgsql;
```

This comprehensive PostgreSQL setup guide provides all the necessary information to successfully configure PostgreSQL for the HR Web Application migration from Oracle to OpenJDK.