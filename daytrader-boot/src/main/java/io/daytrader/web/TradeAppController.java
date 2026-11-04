package io.daytrader.web;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class TradeAppController {
    private final List<ActionHandler> handlers;
    private final PageRenderer pages;
    private final Map<String, ActionHandler> byAction = new HashMap<>();

    public TradeAppController(List<ActionHandler> handlers, PageRenderer pages) {
        this.handlers = handlers;
        this.pages = pages;
    }

    @PostConstruct
    void registerHandlers() {
        for (ActionHandler handler : handlers) {
            for (String action : handler.actions()) {
                ActionHandler previous = byAction.putIfAbsent(action, handler);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate DayTrader action: " + action);
                }
            }
        }
    }

    @RequestMapping(path = "/app", method = {RequestMethod.GET, RequestMethod.POST})
    public String service(HttpServletRequest http, HttpServletResponse response, Model model) {
        response.setContentType("text/html");
        String action = http.getParameter("action");
        if (action == null) {
            return pages.welcome(model, "");
        }

        ActionHandler handler = byAction.get(action);
        if ("login".equals(action) || "register".equals(action)) {
            if (handler == null) return pages.welcome(model, "TradeAppServlet: Invalid Action" + action);
            return invoke(handler, new ActionRequest(action, http, response, null), response, model);
        }

        HttpSession session = http.getSession();
        String userID = (String) session.getAttribute(SessionKeys.UID_BEAN);
        if (userID == null || userID.isBlank()) {
            return pages.welcome(model, "User Not Logged in");
        }
        if (handler == null) return pages.welcome(model, "TradeAppServlet: Invalid Action" + action);
        return invoke(handler, new ActionRequest(action, http, response, userID), response, model);
    }

    private String invoke(ActionHandler handler, ActionRequest request, HttpServletResponse response, Model model) {
        try {
            return handler.handle(request, model);
        } catch (Exception exception) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            model.addAttribute("requestUri", request.http().getRequestURI());
            model.addAttribute("exceptionMessage", exception.getMessage());
            StringWriter trace = new StringWriter();
            exception.printStackTrace(new PrintWriter(trace));
            model.addAttribute("stackTrace", trace.toString());
            return "error";
        }
    }
}
