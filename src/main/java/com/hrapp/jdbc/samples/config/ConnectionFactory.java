package com.hrapp.jdbc.samples.config;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Connection factory for managing database connections.
 * This class provides a centralized way to obtain database connections
 * and manages the underlying HikariCP connection pool.
 * 
 * Features:
 * - Centralized connection management
 * - Connection validation and health checks
 * - Proper error handling and logging
 * - Resource cleanup utilities
 * 
 * @author HR Application Team
 */
public class ConnectionFactory {
    
    private static final Logger LOGGER = Logger.getLogger(ConnectionFactory.class.getName());
    
    // Database configuration instance
    private final DatabaseConfig databaseConfig;
    
    // Singleton instance
    private static volatile ConnectionFactory instance;
    private static final Object LOCK = new Object();
    
    /**
     * Private constructor for singleton pattern
     */
    private ConnectionFactory() {
        this.databaseConfig = DatabaseConfig.getInstance();
    }
    
    /**
     * Get singleton instance of ConnectionFactory
     * 
     * @return ConnectionFactory instance
     */
    public static ConnectionFactory getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ConnectionFactory();
                }
            }
        }
        return instance;
    }
    
    /**
     * Get a database connection from the pool
     * 
     * @return Database connection
     * @throws SQLException if connection cannot be obtained
     */
    public Connection getConnection() throws SQLException {
        try {
            Connection connection = databaseConfig.getConnection();
            
            // Validate connection before returning
            if (!connection.isValid(5)) {
                throw new SQLException("Connection validation failed");
            }
            
            LOGGER.fine("Database connection obtained successfully");
            return connection;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to obtain database connection", e);
            throw new SQLException("Unable to obtain database connection: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get a database connection with auto-commit setting
     * 
     * @param autoCommit Auto-commit setting
     * @return Database connection
     * @throws SQLException if connection cannot be obtained
     */
    public Connection getConnection(boolean autoCommit) throws SQLException {
        Connection connection = getConnection();
        try {
            connection.setAutoCommit(autoCommit);
            return connection;
        } catch (SQLException e) {
            // Close connection if we can't set auto-commit
            closeConnection(connection);
            throw e;
        }
    }
    
    /**
     * Get a database connection with specific transaction isolation level
     * 
     * @param isolationLevel Transaction isolation level
     * @return Database connection
     * @throws SQLException if connection cannot be obtained
     */
    public Connection getConnection(int isolationLevel) throws SQLException {
        Connection connection = getConnection();
        try {
            connection.setTransactionIsolation(isolationLevel);
            return connection;
        } catch (SQLException e) {
            // Close connection if we can't set isolation level
            closeConnection(connection);
            throw e;
        }
    }
    
    /**
     * Get the underlying DataSource
     * 
     * @return DataSource instance
     */
    public DataSource getDataSource() {
        return databaseConfig.getDataSource();
    }
    
    /**
     * Test database connectivity
     * 
     * @return true if database is accessible
     */
    public boolean testConnection() {
        return databaseConfig.testConnection();
    }
    
    /**
     * Check if the connection factory is healthy
     * 
     * @return true if healthy
     */
    public boolean isHealthy() {
        return databaseConfig.isHealthy();
    }
    
    /**
     * Get connection pool statistics
     * 
     * @return Pool statistics as string
     */
    public String getPoolStats() {
        return databaseConfig.getPoolStats();
    }
    
    /**
     * Safely close a database connection
     * 
     * @param connection Connection to close
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    LOGGER.fine("Database connection closed successfully");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing database connection", e);
            }
        }
    }
    
    /**
     * Safely rollback a transaction
     * 
     * @param connection Connection to rollback
     */
    public static void rollbackTransaction(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.getAutoCommit()) {
                    connection.rollback();
                    LOGGER.fine("Transaction rolled back successfully");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error rolling back transaction", e);
            }
        }
    }
    
    /**
     * Safely commit a transaction
     * 
     * @param connection Connection to commit
     * @throws SQLException if commit fails
     */
    public static void commitTransaction(Connection connection) throws SQLException {
        if (connection != null && !connection.getAutoCommit()) {
            connection.commit();
            LOGGER.fine("Transaction committed successfully");
        }
    }
    
    /**
     * Execute a database operation with automatic connection management
     * 
     * @param operation Database operation to execute
     * @return Result of the operation
     * @throws SQLException if operation fails
     */
    public <T> T executeWithConnection(DatabaseOperation<T> operation) throws SQLException {
        Connection connection = null;
        try {
            connection = getConnection();
            return operation.execute(connection);
        } finally {
            closeConnection(connection);
        }
    }
    
    /**
     * Execute a database operation within a transaction
     * 
     * @param operation Database operation to execute
     * @return Result of the operation
     * @throws SQLException if operation fails
     */
    public <T> T executeInTransaction(DatabaseOperation<T> operation) throws SQLException {
        Connection connection = null;
        try {
            connection = getConnection(false); // Disable auto-commit
            T result = operation.execute(connection);
            commitTransaction(connection);
            return result;
        } catch (SQLException e) {
            rollbackTransaction(connection);
            throw e;
        } catch (Exception e) {
            rollbackTransaction(connection);
            throw new SQLException("Transaction failed: " + e.getMessage(), e);
        } finally {
            closeConnection(connection);
        }
    }
    
    /**
     * Execute a database operation with retry logic
     * 
     * @param operation Database operation to execute
     * @param maxRetries Maximum number of retries
     * @return Result of the operation
     * @throws SQLException if all retries fail
     */
    public <T> T executeWithRetry(DatabaseOperation<T> operation, int maxRetries) throws SQLException {
        SQLException lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                return executeWithConnection(operation);
            } catch (SQLException e) {
                lastException = e;
                
                if (attempt <= maxRetries) {
                    LOGGER.warning("Database operation failed (attempt " + attempt + "/" + (maxRetries + 1) + 
                                 "), retrying: " + e.getMessage());
                    
                    // Wait before retry (exponential backoff)
                    try {
                        Thread.sleep(Math.min(1000 * attempt, 5000));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Operation interrupted during retry", ie);
                    }
                } else {
                    LOGGER.log(Level.SEVERE, "Database operation failed after " + (maxRetries + 1) + " attempts", e);
                }
            }
        }
        
        throw lastException;
    }
    
    /**
     * Get database configuration information
     * 
     * @return Database configuration details
     */
    public DatabaseInfo getDatabaseInfo() {
        return new DatabaseInfo(
            databaseConfig.getDatabaseUrl(),
            databaseConfig.getDatabaseUsername(),
            databaseConfig.getDatabaseSchema(),
            isHealthy(),
            getPoolStats()
        );
    }
    
    /**
     * Shutdown the connection factory and cleanup resources
     */
    public void shutdown() {
        LOGGER.info("Shutting down ConnectionFactory");
        databaseConfig.shutdown();
    }
    
    /**
     * Functional interface for database operations
     * 
     * @param <T> Return type of the operation
     */
    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute(Connection connection) throws SQLException;
    }
    
    /**
     * Database information holder class
     */
    public static class DatabaseInfo {
        private final String url;
        private final String username;
        private final String schema;
        private final boolean healthy;
        private final String poolStats;
        
        public DatabaseInfo(String url, String username, String schema, boolean healthy, String poolStats) {
            this.url = url;
            this.username = username;
            this.schema = schema;
            this.healthy = healthy;
            this.poolStats = poolStats;
        }
        
        public String getUrl() { return url; }
        public String getUsername() { return username; }
        public String getSchema() { return schema; }
        public boolean isHealthy() { return healthy; }
        public String getPoolStats() { return poolStats; }
        
        @Override
        public String toString() {
            return String.format("DatabaseInfo{url='%s', username='%s', schema='%s', healthy=%s, poolStats='%s'}",
                    url, username, schema, healthy, poolStats);
        }
    }
}