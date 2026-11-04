package io.daytrader.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public record ActionRequest(String action, HttpServletRequest http, HttpServletResponse response, String userID) {
    public String param(String name) { return http.getParameter(name); }
    public HttpSession session() { return http.getSession(); }
}
