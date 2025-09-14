package com.hrapp.jdbc.samples;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.junit.platform.suite.api.IncludeClassNamePatterns;

/**
 * Comprehensive test suite for the HR Web Application.
 * 
 * This test suite runs all unit tests across the application
 * to ensure complete coverage and validation of all components.
 * 
 * Test Categories Included:
 * - Entity layer tests (Employee)
 * - Business logic tests (JdbcBeanImpl)
 * - Web layer tests (Servlets)
 * - Configuration tests (DatabaseConfig)
 * - Integration tests
 * 
 * Usage:
 * Run this test suite to execute all unit tests in the application.
 * This is useful for:
 * - Continuous Integration builds
 * - Pre-deployment validation
 * - Regression testing
 * - Code coverage analysis
 * 
 * @author HR Application Team
 * @version 1.0
 * @since 1.0
 */
@Suite
@SuiteDisplayName("HR Web Application - Comprehensive Unit Test Suite")
@SelectPackages({
    "com.hrapp.jdbc.samples.entity",
    "com.hrapp.jdbc.samples.bean", 
    "com.hrapp.jdbc.samples.web",
    "com.hrapp.jdbc.samples.config"
})
@IncludeClassNamePatterns(".*Test")
public class ComprehensiveTestSuite {
    
    // This class serves as a test suite configuration
    // No implementation needed - annotations drive the behavior
    
    /*
     * Test Execution Order:
     * 1. Entity Tests (Employee)
     * 2. Configuration Tests (DatabaseConfig, ConnectionFactory)
     * 3. Business Logic Tests (JdbcBeanImpl)
     * 4. Web Layer Tests (WebController, GetRole, SimpleLogoutServlet)
     * 5. Integration Tests (if included)
     * 
     * Expected Coverage:
     * - Entity Layer: >95%
     * - Business Logic: >80%
     * - Web Layer: >85%
     * - Configuration: >90%
     * 
     * Performance Expectations:
     * - Unit tests should complete in <30 seconds
     * - No external dependencies (database, network)
     * - All tests should be deterministic and repeatable
     */
}