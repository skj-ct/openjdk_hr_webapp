/*
 * HR Web Application - OpenJDK Migration
 * GetRole servlet for role-based access control
 * Migrated from Oracle to PostgreSQL backend
 */
package com.hrapp.jdbc.samples.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet for determining user role for authentication and authorization
 * Returns the role of the authenticated user (manager, staff, or anonymous)
 * 
 * @author HR Web Application - OpenJDK Migration
 */
@WebServlet(name = "GetRole", urlPatterns = {"/getrole"})
public class GetRole extends HttpServlet {

    private static final String[] ROLES = {"manager", "staff"};

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/css");
        String returnValue = "anonymous";
        
        for (String role : ROLES) {
            if (request.isUserInRole(role)) {
                returnValue = role;
                break;
            }
        }

        response.getWriter().print(returnValue);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "HR Web Application GetRole: Role detection servlet for authentication and authorization";
    }
}