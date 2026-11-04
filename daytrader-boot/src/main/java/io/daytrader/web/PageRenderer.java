package io.daytrader.web;

import io.daytrader.MarketSummaryDataBean;
import io.daytrader.entity.AccountDataBean;
import io.daytrader.entity.HoldingDataBean;
import io.daytrader.service.TradeService;
import io.daytrader.util.FinancialUtils;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class PageRenderer {
    private final TradeService tradeService;

    public PageRenderer(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    public String welcome(Model model, String message) {
        model.addAttribute("message", message == null ? "" : message);
        return "welcome";
    }

    public String home(ActionRequest request, Model model, String results) {
        AccountDataBean account = tradeService.getAccountData(request.userID());
        Collection<HoldingDataBean> holdingDataBeans = tradeService.getHoldings(request.userID());
        MarketSummaryDataBean summary = tradeService.getMarketSummary();
        BigDecimal holdingsTotal = FinancialUtils.computeHoldingsTotal(holdingDataBeans);
        BigDecimal sumOfCashHoldings = account.getBalance().add(holdingsTotal);
        BigDecimal gain = FinancialUtils.computeGain(sumOfCashHoldings, account.getOpenBalance());
        BigDecimal gainPercent = FinancialUtils.computeGainPercent(sumOfCashHoldings, account.getOpenBalance());
        Date sessionCreated = (Date) request.session().getAttribute(SessionKeys.SESSION_CREATION_DATE);
        if (sessionCreated == null) {
            sessionCreated = new Date();
            request.session().setAttribute(SessionKeys.SESSION_CREATION_DATE, sessionCreated);
        }
        model.addAttribute("accountData", account);
        model.addAttribute("holdingDataBeans", holdingDataBeans);
        model.addAttribute("marketSummaryData", summary);
        model.addAttribute("holdingsTotal", holdingsTotal);
        model.addAttribute("sumOfCashHoldings", sumOfCashHoldings);
        model.addAttribute("gainHtml", FinancialUtils.printGainHTML(gain));
        model.addAttribute("gainPercentHtml", FinancialUtils.printGainPercentHTML(gainPercent));
        model.addAttribute("pageDate", new Date());
        model.addAttribute("closedOrders", request.http().getAttribute("closedOrders"));
        model.addAttribute("sessionCreationDate", sessionCreated);
        model.addAttribute("results", results == null ? "" : results);
        return "tradehome";
    }
}
