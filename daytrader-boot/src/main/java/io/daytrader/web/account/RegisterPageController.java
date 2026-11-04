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

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RegisterPageController {
    private static final String FAKE_CREDIT_CARD = "123-fake-ccnum-456";

    @GetMapping("/register.jsp")
    public String registerPage(HttpServletRequest request, Model model) {
        populateModel(request, model, "");
        return "register";
    }

    static void populateModel(HttpServletRequest request, Model model, String results) {
        model.addAttribute("fullname", valueOrDefault(request.getParameter("Full Name"), ""));
        model.addAttribute("snailmail", valueOrDefault(request.getParameter("snail mail"), ""));
        model.addAttribute("email", valueOrDefault(request.getParameter("email"), ""));
        model.addAttribute("userID", valueOrDefault(request.getParameter("user id"), ""));
        model.addAttribute("money", valueOrDefault(request.getParameter("money"), "10000"));
        model.addAttribute("creditcard", valueOrDefault(request.getParameter("Credit Card Number"), FAKE_CREDIT_CARD));
        model.addAttribute("results", results == null ? "" : results);
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }
}
