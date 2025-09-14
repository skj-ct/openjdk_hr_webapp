/*
 * HR Web Application - OpenJDK Migration
 * WebController servlet for handling employee management operations
 * Migrated from Oracle to PostgreSQL backend
 */
package com.hrapp.jdbc.samples.web;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hrapp.jdbc.samples.bean.JdbcBean;
import com.hrapp.jdbc.samples.bean.JdbcBeanImpl;
import com.hrapp.jdbc.samples.entity.Employee;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main web controller for HR application employee management
 * Handles GET and POST requests for employee CRUD operations
 * 
 * @author HR Web Application - OpenJDK Migration
 */
@WebServlet(name = "WebController", urlPatterns = {"/WebController"})
public class WebController extends HttpServlet {

    private static final String INCREMENT_PCT = "incrementPct";
    private static final String ID_KEY = "id";
    private static final String FN_KEY = "firstName";
    private static final String LOGOUT = "logout";

    JdbcBean jdbcBean = new JdbcBeanImpl();

    private void reportError(HttpServletResponse response, String message)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet WebController</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>" + message + "</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Gson gson = new Gson();

        String value = null;
        List<Employee> employeeList = null;
        
        if ((value = request.getParameter(ID_KEY)) != null) {
            int empId = Integer.valueOf(value).intValue();
            employeeList = jdbcBean.getEmployee(empId);
        } else if ((value = request.getParameter(FN_KEY)) != null) {
            employeeList = jdbcBean.getEmployeeByFn(value);
        } else if ((value = request.getParameter(LOGOUT)) != null) {
            // Handle logout
            HttpSession session = request.getSession(false);
            if (request.isRequestedSessionIdValid() && session != null) {
                session.invalidate();
            }
            handleLogOutResponse(request, response);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            employeeList = jdbcBean.getEmployees();
        }

        if (employeeList != null) {
            response.setContentType("application/json");
            gson.toJson(employeeList,
                    new TypeToken<ArrayList<Employee>>() {
                    }.getType(),
                    response.getWriter());
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * This method would edit the cookie information and make JSESSIONID empty
     * while responding to logout. This would help to avoid same cookie ID each time a person logs in.
     * 
     * @param request servlet request
     * @param response servlet response
     */
    private void handleLogOutResponse(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookie.setMaxAge(0);
                cookie.setValue(null);
                cookie.setPath("/");
                response.addCookie(cookie);
            }
        }
    }

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
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Map<String, String[]> x = request.getParameterMap();
        String value = null;
        
        if ((value = request.getParameter(INCREMENT_PCT)) != null) {
            Gson gson = new Gson();
            response.setContentType("application/json");
            List<Employee> employeeList = jdbcBean.incrementSalary(Integer.valueOf(value));
            gson.toJson(employeeList,
                    new TypeToken<ArrayList<Employee>>() {
                    }.getType(),
                    response.getWriter());
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "HR Web Application WebController: Employee management using PostgreSQL backend with OpenJDK 21";
    }
}