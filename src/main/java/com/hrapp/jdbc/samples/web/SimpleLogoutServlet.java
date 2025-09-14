/*
 * HR Web Application - OpenJDK Migration
 * SimpleLogoutServlet for secure logout functionality
 * Migrated from Oracle to PostgreSQL backend
 */
package com.hrapp.jdbc.samples.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Cookie;
import java.io.IOException;

/**
 * Simple Logout Servlet that handles logout functionality safely
 * without null pointer exceptions.
 * 
 * @author HR Web Application - OpenJDK Migration
 */
@WebServlet(name = "SimpleLogoutServlet", urlPatterns = {"/simplelogout"})
public class SimpleLogoutServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Invalidate session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        
        // Clear cookies safely (with null check to prevent NPE)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookie.setMaxAge(0);
                cookie.setValue("");
                cookie.setPath("/");
                response.addCookie(cookie);
            }
        }
        
        // Set no-cache headers
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        
        // Send JSON response for AJAX calls
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":\"logged_out\"}");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
    
    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "HR Web Application SimpleLogoutServlet: Secure logout functionality with session and cookie management";
    }
}