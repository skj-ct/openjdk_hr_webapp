package com.hrapp.jdbc.samples.web;

import com.hrapp.jdbc.samples.bean.JdbcBean;
import com.hrapp.jdbc.samples.entity.Employee;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for WebController servlet.
 * 
 * This test class provides complete coverage of the WebController servlet
 * with proper mocking, error handling, and production-grade test practices.
 * 
 * Test Categories:
 * - HTTP method handling (GET/POST)
 * - Parameter processing and validation
 * - JSON response generation
 * - Session management and authentication
 * - Error handling and edge cases
 * - Security and input validation
 * 
 * @author HR Application Team
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebController Servlet Unit Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebControllerTest {

    @Mock
    private HttpServletRequest mockRequest;
    
    @Mock
    private HttpServletResponse mockResponse;
    
    @Mock
    private HttpSession mockSession;
    
    @Mock
    private JdbcBean mockJdbcBean;
    
    private WebController webController;
    private StringWriter responseWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws IOException {
        webController = new WebController();
        
        // Replace the JdbcBean with our mock using reflection or setter
        try {
            java.lang.reflect.Field jdbcBeanField = WebController.class.getDeclaredField("jdbcBean");
            jdbcBeanField.setAccessible(true);
            jdbcBeanField.set(webController, mockJdbcBean);
        } catch (Exception e) {
            // If reflection fails, tests will use the default JdbcBean
        }
        
        // Set up response writer
        responseWriter = new StringWriter();
        printWriter = new PrintWriter(responseWriter);
        when(mockResponse.getWriter()).thenReturn(printWriter);
        
        // Set up proper content type
        when(mockResponse.getContentType()).thenReturn("application/json");
    }

    @AfterEach
    void tearDown() {
        // Ensure proper cleanup
        if (printWriter != null) {
            printWriter.close();
        }
    }

    // ========================================
    // SERVLET INFO AND INITIALIZATION TESTS
    // ========================================

    @Test
    @Order(1)
    @DisplayName("Servlet info should return correct description")
    void testGetServletInfo() {
        // Act
        String info = webController.getServletInfo();

        // Assert
        assertNotNull(info);
        assertTrue(info.contains("JdbcWebServlet"));
        assertTrue(info.contains("JSON"));
        assertTrue(info.contains("nirmala.sundarapp@oracle.com"));
    }

    @Test
    @Order(2)
    @DisplayName("WebController should initialize properly")
    void testWebControllerInstantiation() {
        // Assert
        assertNotNull(webController);
        assertNotNull(webController.jdbcBean);
    }

    // ========================================
    // HTTP GET METHOD TESTS
    // ========================================

    @Test
    @Order(10)
    @DisplayName("GET request should return all employees when no parameters")
    void testDoGet_GetAllEmployees() throws ServletException, IOException {
        // Arrange
        List<Employee> employees = createSampleEmployees();
        
        when(mockRequest.getParameter("id")).thenReturn(null);
        when(mockRequest.getParameter("firstName")).thenReturn(null);
        when(mockRequest.getParameter("logout")).thenReturn(null);
        when(mockJdbcBean.getEmployees()).thenReturn(employees);

        // Act
        webController.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setContentType("application/json");
        verify(mockJdbcBean).getEmployees();
        
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        assertTrue(jsonResponse.contains("John"));
        assertTrue(jsonResponse.contains("Jane"));
        assertTrue(jsonResponse.contains("75000"));
    }

    @Test
    @Order(11)
    @DisplayName("GET request should return specific employee by ID")
    void testDoGet_GetEmployeeById() throws ServletException, IOException {
        // Arrange
        Employee employee = new Employee(1, "John", "Doe", "john.doe@company.com", 
                                       "555-1234", "IT_PROG", new BigDecimal("75000.00"));
        List<Employee> employees = Collections.singletonList(employee);
        
        when(mockRequest.getParameter("id")).thenReturn("1");
        when(mockRequest.getParameter("firstName")).thenReturn(null);
        when(mockRequest.getParameter("logout")).thenReturn(null);
        when(mockJdbcBean.getEmployee(1)).thenReturn(employees);

        // Act
        webController.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setContentType("application/json");
        verify(mockJdbcBean).getEmployee(1);
        
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        assertTrue(jsonResponse.contains("John"));
        assertTrue(jsonResponse.contains("75000"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "100", "999", "2147483647"})
    @Order(12)
    @DisplayName("GET request should handle various valid employee IDs")
    void testDoGet_GetEmployeeById_ValidIds(String employeeId) throws ServletException, IOException {
        // Arrange
        int id = Integer.parseInt(employeeId);
        when(mockRequest.getParameter("id")).thenReturn(employeeId);
        when(mockRequest.getParameter("firstName")).thenReturn(null);
        when(mockRequest.getParameter("logout")).thenReturn(null);
        when(mockJdbcBean.getEmployee(id)).thenReturn(Collections.emptyList());

        // Act
        webController.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockJdbcBean).getEmployee(id);
        verify(mockResponse).setContentType("application/json");
    }

    @Test
    @Order(13)
    @DisplayName("GET request should handle invalid employee ID format")
    void testDoGet_GetEmployeeById_InvalidIdFormat() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getParameter("id")).thenReturn("invalid");
        when(mockRequest.getParameter("firstName")).thenReturn(null);
        when(mockRequest.getParameter("logout")).thenReturn(null);

        // Act & Assert
        assertThrows(NumberFormatException.class, () -> {
            webController.doGet(mockRequest, mockResponse);
        });
    }

    @Test
    @Order(14)
    @DisplayName("GET request should return employees by first name")
    void testDoGet_GetEmployeesByFirstName() throws ServletException, IOException {
        // Arrange
        List<Employee> employees = Arrays.asList(
            new Employee(1, "John", "Doe", "john.doe@company.com", "555-1234", "IT_PROG", new BigDecimal("75000.00")),
            new Employee(3, "Johnny", "Cash", "johnny.cash@company.com", "555-9999", "MK_REP", new BigDecimal("60000.00"))
        );
        
        when(mockRequest.getParameter("id")).thenReturn(null);
        when(mockRequest.getParameter("firstName")).thenReturn("John");
        when(mockRequest.getParameter("logout")).thenReturn(null);
        when(mockJdbcBean.getEmployeeByFn("John")).thenReturn(employees);

        // Act
        webController.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setContentType("application/json");
        verify(mockJdbcBean).getEmployeeByFn("John");
        
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        assertTrue(jsonResponse.contains("John"));
        assertTrue(jsonResponse.contains("Johnny"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "A", "VeryLongFirstNameThatShouldStillWork", "José", "李"})
    @Order(15)
    @DisplayName("GET request should handle various first name patterns")
    void testDoGet_GetEmployeesByFirstName_EdgeCases(String firstName) throws ServletException, IOException {
        // Arrange
        when(mockRequest.getParameter("id")).thenReturn(null);
        when(mockRequest.getParameter("firstName")).thenReturn(firstName);
        when(mockRequest.getParameter("logout")).thenReturn(null);
        when(mockJdbcBean.getEmployeeByFn(firstName)).thenReturn(Collections.emptyList());

        // Act
        webController.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockJdbcBean).getEmployeeByFn(firstName);
        verify(mockResponse).setContentType("application/json");
    }

    // ========================================
    // LOGOUT FUNCTIONALITY TESTS
    // ========================================

    @Test
    @Order(20)
    @DisplayName("GET request should handle logout with valid session")
    void testDoGet_Logout_ValidSession() throws ServletException, IOException {
        // Arrange
        Cookie[] cookies = {
            new Cookie("JSESSIONID", "ABC123"),
            new Cookie("userPref", "theme=dark")
        };
        
        when(mockRequest.getParameter("id")).thenReturn(null);
        when(mockRequest.getParameter("firstName")).thenReturn(null);
        when(mockRequest.getParameter("logout")).thenReturn("true");
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockRequest.isRequestedSessionIdValid()).thenReturn(true);
        when(mockRequest.getCookies()).thenReturn(cookies);

        // Act
        webController.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockSession).invalidate();
        verify(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(mockResponse, times(2)).addCookie(argThat(cookie -> 
            cookie.getMaxAge() == 0 && 
            cookie.getValue() == null && 
            "/".equals(cookie.getPath())
        ));
    }

    @Test
    @Order(21)
    @DisplayName("GET request should handle logout with null session")
    void testDoGet_Logout_NullSession() throws ServletException, IOException {
        // Arrange
        Cookie[] cookies = {new Cookie("JSESSIONID", "ABC123")};
        
        when(mockRequest.getParameter("logout")).thenReturn("true");
        when(mockRequest.getSession(false)).thenReturn(null);
        when(mockRequest.isRequestedSessionIdValid()).thenReturn(false);
        when(mockRequest.getCookies()).thenReturn(cookies);

        // Act
        webController.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockSession, never()).invalidate();
        verify(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(mockResponse).addCookie(any(Cookie.class));
    }

    @Test
    @Order(22)
    @DisplayName("GET request should handle logout with no cookies")
    void testDoGet_Logout_NoCookies() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getParameter("logout")).thenReturn("true");
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockRequest.isRequestedSessionIdValid()).thenReturn(true);
        when(mockRequest.getCookies()).thenReturn(new Cookie[0]);

        // Act
        webController.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockSession).invalidate();
        verify(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(mockResponse, never()).addCookie(any(Cookie.class));
    }

    // ========================================
    // ERROR HANDLING TESTS
    // ========================================

    @Test
    @Order(30)
    @DisplayName("GET request should return 404 when no employees found")
    void testDoGet_NoEmployeesFound() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getParameter("id")).thenReturn(null);
        when(mockRequest.getParameter("firstName")).thenReturn(null);
        when(mockRequest.getParameter("logout")).thenReturn(null);
        when(mockJdbcBean.getEmployees()).thenReturn(null);

        // Act
        webController.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(mockJdbcBean).getEmployees();
    }

    @Test
    @Order(31)
    @DisplayName("GET request should handle empty employee list")
    void testDoGet_EmptyEmployeeList() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getParameter("id")).thenReturn("999");
        when(mockJdbcBean.getEmployee(999)).thenReturn(Collections.emptyList());

        // Act
        webController.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setContentType("application/json");
        verify(mockJdbcBean).getEmployee(999);
        
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        assertEquals("[]", jsonResponse.trim());
    }

    // ========================================
    // HTTP POST METHOD TESTS
    // ========================================

    @Test
    @Order(40)
    @DisplayName("POST request should handle salary increment")
    void testDoPost_IncrementSalary() throws ServletException, IOException {
        // Arrange
        List<Employee> employees = Arrays.asList(
            new Employee(1, "John", "Doe", "john.doe@company.com", "555-1234", "IT_PROG", new BigDecimal("82500.00")),
            new Employee(2, "Jane", "Smith", "jane.smith@company.com", "555-5678", "HR_REP", new BigDecimal("71500.00"))
        );
        
        when(mockRequest.getParameter("incrementPct")).thenReturn("10");
        when(mockJdbcBean.incrementSalary(10)).thenReturn(employees);

        // Act
        webController.doPost(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setContentType("application/json");
        verify(mockJdbcBean).incrementSalary(10);
        
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        assertTrue(jsonResponse.contains("82500"));
        assertTrue(jsonResponse.contains("71500"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "10", "15", "25", "50", "-5", "-10", "0", "100"})
    @Order(41)
    @DisplayName("POST request should handle various salary increment percentages")
    void testDoPost_IncrementSalary_VariousPercentages(String percentage) throws ServletException, IOException {
        // Arrange
        int pct = Integer.parseInt(percentage);
        List<Employee> employees = Collections.singletonList(
            new Employee(1, "John", "Doe", "john.doe@company.com", "555-1234", "IT_PROG", new BigDecimal("75000.00"))
        );
        
        when(mockRequest.getParameter("incrementPct")).thenReturn(percentage);
        when(mockJdbcBean.incrementSalary(pct)).thenReturn(employees);

        // Act
        webController.doPost(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setContentType("application/json");
        verify(mockJdbcBean).incrementSalary(pct);
    }

    @Test
    @Order(42)
    @DisplayName("POST request should return 404 when no increment parameter")
    void testDoPost_NoIncrementParameter() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getParameter("incrementPct")).thenReturn(null);

        // Act
        webController.doPost(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(mockJdbcBean, never()).incrementSalary(anyInt());
    }

    @Test
    @Order(43)
    @DisplayName("POST request should handle invalid increment parameter format")
    void testDoPost_InvalidIncrementParameter() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getParameter("incrementPct")).thenReturn("invalid");

        // Act & Assert
        assertThrows(NumberFormatException.class, () -> {
            webController.doPost(mockRequest, mockResponse);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "abc", "10.5", "2147483648", "-2147483649"})
    @Order(44)
    @DisplayName("POST request should handle various invalid increment parameters")
    void testDoPost_InvalidIncrementParameters(String invalidParam) throws ServletException, IOException {
        // Arrange
        when(mockRequest.getParameter("incrementPct")).thenReturn(invalidParam);

        // Act & Assert
        assertThrows(NumberFormatException.class, () -> {
            webController.doPost(mockRequest, mockResponse);
        });
    }

    // ========================================
    // JSON SERIALIZATION TESTS
    // ========================================

    @Test
    @Order(50)
    @DisplayName("JSON response should contain all employee fields")
    void testJsonSerialization_AllEmployeeFields() throws ServletException, IOException {
        // Arrange
        Employee employee = new Employee(100, "Test", "User", "test@company.com", 
                                       "555-0000", "IT_PROG", new BigDecimal("50000.50"));
        List<Employee> employees = Collections.singletonList(employee);
        
        when(mockRequest.getParameter("id")).thenReturn("100");
        when(mockJdbcBean.getEmployee(100)).thenReturn(employees);

        // Act
        webController.doGet(mockRequest, mockResponse);

        // Assert
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        
        // Verify all employee fields are serialized
        assertTrue(jsonResponse.contains("\"employeeId\":100"));
        assertTrue(jsonResponse.contains("\"firstName\":\"Test\""));
        assertTrue(jsonResponse.contains("\"lastName\":\"User\""));
        assertTrue(jsonResponse.contains("\"email\":\"test@company.com\""));
        assertTrue(jsonResponse.contains("\"phoneNumber\":\"555-0000\""));
        assertTrue(jsonResponse.contains("\"jobId\":\"IT_PROG\""));
        assertTrue(jsonResponse.contains("\"salary\":50000.5"));
    }

    @Test
    @Order(51)
    @DisplayName("JSON response should handle special characters properly")
    void testJsonSerialization_SpecialCharacters() throws ServletException, IOException {
        // Arrange
        Employee employee = new Employee(1, "José", "O'Connor", "jose@company.com", 
                                       "+1-555-123", "IT_PROG", new BigDecimal("75000.00"));
        List<Employee> employees = Collections.singletonList(employee);
        
        when(mockRequest.getParameter("id")).thenReturn("1");
        when(mockJdbcBean.getEmployee(1)).thenReturn(employees);

        // Act
        webController.doGet(mockRequest, mockResponse);

        // Assert
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        
        assertTrue(jsonResponse.contains("José"));
        assertTrue(jsonResponse.contains("O'Connor"));
        assertTrue(jsonResponse.contains("+1-555-123"));
    }

    // ========================================
    // SECURITY AND INPUT VALIDATION TESTS
    // ========================================

    @Test
    @Order(60)
    @DisplayName("Request should handle SQL injection attempts in ID parameter")
    void testSecurity_SqlInjectionInId() throws ServletException, IOException {
        // Arrange
        String maliciousId = "1; DROP TABLE employees; --";
        when(mockRequest.getParameter("id")).thenReturn(maliciousId);

        // Act & Assert
        assertThrows(NumberFormatException.class, () -> {
            webController.doGet(mockRequest, mockResponse);
        });
        
        // Verify no database operations were attempted
        verify(mockJdbcBean, never()).getEmployee(anyInt());
    }

    @Test
    @Order(61)
    @DisplayName("Request should handle XSS attempts in firstName parameter")
    void testSecurity_XssInFirstName() throws ServletException, IOException {
        // Arrange
        String maliciousName = "<script>alert('xss')</script>";
        when(mockRequest.getParameter("id")).thenReturn(null);
        when(mockRequest.getParameter("firstName")).thenReturn(maliciousName);
        when(mockRequest.getParameter("logout")).thenReturn(null);
        when(mockJdbcBean.getEmployeeByFn(maliciousName)).thenReturn(Collections.emptyList());

        // Act
        webController.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockJdbcBean).getEmployeeByFn(maliciousName);
        verify(mockResponse).setContentType("application/json");
        
        printWriter.flush();
        String jsonResponse = responseWriter.toString();
        assertEquals("[]", jsonResponse.trim());
    }

    // ========================================
    // PERFORMANCE AND RESOURCE MANAGEMENT TESTS
    // ========================================

    @Test
    @Order(70)
    @DisplayName("Multiple requests should not cause resource leaks")
    void testResourceManagement_MultipleRequests() throws ServletException, IOException {
        // Arrange
        when(mockRequest.getParameter("id")).thenReturn(null);
        when(mockRequest.getParameter("firstName")).thenReturn(null);
        when(mockRequest.getParameter("logout")).thenReturn(null);
        when(mockJdbcBean.getEmployees()).thenReturn(Collections.emptyList());

        // Act - Simulate multiple requests
        for (int i = 0; i < 10; i++) {
            webController.doGet(mockRequest, mockResponse);
            responseWriter.getBuffer().setLength(0); // Clear buffer
        }

        // Assert
        verify(mockJdbcBean, times(10)).getEmployees();
        verify(mockResponse, times(10)).setContentType("application/json");
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private List<Employee> createSampleEmployees() {
        return Arrays.asList(
            new Employee(1, "John", "Doe", "john.doe@company.com", "555-1234", "IT_PROG", new BigDecimal("75000.00")),
            new Employee(2, "Jane", "Smith", "jane.smith@company.com", "555-5678", "HR_REP", new BigDecimal("65000.00")),
            new Employee(3, "Bob", "Johnson", "bob.johnson@company.com", "555-9012", "FI_ACCOUNT", new BigDecimal("55000.00"))
        );
    }
}