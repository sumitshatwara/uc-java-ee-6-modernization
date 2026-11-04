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
package io.daytrader.web.trading;

import io.daytrader.config.TradeConfig;
import io.daytrader.entity.HoldingDataBean;
import io.daytrader.entity.OrderDataBean;
import io.daytrader.entity.QuoteDataBean;
import io.daytrader.service.TradeService;
import io.daytrader.util.FinancialUtils;
import io.daytrader.util.Log;
import io.daytrader.web.ActionHandler;
import io.daytrader.web.ActionRequest;
import io.daytrader.web.PageRenderer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class TradingHandlers implements ActionHandler {
    private final TradeService tradeService;
    private final PageRenderer pages;
    private final TradeConfig tradeConfig;

    public TradingHandlers(TradeService tradeService, PageRenderer pages, TradeConfig tradeConfig) {
        this.tradeService = tradeService;
        this.pages = pages;
        this.tradeConfig = tradeConfig;
    }

    @Override
    public Set<String> actions() {
        return Set.of("buy", "sell", "portfolio", "portfolioNoEdge");
    }

    @Override
    public String handle(ActionRequest request, Model model) {
        return switch (request.action()) {
            case "buy" -> buy(request, model);
            case "sell" -> sell(request, model);
            case "portfolio", "portfolioNoEdge" -> portfolio(request, model);
            default -> throw new IllegalArgumentException("Unsupported trading action: " + request.action());
        };
    }

    private String buy(ActionRequest request, Model model) {
        String symbol = request.param("symbol");
        String quantity = request.param("quantity");
        double shares;
        try {
            shares = Double.parseDouble(quantity);
        } catch (NumberFormatException exception) {
            Log.error("TradeServletAction.doBuy(...) illegal argument. userID = " + request.userID()
                    + ", symbol = " + symbol, exception);
            pages.home(request, model, "illegal argument:");
            addOrderPageAttributes(request, model);
            return "buyIllegalArgument";
        }

        OrderDataBean order = tradeService.buy(request.userID(), symbol, shares, orderProcessingMode());
        model.addAttribute("orderRow", OrderRow.of(order));
        addOrderPageAttributes(request, model);
        return "order";
    }

    private String sell(ActionRequest request, Model model) {
        int holdingID = Integer.parseInt(request.param("holdingID"));
        OrderDataBean order = tradeService.sell(request.userID(), holdingID, orderProcessingMode());
        model.addAttribute("orderRow", OrderRow.of(order));
        addOrderPageAttributes(request, model);
        return "order";
    }

    private String portfolio(ActionRequest request, Model model) {
        Collection<HoldingDataBean> holdings = tradeService.getHoldings(request.userID());
        List<PortfolioRow> rows = new ArrayList<>();
        BigDecimal totalGain = new BigDecimal(0.0);
        BigDecimal totalBasis = new BigDecimal(0.0);
        BigDecimal totalValue = new BigDecimal(0.0);
        try {
            for (HoldingDataBean holding : holdings) {
                QuoteDataBean quote = tradeService.getQuote(holding.getQuoteID());
                BigDecimal basis = holding.getPurchasePrice().multiply(new BigDecimal(holding.getQuantity()));
                BigDecimal marketValue = quote.getPrice().multiply(new BigDecimal(holding.getQuantity()));
                totalBasis = totalBasis.add(basis);
                totalValue = totalValue.add(marketValue);
                BigDecimal gain = marketValue.subtract(basis);
                totalGain = totalGain.add(gain);
                if (basis.doubleValue() == 0.0) {
                    Log.error("portfolio.jsp: Holding with zero basis. holdingID=" + holding.getHoldingID()
                            + " symbol=" + holding.getQuoteID() + " purchasePrice=" + holding.getPurchasePrice());
                }
                rows.add(PortfolioRow.of(holding, quote, basis, marketValue, gain));
            }
        } catch (Exception exception) {
            Log.error("portfolio.jsp: error displaying user holdings", exception);
        }

        Collection<?> closedOrders = request.http().getAttribute("closedOrders") instanceof Collection<?> orders
                ? orders : List.of();
        model.addAttribute("pageDate", new Date());
        model.addAttribute("closedOrderRows", orderRows(closedOrders));
        model.addAttribute("portfolioRows", rows);
        model.addAttribute("holdingCount", holdings.size());
        model.addAttribute("totalBasis", holdings.isEmpty() ? BigDecimal.ZERO : totalBasis);
        model.addAttribute("totalValue", holdings.isEmpty() ? BigDecimal.ZERO : totalValue);
        model.addAttribute("totalGainSummary",
                "$ " + FinancialUtils.printGainHTML(totalGain) + " "
                        + FinancialUtils.printGainPercentHTML(FinancialUtils.computeGainPercent(totalValue, totalBasis)));
        return "portfolio";
    }

    private int orderProcessingMode() {
        String mode = tradeConfig.getOrderProcessingMode();
        if (mode == null) return TradeConfig.SYNCH;
        return switch (mode.trim().toUpperCase()) {
            case "ASYNCH", "1" -> TradeConfig.ASYNCH;
            case "ASYNCH_2PHASE", "2" -> TradeConfig.ASYNCH_2PHASE;
            case "SYNCH", "0" -> TradeConfig.SYNCH;
            default -> TradeConfig.SYNCH;
        };
    }

    private void addOrderPageAttributes(ActionRequest request, Model model) {
        Collection<?> closedOrders = request.http().getAttribute("closedOrders") instanceof Collection<?> orders
                ? orders : List.of();
        model.addAttribute("pageDate", new Date());
        model.addAttribute("closedOrderRows", orderRows(closedOrders));
    }

    private static List<OrderRow> orderRows(Collection<?> orders) {
        List<OrderRow> rows = new ArrayList<>();
        for (Object value : orders) {
            rows.add(OrderRow.of((OrderDataBean) value));
        }
        return rows;
    }

    private static String legacyDate(Date date) {
        return date == null ? "null" : new Date(date.getTime()).toString();
    }

    public record OrderRow(String orderID, String orderStatus, String openDate, String completionDate,
                           String orderFee, String orderType, String symbolLink, String symbol, String quantity) {
        static OrderRow of(OrderDataBean order) {
            String symbol = String.valueOf(order.getSymbol());
            return new OrderRow(String.valueOf(order.getOrderID()), String.valueOf(order.getOrderStatus()),
                    legacyDate(order.getOpenDate()), legacyDate(order.getCompletionDate()),
                    String.valueOf(order.getOrderFee()), String.valueOf(order.getOrderType()),
                    FinancialUtils.printQuoteLink(order.getSymbol()), symbol,
                    String.valueOf(order.getQuantity()));
        }
    }

    public record PortfolioRow(String holdingID, String purchaseDate, String symbolLink, String quantity,
                               String purchasePrice, String currentPrice, String basis, String marketValue,
                               String gainHtml, String sellLink) {
        static PortfolioRow of(HoldingDataBean holding, QuoteDataBean quote, BigDecimal basis,
                               BigDecimal marketValue, BigDecimal gain) {
            Integer holdingID = holding.getHoldingID();
            return new PortfolioRow(String.valueOf(holdingID), legacyDate(holding.getPurchaseDate()),
                    FinancialUtils.printQuoteLink(holding.getQuoteID()), String.valueOf(holding.getQuantity()),
                    String.valueOf(holding.getPurchasePrice()), String.valueOf(quote.getPrice()),
                    basis.toString(), marketValue.toString(), FinancialUtils.printGainHTML(gain),
                    "<A href=\"app?action=sell&holdingID=" + holdingID + "\">sell</A>");
        }
    }
}
