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
package io.daytrader.web.auth;

import io.daytrader.entity.AccountDataBean;
import io.daytrader.service.TradeService;
import io.daytrader.util.Log;
import io.daytrader.web.ActionHandler;
import io.daytrader.web.ActionRequest;
import io.daytrader.web.PageRenderer;
import io.daytrader.web.SessionKeys;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpSession;
import java.util.Date;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class AuthActionHandler implements ActionHandler {
    private final TradeService tradeService;
    private final PageRenderer pages;

    public AuthActionHandler(TradeService tradeService, PageRenderer pages) {
        this.tradeService = tradeService;
        this.pages = pages;
    }

    @Override
    public Set<String> actions() {
        return Set.of("login", "logout");
    }

    @Override
    public String handle(ActionRequest request, Model model) throws Exception {
        return switch (request.action()) {
            case "login" -> login(request, model);
            case "logout" -> logout(request, model);
            default -> throw new IllegalArgumentException("Unsupported auth action: " + request.action());
        };
    }

    private String login(ActionRequest request, Model model) throws ServletException {
        String userID = request.param("uid");
        String passwd = request.param("passwd");
        AccountDataBean accountData;
        try {
            accountData = tradeService.login(userID, passwd);
        } catch (RuntimeException e) {
            throw new ServletException("TradeServletAction.doLogin(...)Exception logging in user " + userID, e);
        }
        if (accountData == null) {
            Log.log("TradeServletAction.doLogin(...): Error finding account for user " + userID
                    + "; user entered a bad username or the database is not populated");
            return pages.welcome(model, "\nCould not find account for + " + userID);
        }
        HttpSession session = request.http().getSession(true);
        session.setAttribute(SessionKeys.UID_BEAN, userID);
        session.setAttribute(SessionKeys.SESSION_CREATION_DATE, new Date());
        return pages.home(new ActionRequest("home", request.http(), request.response(), userID), model, "Ready to Trade");
    }

    private String logout(ActionRequest request, Model model) throws ServletException {
        String userID = request.userID();
        String results = "";
        try {
            tradeService.logout(userID);
        } catch (IllegalArgumentException e) {
            results = results + "illegal argument:" + e.getMessage();
            Log.error(e, "TradeServletAction.doLogout(...): illegal argument, information should be in exception string; "
                    + "treating this as a user error and forwarding on to a new page");
        } catch (RuntimeException e) {
            Log.error(e, "TradeServletAction.doLogout(...): Error logging out" + userID + "; "
                    + "fowarding to an error page");
            throw new ServletException("TradeServletAction.doLogout(...)exception logging out user " + userID, e);
        }
        HttpSession session = request.http().getSession(false);
        if (session != null) {
            session.invalidate();
        }
        if (Boolean.TRUE.equals(request.http().getAttribute("TSS-RecreateSessionInLogout"))) {
            request.http().getSession(true);
        }
        return pages.welcome(model, results);
    }
}
