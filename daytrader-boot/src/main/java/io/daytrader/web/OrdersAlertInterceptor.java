package io.daytrader.web;

import io.daytrader.service.TradeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Collection;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OrdersAlertInterceptor implements HandlerInterceptor {
    private final TradeService tradeService;

    public OrdersAlertInterceptor(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String action = request.getParameter("action");
            if (action == null || action.trim().isEmpty() || "logout".equals(action.trim())) return true;
            String userID = "login".equals(action.trim()) ? request.getParameter("uid") : null;
            if (userID == null) {
                HttpSession session = request.getSession(false);
                if (session != null) userID = (String) session.getAttribute(SessionKeys.UID_BEAN);
            }
            if (userID != null && !userID.isBlank()) {
                Collection<?> closedOrders = tradeService.getClosedOrders(userID);
                if (closedOrders != null && !closedOrders.isEmpty()) {
                    request.setAttribute("closedOrders", closedOrders);
                }
            }
        } catch (Exception ignored) {
        }
        return true;
    }
}
