/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.daytrader.web.account;

import io.daytrader.entity.AccountDataBean;
import io.daytrader.entity.AccountProfileDataBean;
import io.daytrader.entity.OrderDataBean;
import io.daytrader.service.TradeService;
import io.daytrader.util.FinancialUtils;
import io.daytrader.util.Log;
import io.daytrader.web.ActionHandler;
import io.daytrader.web.ActionRequest;
import io.daytrader.web.PageRenderer;
import io.daytrader.web.SessionKeys;
import jakarta.persistence.EntityManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class AccountHandlers implements ActionHandler {
    // Mirrors legacy TradeConfig.longRun's default (true), so the legacy account page shows "Total Orders: 0".
    private static final boolean LONG_RUN = true;

    private final TradeService tradeService;
    private final PageRenderer pages;
    private final EntityManager entityManager;

    public AccountHandlers(TradeService tradeService, PageRenderer pages, EntityManager entityManager) {
        this.tradeService = tradeService;
        this.pages = pages;
        this.entityManager = entityManager;
    }

    @Override
    public Set<String> actions() {
        return Set.of("register", "account", "update_profile");
    }

    @Override
    public String handle(ActionRequest request, Model model) throws Exception {
        return switch (request.action()) {
            case "register" -> register(request, model);
            case "account" -> account(request, model, "");
            case "update_profile" -> updateProfile(request, model);
            default -> throw new IllegalArgumentException("Unsupported account action: " + request.action());
        };
    }

    private String register(ActionRequest request, Model model) throws ServletException {
        String userID = request.param("user id");
        try {
            String password = request.param("passwd");
            String confirmPassword = request.param("confirm passwd");
            if (password.equals(confirmPassword) && password.length() >= 1) {
                AccountDataBean account = tradeService.register(userID, password, request.param("Full Name"),
                        request.param("snail mail"), request.param("email"), request.param("Credit Card Number"),
                        new BigDecimal(request.param("money")));
                if (account == null) {
                    String results = "Registration operation failed;";
                    System.out.println(results);
                    RegisterPageController.populateModel(request.http(), model, results);
                    return "register";
                }
                entityManager.clear();
                tradeService.login(userID, password);
                HttpSession session = request.http().getSession(true);
                session.setAttribute(SessionKeys.UID_BEAN, userID);
                session.setAttribute(SessionKeys.SESSION_CREATION_DATE, new Date());
                return pages.home(new ActionRequest("home", request.http(), request.response(), userID),
                        model, "Ready to Trade");
            }

            String results = "Registration operation failed, your passwords did not match";
            System.out.println(results);
            RegisterPageController.populateModel(request.http(), model, results);
            return "register";
        } catch (Exception e) {
            throw new ServletException("TradeServletAction.doRegister(...) exception user =" + userID, e);
        }
    }

    private String account(ActionRequest request, Model model, String results) throws ServletException {
        String userID = request.userID();
        try {
            AccountDataBean accountData = tradeService.getAccountData(userID);
            AccountProfileDataBean accountProfileData = tradeService.getAccountProfileData(userID);
            List<OrderDataBean> orders = LONG_RUN
                    ? new ArrayList<>()
                    : new ArrayList<>(tradeService.getOrders(userID));
            boolean showAllOrders = request.param("showAllOrders") != null;
            List<OrderRow> orderRows = orderRows(orders, showAllOrders);
            Collection<?> closedOrders = (Collection<?>) request.http().getAttribute("closedOrders");
            List<OrderRow> alertRows = alertRows(closedOrders);

            model.addAttribute("accountData", accountData);
            model.addAttribute("accountProfileData", accountProfileData);
            model.addAttribute("results", results);
            model.addAttribute("pageDate", new Date());
            model.addAttribute("accountCreated", legacyDate(accountData.getCreationDate()));
            model.addAttribute("lastLogin", legacyDate(accountData.getLastLogin()));
            model.addAttribute("orderCount", orders.size());
            model.addAttribute("orderRows", orderRows);
            model.addAttribute("alertRows", alertRows);
            return "account";
        } catch (IllegalArgumentException e) {
            Log.error(e, "TradeServletAction.doAccount(...): illegal argument, information should be in exception string");
            return pages.home(request, model, results + "could not find account for userID = " + userID);
        } catch (Exception e) {
            throw new ServletException("TradeServletAction.doAccount(...) exception user =" + userID, e);
        }
    }

    private String updateProfile(ActionRequest request, Model model) throws ServletException {
        String userID = request.userID();
        String results = "";
        try {
            String password = request.param("password");
            String confirmPassword = request.param("cpassword");
            String fullName = request.param("fullname");
            String address = request.param("address");
            String creditCard = request.param("creditcard");
            String email = request.param("email");

            boolean doUpdate = true;
            if (!password.equals(confirmPassword)) {
                results = "Update profile error: passwords do not match";
                doUpdate = false;
            } else if (password.length() <= 0 || fullName.length() <= 0 || address.length() <= 0
                    || creditCard.length() <= 0 || email.length() <= 0) {
                results = "Update profile error: please fill in all profile information fields";
                doUpdate = false;
            }

            AccountProfileDataBean profile = new AccountProfileDataBean(
                    userID, password, fullName, address, email, creditCard);
            if (doUpdate) {
                tradeService.updateAccountProfile(profile);
                results = "Account profile update successful";
            }
        } catch (IllegalArgumentException e) {
            Log.error(e, "TradeServletAction.doAccount(...): illegal argument, information should be in exception string");
        } catch (Exception e) {
            throw new ServletException("TradeServletAction.doAccountUpdate(...) exception user =" + userID, e);
        }
        return account(request, model, results);
    }

    private static List<OrderRow> orderRows(Collection<OrderDataBean> orders, boolean showAllOrders) {
        List<OrderRow> rows = new ArrayList<>();
        int count = 0;
        for (OrderDataBean order : orders) {
            if (!showAllOrders && count++ >= 5) break;
            BigDecimal price = order.getPrice();
            rows.add(new OrderRow(order.getOrderID(), order.getOrderStatus(), legacyDate(order.getOpenDate()),
                    legacyDate(order.getCompletionDate()), order.getOrderFee(), order.getOrderType(),
                    FinancialUtils.printQuoteLink(order.getSymbol()), order.getQuantity(), price,
                    price.multiply(new BigDecimal(order.getQuantity()))));
        }
        return rows;
    }

    private static List<OrderRow> alertRows(Collection<?> closedOrders) {
        List<OrderRow> rows = new ArrayList<>();
        if (closedOrders == null) return rows;
        for (Object closedOrder : closedOrders) {
            OrderDataBean order = (OrderDataBean) closedOrder;
            BigDecimal price = order.getPrice();
            rows.add(new OrderRow(order.getOrderID(), order.getOrderStatus(), legacyDate(order.getOpenDate()),
                    legacyDate(order.getCompletionDate()), order.getOrderFee(), order.getOrderType(),
                    FinancialUtils.printQuoteLink(order.getSymbol()), order.getQuantity(), price,
                    price.multiply(new BigDecimal(order.getQuantity()))));
        }
        return rows;
    }

    static String legacyDate(Date date) {
        return date == null ? "null" : new Date(date.getTime()).toString();
    }

    public record OrderRow(Integer orderID, String orderStatus, String openDate, String completionDate,
                           BigDecimal orderFee, String orderType, String symbolLink, double quantity,
                           BigDecimal price, BigDecimal total) { }
}
