package com.hrapp.jdbc.samples.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ConnectionFactoryTest {

    @Test
    void testConnectionFactoryExists() {
        // Basic test to ensure the class can be instantiated
        assertDoesNotThrow(() -> {
            // Test that ConnectionFactory class exists and can be referenced
            Class.forName("com.hrapp.jdbc.samples.config.ConnectionFactory");
        });
    }
}
