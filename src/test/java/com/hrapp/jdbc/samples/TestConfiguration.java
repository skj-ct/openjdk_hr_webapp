package com.hrapp.jdbc.samples;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base test configuration class for HR Application tests.
 * 
 * This class provides common test configuration and utilities
 * for all test classes in the HR Application.
 * 
 * Features:
 * - Mockito integration
 * - Common test utilities
 * - Test data factories
 * - Assertion helpers
 * 
 * @author HR Application Team
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
public abstract class TestConfiguration {
    
    // Common test constants
    public static final String TEST_EMAIL_DOMAIN = "@company.com";
    public static final String TEST_PHONE_PREFIX = "555-";
    
    // Test data ranges
    public static final int MIN_EMPLOYEE_ID = 1;
    public static final int MAX_EMPLOYEE_ID = 999999;
    public static final String MIN_SALARY = "0.01";
    public static final String MAX_SALARY = "999999.99";
    
    // Common test user data
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin123";
    public static final String HR_USERNAME = "hr";
    public static final String HR_PASSWORD = "hr123";
    public static final String EMPLOYEE_USERNAME = "employee";
    public static final String EMPLOYEE_PASSWORD = "emp123";
    
    /**
     * Creates a test email address with the given username
     */
    protected String createTestEmail(String username) {
        return username.toLowerCase() + TEST_EMAIL_DOMAIN;
    }
    
    /**
     * Creates a test phone number with the given suffix
     */
    protected String createTestPhone(String suffix) {
        return TEST_PHONE_PREFIX + suffix;
    }
    
    /**
     * Validates that a string is not null or empty
     */
    protected void assertNotNullOrEmpty(String value, String fieldName) {
        org.junit.jupiter.api.Assertions.assertNotNull(value, fieldName + " should not be null");
        org.junit.jupiter.api.Assertions.assertFalse(value.trim().isEmpty(), fieldName + " should not be empty");
    }
    
    /**
     * Validates that a number is within the expected range
     */
    protected void assertInRange(int value, int min, int max, String fieldName) {
        org.junit.jupiter.api.Assertions.assertTrue(value >= min && value <= max, 
            fieldName + " should be between " + min + " and " + max + ", but was " + value);
    }
}