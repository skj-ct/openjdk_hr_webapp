# HR Web Application - OpenJDK Migration

A modernized HR Web Application migrated from Oracle JDK 8 + Oracle Database to OpenJDK 21 + PostgreSQL. This application provides comprehensive employee management capabilities with role-based access control.

## 🚀 Technology Stack

- **Java**: OpenJDK 21 (LTS)
- **Database**: PostgreSQL 16+
- **JDBC Driver**: PostgreSQL JDBC Driver (42.7.3)
- **Connection Pooling**: HikariCP (5.1.0)
- **Database Migrations**: Flyway (10.10.0)
- **Build Tool**: Maven 3.9+
- **Application Server**: Apache Tomcat 10.1+
- **Web Framework**: Java Servlets (Jakarta EE)
- **Frontend**: HTML5, CSS3, JavaScript ES6
- **JSON Processing**: Google Gson (2.10.1)

## 📋 Features

- **Employee Management**: CRUD operations for employee records
- **Role-Based Access**: Manager (full access) and Staff (read-only) roles
- **Salary Management**: Bulk salary increment/decrement functionality
- **Form-Based Authentication**: Secure login with session management
- **Data Validation**: Comprehensive input validation and error handling
- **Connection Pooling**: High-performance database connection management
- **Database Migrations**: Automated schema management with Flyway
- **Responsive UI**: Modern, mobile-friendly interface

## 🏗️ Project Structure

```
├── src/
│   ├── main/
│   │   ├── java/com/hrapp/jdbc/samples/
│   │   │   ├── bean/              # Business logic layer
│   │   │   ├── entity/            # Data models
│   │   │   ├── web/               # Controller layer (Servlets)
│   │   │   └── config/            # Database configuration
│   │   ├── resources/
│   │   │   ├── db/migration/      # Flyway migration scripts
│   │   │   └── application.properties
│   │   └── webapp/                # Web resources (HTML, CSS, JS)
│   └── test/                      # Unit and integration tests
├── pom.xml
├── README.md
└── .gitignore
```

## 🛠️ Prerequisites

- **OpenJDK 21+**
- **PostgreSQL 16+**
- **Apache Tomcat 10.1+**
- **Maven 3.9+**

## 🚀 Quick Start

### 1. Database Setup

```bash
# Create database and user
sudo -u postgres psql
CREATE DATABASE hrdb;
CREATE USER hr_user WITH PASSWORD 'hr_password';
GRANT ALL PRIVILEGES ON DATABASE hrdb TO hr_user;
```

### 2. Build Application

```bash
mvn clean package
```

### 3. Deploy to Tomcat

```bash
# Copy WAR file to Tomcat webapps directory
cp target/HRWebApp.war $CATALINA_HOME/webapps/
```

### 4. Configure Users

Update `$CATALINA_HOME/conf/tomcat-users.xml`:

```xml
<role rolename="manager"/>
<role rolename="staff"/>
<user username="admin" password="admin123" roles="manager"/>
<user username="hr" password="hr123" roles="staff"/>
```

### 5. Access Application

- **URL**: http://localhost:8080/HRWebApp/
- **Manager Login**: admin/admin123
- **Staff Login**: hr/hr123

## 🔧 Configuration

### Database Configuration

Edit `src/main/resources/application.properties`:

```properties
# Database Connection
db.url=jdbc:postgresql://localhost:5432/hrdb
db.username=hr_user
db.password=hr_password

# HikariCP Settings
hikari.maximumPoolSize=10
hikari.minimumIdle=2
```

### Environment Profiles

- **Development**: `application-dev.properties`
- **Production**: `application-prod.properties`

## 🧪 Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Generate test coverage report
mvn jacoco:report
```

## 📊 Database Schema

The application uses PostgreSQL with the following main table:

```sql
CREATE TABLE hr.employees (
    employee_id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    job_id VARCHAR(10) NOT NULL,
    salary NUMERIC(8,2) NOT NULL CHECK (salary >= 0)
);
```

## 🔐 Security Features

- Form-based authentication
- Role-based access control (Manager/Staff)
- Session management
- Input validation and sanitization
- SQL injection prevention
- XSS protection

## 🚀 Deployment

### Production Deployment

1. **Build for production**:
   ```bash
   mvn clean package -Pproduction
   ```

2. **Configure production database**:
   - Update connection settings
   - Enable SSL if required
   - Configure connection pooling

3. **Deploy to production Tomcat**:
   - Copy WAR file
   - Configure users and roles
   - Set up monitoring

## 📈 Performance

- **HikariCP**: High-performance connection pooling
- **Database Indexing**: Optimized queries with proper indexes
- **Caching**: Application-level caching for reference data
- **Connection Management**: Automatic resource cleanup

## 🔄 Migration from Oracle

This application was successfully migrated from:
- Oracle JDK 8 → OpenJDK 21
- Oracle Database → PostgreSQL 16
- Oracle JDBC → PostgreSQL JDBC
- Manual SQL → Flyway migrations

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📝 License

This project is licensed under the MIT License.

## 🆘 Support

For issues and questions:
1. Check the troubleshooting section in the documentation
2. Review application logs in `logs/hrapp.log`
3. Check Tomcat logs for deployment issues
4. Create an issue in the GitHub repository

## 📚 Documentation

- [Build Documentation](docs/BUILD.md)
- [Deployment Guide](docs/DEPLOYMENT.md)
- [API Documentation](docs/API.md)
- [Migration Guide](docs/MIGRATION.md)
