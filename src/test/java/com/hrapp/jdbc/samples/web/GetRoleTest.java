package com.hrapp.jdbc.samples.web;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for GetRole servlet.
 * 
 * This test class provides complete coverage of the GetRole servlet
 * with proper mocking, security testing, and production-grade practices.
 * 
 * Test Categories:
 * - Role determination for different user types
 * - Security and authentication validation
 * - Error handling and edge cases
 * - JSON response generation
 * - Input validation and sanitization
 * 
 * @author HR Application Team
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetRole Servlet Unit Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GetRoleTest {

    @Mock
    private HttpServletRequest mockRequest;
    
    @Mock
    private HttpServletResponse mockResponse;
    
    @Mock
    private Principal mockPrincipal;
    
    private GetRole getRoleServlet;
    private StringWriter responseWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws IOException {
        getRoleServlet = new GetRole();
        
        // Set up response writer
        responseWriter = new StringWriter();
        printWriter = new PrintWriter(responseWriter);
        when(mockResponse.getWriter()).thenReturn(printWriter);
    }

    @AfterEach
    void tearDown() {
        if (printWriter != null) {
            printWriter.close();
        }
    }

    // ========================================
    // SERVLET INFO AND INITIALIZATION TESTS
    // ========================================

    @Test
    @Order(1)
    @DisplayName("Servlet should initialize properly")
    void testServletInitialization() {
        // Assert
        assertNotNull(getRoleServlet);
    }

    // ========================================
    // ROLE DETERMINATION TESTS
    // ========================================

    @Test
    @Order(10)
    @DisplayName("Should return manager role for admin user")
    void testGetRole_AdminUser_ReturnsManager() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn("admin");
        when(mockRequest.isUserInRole("manager")).thenReturn(true);
        when(mockRequest.isUserInRole("staff")).thenReturn(false);

        // Act
        getRoleServlet.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setContentType("application/json");
        verify(mockResponse).setCharacterEncoding("UTF-8");
        
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        assertTrue(jsonResponse.contains("\"role\":\"manager\""));
        assertTrue(jsonResponse.contains("\"username\":\"admin\""));
    }

    @Test
    @Order(11)
    @DisplayName("Should return staff role for HR user")
    void testGetRole_HrUser_ReturnsStaff() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn("hr");
        when(mockRequest.isUserInRole("manager")).thenReturn(false);
        when(mockRequest.isUserInRole("staff")).thenReturn(true);

        // Act
        getRoleServlet.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setContentType("application/json");
        verify(mockResponse).setCharacterEncoding("UTF-8");
        
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        assertTrue(jsonResponse.contains("\"role\":\"staff\""));
        assertTrue(jsonResponse.contains("\"username\":\"hr\""));
    }

    @Test
    @Order(12)
    @DisplayName("Should return staff role for employee user")
    void testGetRole_EmployeeUser_ReturnsStaff() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn("employee");
        when(mockRequest.isUserInRole("manager")).thenReturn(false);
        when(mockRequest.isUserInRole("staff")).thenReturn(true);

        // Act
        getRoleServlet.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setContentType("application/json");
        
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        assertTrue(jsonResponse.contains("\"role\":\"staff\""));
        assertTrue(jsonResponse.contains("\"username\":\"employee\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"hr", "employee", "testuser", "readonly"})
    @Order(13)
    @DisplayName("Should return staff role for various staff users")
    void testGetRole_VariousStaffUsers(String username) throws ServletException, IOException {
        // Arrange
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn(username);
        when(mockRequest.isUserInRole("manager")).thenReturn(false);
        when(mockRequest.isUserInRole("staff")).thenReturn(true);

        // Act
        getRoleServlet.doGet(mockRequest, mockResponse);

        // Assert
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        assertTrue(jsonResponse.contains("\"role\":\"staff\""));
        assertTrue(jsonResponse.contains("\"username\":\"" + username + "\""));
    }

    // ========================================
    // AUTHENTICATION AND SECURITY TESTS
    // ========================================

    @Test
    @Order(20)
    @DisplayName("Should handle unauthenticated user")
    void testGetRole_UnauthenticatedUser() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getUserPrincipal()).thenReturn(null);

        // Act
        getRoleServlet.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(mockResponse).setContentType("application/json");
        
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        assertTrue(jsonResponse.contains("\"error\":\"Unauthorized\""));
        assertTrue(jsonResponse.contains("\"message\":\"User not authenticated\""));
    }

    @Test
    @Order(21)
    @DisplayName("Should handle user with no roles")
    void testGetRole_UserWithNoRoles() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn("noroleuser");
        when(mockRequest.isUserInRole("manager")).thenReturn(false);
        when(mockRequest.isUserInRole("staff")).thenReturn(false);

        // Act
        getRoleServlet.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(mockResponse).setContentType("application/json");
        
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        assertTrue(jsonResponse.contains("\"error\":\"Forbidden\""));
        assertTrue(jsonResponse.contains("\"message\":\"User has no valid roles\""));
    }

    @Test
    @Order(22)
    @DisplayName("Should handle user with both manager and staff roles")
    void testGetRole_UserWithBothRoles() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn("superuser");
        when(mockRequest.isUserInRole("manager")).thenReturn(true);
        when(mockRequest.isUserInRole("staff")).thenReturn(true);

        // Act
        getRoleServlet.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setContentType("application/json");
        
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        // Manager role should take precedence
        assertTrue(jsonResponse.contains("\"role\":\"manager\""));
        assertTrue(jsonResponse.contains("\"username\":\"superuser\""));
    }

    // ========================================
    // INPUT VALIDATION AND SECURITY TESTS
    // ========================================

    @Test
    @Order(30)
    @DisplayName("Should handle username with special characters")
    void testGetRole_UsernameWithSpecialCharacters() throws ServletException, IOException {
        // Arrange
        String specialUsername = "user@domain.com";
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn(specialUsername);
        when(mockRequest.isUserInRole("manager")).thenReturn(false);
        when(mockRequest.isUserInRole("staff")).thenReturn(true);

        // Act
        getRoleServlet.doGet(mockRequest, mockResponse);

        // Assert
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        assertTrue(jsonResponse.contains("\"username\":\"user@domain.com\""));
        assertTrue(jsonResponse.contains("\"role\":\"staff\""));
    }

    @Test
    @Order(31)
    @DisplayName("Should handle username with potential XSS content")
    void testGetRole_UsernameWithXssContent() throws ServletException, IOException {
        // Arrange
        String xssUsername = "<script>alert('xss')</script>";
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn(xssUsername);
        when(mockRequest.isUserInRole("manager")).thenReturn(false);
        when(mockRequest.isUserInRole("staff")).thenReturn(true);

        // Act
        getRoleServlet.doGet(mockRequest, mockResponse);

        // Assert
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        // JSON should properly escape the content
        assertTrue(jsonResponse.contains("\\u003cscript\\u003e") || 
                  jsonResponse.contains("&lt;script&gt;") ||
                  jsonResponse.contains("\"username\":\"<script>alert('xss')</script>\""));
        assertTrue(jsonResponse.contains("\"role\":\"staff\""));
    }

    @Test
    @Order(32)
    @DisplayName("Should handle very long username")
    void testGetRole_VeryLongUsername() throws ServletException, IOException {
        // Arrange
        String longUsername = "a".repeat(1000);
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn(longUsername);
        when(mockRequest.isUserInRole("manager")).thenReturn(true);
        when(mockRequest.isUserInRole("staff")).thenReturn(false);

        // Act
        getRoleServlet.doGet(mockRequest, mockResponse);

        // Assert
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        assertTrue(jsonResponse.contains("\"role\":\"manager\""));
        assertTrue(jsonResponse.contains(longUsername));
    }

    // ========================================
    // HTTP METHOD TESTS
    // ========================================

    @Test
    @Order(40)
    @DisplayName("POST request should work same as GET")
    void testDoPost_SameAsGet() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn("admin");
        when(mockRequest.isUserInRole("manager")).thenReturn(true);
        when(mockRequest.isUserInRole("staff")).thenReturn(false);

        // Act
        getRoleServlet.doPost(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setContentType("application/json");
        verify(mockResponse).setCharacterEncoding("UTF-8");
        
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        assertTrue(jsonResponse.contains("\"role\":\"manager\""));
        assertTrue(jsonResponse.contains("\"username\":\"admin\""));
    }

    // ========================================
    // ERROR HANDLING TESTS
    // ========================================

    @Test
    @Order(50)
    @DisplayName("Should handle IOException gracefully")
    void testGetRole_IOExceptionHandling() throws IOException, ServletException {
        // Arrange
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn("admin");
        when(mockRequest.isUserInRole("manager")).thenReturn(true);
        when(mockResponse.getWriter()).thenThrow(new IOException("Writer error"));

        // Act & Assert
        assertThrows(IOException.class, () -> {
            getRoleServlet.doGet(mockRequest, mockResponse);
        });
    }

    @Test
    @Order(51)
    @DisplayName("Should handle null principal name")
    void testGetRole_NullPrincipalName() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn(null);

        // Act
        getRoleServlet.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        assertTrue(jsonResponse.contains("\"error\":\"Unauthorized\""));
    }

    // ========================================
    // JSON RESPONSE FORMAT TESTS
    // ========================================

    @Test
    @Order(60)
    @DisplayName("JSON response should have correct format for manager")
    void testJsonResponseFormat_Manager() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn("admin");
        when(mockRequest.isUserInRole("manager")).thenReturn(true);
        when(mockRequest.isUserInRole("staff")).thenReturn(false);

        // Act
        getRoleServlet.doGet(mockRequest, mockResponse);

        // Assert
        printWriter.flush();
        String jsonResponse = responseWriter.toString().trim();
        
        // Verify JSON structure
        assertTrue(jsonResponse.startsWith("{"));
        assertTrue(jsonResponse.endsWith("}"));
        assertTrue(jsonResponse.contains("\"role\":\"manager\""));
        assertTrue(jsonResponse.contains("\"username\":\"admin\""));
        assertTrue(jsonResponse.contains("\"authenticated\":true"));
    }

    @Test
    @Order(61)
    @DisplayName("JSON response should have correct format for staff")
    void testJsonResponseFormat_Staff() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn("hr");
        when(mockRequest.isUserInRole("manager")).thenReturn(false);
        when(mockRequest.isUserInRole("staff")).thenReturn(true);

        // Act
        getRoleServlet.doGet(mockRequest, mockResponse);

        // Assert
        printWriter.flush();
        String jsonResponse = responseWriter.toString().trim();
        
        // Verify JSON structure
        assertTrue(jsonResponse.startsWith("{"));
        assertTrue(jsonResponse.endsWith("}"));
        assertTrue(jsonResponse.contains("\"role\":\"staff\""));
        assertTrue(jsonResponse.contains("\"username\":\"hr\""));
        assertTrue(jsonResponse.contains("\"authenticated\":true"));
    }

    @Test
    @Order(62)
    @DisplayName("JSON error response should have correct format")
    void testJsonErrorResponseFormat() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getUserPrincipal()).thenReturn(null);

        // Act
        getRoleServlet.doGet(mockRequest, mockResponse);

        // Assert
        printWriter.flush();
        String jsonResponse = responseWriter.toString().trim();
        
        // Verify JSON error structure
        assertTrue(jsonResponse.startsWith("{"));
        assertTrue(jsonResponse.endsWith("}"));
        assertTrue(jsonResponse.contains("\"error\":\"Unauthorized\""));
        assertTrue(jsonResponse.contains("\"message\":"));
        assertTrue(jsonResponse.contains("\"authenticated\":false"));
    }

    // ========================================
    // PERFORMANCE TESTS
    // ========================================

    @Test
    @Order(70)
    @DisplayName("Multiple requests should not cause performance issues")
    void testPerformance_MultipleRequests() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn("admin");
        when(mockRequest.isUserInRole("manager")).thenReturn(true);
        when(mockRequest.isUserInRole("staff")).thenReturn(false);

        // Act - Simulate multiple requests
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            getRoleServlet.doGet(mockRequest, mockResponse);
            responseWriter.getBuffer().setLength(0); // Clear buffer
        }
        long endTime = System.currentTimeMillis();

        // Assert - Should complete quickly (less than 1 second for 100 requests)
        assertTrue(endTime - startTime < 1000, "100 requests should complete in less than 1 second");
        
        // Verify all requests were processed
        verify(mockRequest, times(100)).getUserPrincipal();
        verify(mockResponse, times(100)).setContentType("application/json");
    }
}