/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.daytrader.web.market;

import io.daytrader.entity.QuoteDataBean;
import io.daytrader.service.TradeService;
import io.daytrader.util.FinancialUtils;
import io.daytrader.util.Log;
import io.daytrader.web.ActionHandler;
import io.daytrader.web.ActionRequest;
import io.daytrader.web.PageRenderer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/** Ports TradeAppServlet's "home" and "quotes" actions (doHome, doQuotes, quote.jsp, displayQuote.jsp). */
@Component
public class MarketHandlers implements ActionHandler {
    static final String NO_SYMBOL = "no_stock_symbol_provided";

    private final TradeService tradeService;
    private final PageRenderer pages;

    public MarketHandlers(TradeService tradeService, PageRenderer pages) {
        this.tradeService = tradeService;
        this.pages = pages;
    }

    @Override
    public Set<String> actions() {
        return Set.of("home", "quotes");
    }

    @Override
    public String handle(ActionRequest request, Model model) {
        return switch (request.action()) {
            case "home" -> pages.home(request, model, "");
            case "quotes" -> quotes(request, model);
            default -> throw new IllegalArgumentException("Unsupported market action: " + request.action());
        };
    }

    private String quotes(ActionRequest request, Model model) {
        String symbols = request.param("symbols");
        if (symbols == null) symbols = NO_SYMBOL;
        List<QuoteRow> rows = new ArrayList<>();
        StringTokenizer tokens = new StringTokenizer(symbols, " ,");
        while (tokens.hasMoreTokens()) {
            String symbol = tokens.nextToken();
            try {
                rows.add(QuoteRow.of(tradeService.getQuote(symbol)));
            } catch (Exception e) {
                Log.error("displayQuote.jsp  exception", e);
            }
        }
        model.addAttribute("pageDate", new Date());
        model.addAttribute("closedOrders", request.http().getAttribute("closedOrders"));
        model.addAttribute("quoteRows", rows);
        return "quote";
    }

    /** One displayQuote.jsp row, with every cell already converted to text the way the JSP prints it. */
    public record QuoteRow(String symbol, String symbolLink, String companyName, String volume, String range,
                           String open, String price, String gainHtml, String gainPercentHtml) {
        static QuoteRow of(QuoteDataBean quote) {
            return new QuoteRow(
                    quote.getSymbol(),
                    FinancialUtils.printQuoteLink(quote.getSymbol()),
                    String.valueOf(quote.getCompanyName()),
                    String.valueOf(quote.getVolume()),
                    quote.getLow() + " - " + quote.getHigh(),
                    String.valueOf(quote.getOpen()),
                    String.valueOf(quote.getPrice()),
                    FinancialUtils.printGainHTML(new BigDecimal(quote.getChange())),
                    FinancialUtils.printGainPercentHTML(
                            FinancialUtils.computeGainPercent(quote.getPrice(), quote.getOpen())));
        }
    }
}
