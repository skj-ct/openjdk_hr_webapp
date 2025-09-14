package com.hrapp.jdbc.samples.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database configuration class for PostgreSQL with HikariCP connection pooling.
 * This class replaces the Oracle-specific database configuration and provides
 * modern connection pooling capabilities.
 * 
 * Features:
 * - HikariCP connection pooling for optimal performance
 * - Configuration from properties file
 * - Health checks and connection validation
 * - Proper resource management and cleanup
 * 
 * @author HR Application Team
 */
public class DatabaseConfig {
    
    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());
    
    // Singleton instance
    private static volatile DatabaseConfig instance;
    private static final Object LOCK = new Object();
    
    // HikariCP DataSource
    private HikariDataSource dataSource;
    
    // Configuration properties
    private Properties config;
    
    // Default configuration values
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/hrdb";
    private static final String DEFAULT_USERNAME = "hr_user";
    private static final String DEFAULT_PASSWORD = "hr_password";
    private static final String DEFAULT_SCHEMA = "hr";
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final int DEFAULT_MIN_IDLE = 2;
    private static final long DEFAULT_CONNECTION_TIMEOUT = 30000; // 30 seconds
    private static final long DEFAULT_IDLE_TIMEOUT = 600000; // 10 minutes
    private static final long DEFAULT_MAX_LIFETIME = 1800000; // 30 minutes
    
    /**
     * Private constructor for singleton pattern
     */
    private DatabaseConfig() {
        loadConfiguration();
        initializeDataSource();
    }
    
    /**
     * Get singleton instance of DatabaseConfig
     * 
     * @return DatabaseConfig instance
     */
    public static DatabaseConfig getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new DatabaseConfig();
                }
            }
        }
        return instance;
    }
    
    /**
     * Load configuration from application.properties file
     */
    private void loadConfiguration() {
        config = new Properties();
        
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            
            if (input != null) {
                config.load(input);
                LOGGER.info("Database configuration loaded from application.properties");
            } else {
                LOGGER.warning("application.properties not found, using default configuration");
                loadDefaultConfiguration();
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error loading application.properties, using defaults", e);
            loadDefaultConfiguration();
        }
    }
    
    /**
     * Load default configuration values
     */
    private void loadDefaultConfiguration() {
        config.setProperty("db.url", DEFAULT_URL);
        config.setProperty("db.username", DEFAULT_USERNAME);
        config.setProperty("db.password", DEFAULT_PASSWORD);
        config.setProperty("db.schema", DEFAULT_SCHEMA);
        config.setProperty("hikari.maximumPoolSize", String.valueOf(DEFAULT_MAX_POOL_SIZE));
        config.setProperty("hikari.minimumIdle", String.valueOf(DEFAULT_MIN_IDLE));
        config.setProperty("hikari.connectionTimeout", String.valueOf(DEFAULT_CONNECTION_TIMEOUT));
        config.setProperty("hikari.idleTimeout", String.valueOf(DEFAULT_IDLE_TIMEOUT));
        config.setProperty("hikari.maxLifetime", String.valueOf(DEFAULT_MAX_LIFETIME));
    }
    
    /**
     * Initialize HikariCP DataSource with configuration
     */
    private void initializeDataSource() {
        try {
            HikariConfig hikariConfig = new HikariConfig();
            
            // Basic connection settings
            hikariConfig.setJdbcUrl(getProperty("db.url", DEFAULT_URL));
            hikariConfig.setUsername(getProperty("db.username", DEFAULT_USERNAME));
            hikariConfig.setPassword(getProperty("db.password", DEFAULT_PASSWORD));
            hikariConfig.setDriverClassName("org.postgresql.Driver");
            
            // Connection pool settings
            hikariConfig.setMaximumPoolSize(getIntProperty("hikari.maximumPoolSize", DEFAULT_MAX_POOL_SIZE));
            hikariConfig.setMinimumIdle(getIntProperty("hikari.minimumIdle", DEFAULT_MIN_IDLE));
            hikariConfig.setConnectionTimeout(getLongProperty("hikari.connectionTimeout", DEFAULT_CONNECTION_TIMEOUT));
            hikariConfig.setIdleTimeout(getLongProperty("hikari.idleTimeout", DEFAULT_IDLE_TIMEOUT));
            hikariConfig.setMaxLifetime(getLongProperty("hikari.maxLifetime", DEFAULT_MAX_LIFETIME));
            
            // Connection validation and health checks
            hikariConfig.setConnectionTestQuery("SELECT 1");
            hikariConfig.setValidationTimeout(5000); // 5 seconds
            
            // Performance and reliability settings
            hikariConfig.setLeakDetectionThreshold(getLongProperty("hikari.leakDetectionThreshold", 60000)); // 1 minute
            hikariConfig.setPoolName("HRApp-ConnectionPool");
            
            // Schema setting for PostgreSQL
            String schema = getProperty("db.schema", DEFAULT_SCHEMA);
            if (schema != null && !schema.trim().isEmpty()) {
                hikariConfig.setSchema(schema);
            }
            
            // Additional PostgreSQL-specific settings
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
            hikariConfig.addDataSourceProperty("reWriteBatchedInserts", "true");
            
            // Initialize the data source
            dataSource = new HikariDataSource(hikariConfig);
            
            LOGGER.info("HikariCP DataSource initialized successfully");
            LOGGER.info("Connection pool configuration: maxPoolSize=" + hikariConfig.getMaximumPoolSize() + 
                       ", minIdle=" + hikariConfig.getMinimumIdle());
            
            // Test the connection
            testConnection();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database configuration", e);
            throw new RuntimeException("Database configuration initialization failed", e);
        }
    }
    
    /**
     * Get DataSource instance
     * 
     * @return HikariDataSource instance
     */
    public DataSource getDataSource() {
        if (dataSource == null || dataSource.isClosed()) {
            throw new IllegalStateException("DataSource is not initialized or has been closed");
        }
        return dataSource;
    }
    
    /**
     * Get database connection from the pool
     * 
     * @return Database connection
     * @throws SQLException if connection cannot be obtained
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource is not available");
        }
        
        Connection connection = dataSource.getConnection();
        
        // Set schema if configured
        String schema = getProperty("db.schema", DEFAULT_SCHEMA);
        if (schema != null && !schema.trim().isEmpty()) {
            try {
                connection.setSchema(schema);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to set schema: " + schema, e);
                // Continue without setting schema
            }
        }
        
        return connection;
    }
    
    /**
     * Test database connection
     * 
     * @return true if connection is successful
     */
    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            boolean isValid = connection.isValid(5); // 5 second timeout
            if (isValid) {
                LOGGER.info("Database connection test successful");
            } else {
                LOGGER.warning("Database connection test failed - connection is not valid");
            }
            return isValid;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection test failed", e);
            return false;
        }
    }
    
    /**
     * Get connection pool statistics
     * 
     * @return Connection pool information as string
     */
    public String getPoolStats() {
        if (dataSource == null) {
            return "DataSource not initialized";
        }
        
        return String.format(
            "Pool Stats - Active: %d, Idle: %d, Total: %d, Waiting: %d",
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getTotalConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }
    
    /**
     * Check if DataSource is healthy
     * 
     * @return true if DataSource is healthy
     */
    public boolean isHealthy() {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }
        
        try {
            // Check if we can get a connection within reasonable time
            try (Connection connection = dataSource.getConnection()) {
                return connection.isValid(3); // 3 second timeout
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Health check failed", e);
            return false;
        }
    }
    
    /**
     * Shutdown the DataSource and cleanup resources
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            LOGGER.info("Shutting down database connection pool");
            dataSource.close();
            LOGGER.info("Database connection pool shutdown complete");
        }
    }
    
    /**
     * Create a new DataSource with custom configuration (for testing)
     * 
     * @param url Database URL
     * @param username Database username
     * @param password Database password
     * @return HikariDataSource instance
     */
    public static HikariDataSource createDataSource(String url, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        
        // Basic pool settings for testing
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("Test-ConnectionPool");
        
        return new HikariDataSource(config);
    }
    
    // Helper methods for configuration
    
    private String getProperty(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }
    
    private int getIntProperty(String key, int defaultValue) {
        try {
            String value = config.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid integer value for property " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    private long getLongProperty(String key, long defaultValue) {
        try {
            String value = config.getProperty(key);
            return value != null ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid long value for property " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Get database URL
     * 
     * @return Database URL
     */
    public String getDatabaseUrl() {
        return getProperty("db.url", DEFAULT_URL);
    }
    
    /**
     * Get database username
     * 
     * @return Database username
     */
    public String getDatabaseUsername() {
        return getProperty("db.username", DEFAULT_USERNAME);
    }
    
    /**
     * Get database schema
     * 
     * @return Database schema
     */
    public String getDatabaseSchema() {
        return getProperty("db.schema", DEFAULT_SCHEMA);
    }
}