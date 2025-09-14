package com.hrapp.jdbc.samples.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for authentication flow between GetRole and SimpleLogoutServlet.
 * Tests the complete authentication lifecycle including role determination and logout.
 */
class AuthenticationIntegrationTest {

    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private HttpSession session;
    
    private GetRole getRoleServlet;
    private SimpleLogoutServlet logoutServlet;
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        getRoleServlet = new GetRole();
        logoutServlet = new SimpleLogoutServlet();
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    void testCompleteAuthenticationFlow_ManagerUser() throws Exception {
        // Test manager login and role determination
        when(request.isUserInRole("manager")).thenReturn(true);
        when(request.isUserInRole("staff")).thenReturn(false);
        
        // Get role
        getRoleServlet.doGet(request, response);
        printWriter.flush();
        
        assertEquals("manager", stringWriter.toString());
        verify(response).setContentType("text/css");
        
        // Reset for logout test
        stringWriter.getBuffer().setLength(0);
        when(request.getSession(false)).thenReturn(session);
        when(request.getCookies()).thenReturn(null);
        
        // Logout
        logoutServlet.doGet(request, response);
        printWriter.flush();
        
        verify(session).invalidate();
        assertEquals("{\"status\":\"logged_out\"}", stringWriter.toString());
    }

    @Test
    void testCompleteAuthenticationFlow_StaffUser() throws Exception {
        // Test staff login and role determination
        when(request.isUserInRole("manager")).thenReturn(false);
        when(request.isUserInRole("staff")).thenReturn(true);
        
        // Get role
        getRoleServlet.doGet(request, response);
        printWriter.flush();
        
        assertEquals("staff", stringWriter.toString());
        verify(response).setContentType("text/css");
        
        // Reset for logout test
        stringWriter.getBuffer().setLength(0);
        when(request.getSession(false)).thenReturn(session);
        when(request.getCookies()).thenReturn(null);
        
        // Logout
        logoutServlet.doGet(request, response);
        printWriter.flush();
        
        verify(session).invalidate();
        assertEquals("{\"status\":\"logged_out\"}", stringWriter.toString());
    }

    @Test
    void testAuthenticationFlow_UnauthorizedUser() throws Exception {
        // Test unauthorized user
        when(request.isUserInRole("manager")).thenReturn(false);
        when(request.isUserInRole("staff")).thenReturn(false);
        
        // Get role
        getRoleServlet.doGet(request, response);
        printWriter.flush();
        
        assertEquals("anonymous", stringWriter.toString());
        
        // Even anonymous users can logout (cleanup)
        stringWriter.getBuffer().setLength(0);
        when(request.getSession(false)).thenReturn(null);
        when(request.getCookies()).thenReturn(null);
        
        // Logout
        logoutServlet.doGet(request, response);
        printWriter.flush();
        
        assertEquals("{\"status\":\"logged_out\"}", stringWriter.toString());
    }

    @Test
    void testRoleBasedAccessControl() throws Exception {
        // Test that manager role takes precedence
        when(request.isUserInRole("manager")).thenReturn(true);
        when(request.isUserInRole("staff")).thenReturn(true);
        
        getRoleServlet.doGet(request, response);
        printWriter.flush();
        
        // Manager should be returned even if user also has staff role
        assertEquals("manager", stringWriter.toString());
    }

    @Test
    void testSessionManagement() throws Exception {
        // Test session handling during logout
        when(request.getSession(false)).thenReturn(session);
        when(request.getCookies()).thenReturn(null);
        
        logoutServlet.doGet(request, response);
        
        // Verify session is properly invalidated
        verify(session, times(1)).invalidate();
        verify(response).setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        verify(response).setHeader("Pragma", "no-cache");
        verify(response).setDateHeader("Expires", 0);
    }
}