package io.daytrader.web.market;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import io.daytrader.MarketSummaryDataBean;
import io.daytrader.entity.AccountDataBean;
import io.daytrader.entity.OrderDataBean;
import io.daytrader.entity.QuoteDataBean;
import io.daytrader.service.TradeService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MarketHandlersTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradeService tradeService;

    @BeforeEach
    void quotes() {
        when(tradeService.getQuote("s:1")).thenReturn(new QuoteDataBean("s:1", "S1 Incorporated", 1250.0,
                new BigDecimal("31.50"), new BigDecimal("30.00"), new BigDecimal("29.75"), new BigDecimal("32.00"),
                1.5));
        when(tradeService.getQuote("s:2")).thenReturn(new QuoteDataBean("s:2", "S2 Incorporated", 0.0,
                new BigDecimal("18.00"), new BigDecimal("20.00"), new BigDecimal("18.00"), new BigDecimal("20.00"),
                -2.0));
        when(tradeService.getClosedOrders(anyString())).thenReturn(new ArrayList<>());
    }

    @Test
    void quotesRendersOneRowPerSymbolWithBuyForm() throws Exception {
        mockMvc.perform(get("/app").sessionAttr("uidBean", "uid:0")
                        .param("action", "quotes").param("symbols", "s:1, s:2"))
                .andExpect(status().isOk())
                .andExpect(view().name("quote"))
                .andExpect(content().string(containsString("<title>DayTrader: Quotes and Trading</title>")))
                .andExpect(content().string(containsString("<A href=\"app?action=quotes&symbols=s:1\">s:1</A>")))
                .andExpect(content().string(containsString("<td>S1 Incorporated</td>")))
                .andExpect(content().string(containsString("<td>1250.0</td>")))
                .andExpect(content().string(containsString("<td>29.75 - 32.00</td>")))
                .andExpect(content().string(containsString("<td nowrap>30.00</td>")))
                .andExpect(content().string(containsString("<td>$ 31.50</td>")))
                .andExpect(content().string(containsString("<FONT color=\"#009900\">1.50</FONT>")))
                .andExpect(content().string(containsString("<FONT color=\"#009900\">+5.00%</FONT>")))
                .andExpect(content().string(containsString("<FONT color=\"#ff0000\">-2.00</FONT>")))
                .andExpect(content().string(containsString("<FONT color=\"#ff0000\">-10.00%</FONT>")))
                .andExpect(content().string(containsString("<input type=\"submit\" name=\"action\" value=\"buy\">"
                        + "<input\n                    type=\"hidden\" name=\"symbol\" value=\"s:2\"><input\n"
                        + "                    size=\"4\" type=\"text\" name=\"quantity\" value=\"100\">")))
                .andExpect(content().string(containsString(
                        "<input size=\"20\" type=\"text\" name=\"symbols\" value=\"s:0, s:1, s:2, s:3, s:4\">")))
                .andExpect(content().string(containsString("for a quote or to trade.")));
        verify(tradeService).getQuote("s:1");
        verify(tradeService).getQuote("s:2");
    }

    @Test
    void quotesSplitsOnCommasAndSpacesAndSkipsUnknownSymbols() throws Exception {
        mockMvc.perform(get("/app").sessionAttr("uidBean", "uid:0")
                        .param("action", "quotes").param("symbols", "s:2,,nope s:1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("quoteRows", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(content().string(org.hamcrest.Matchers.stringContainsInOrder(List.of(
                        "symbols=s:2\">s:2</A>", "symbols=s:1\">s:1</A>"))))
                .andExpect(content().string(not(containsString("nope"))));
    }

    @Test
    void quotesWithoutSymbolsRendersEmptyTable() throws Exception {
        mockMvc.perform(get("/app").sessionAttr("uidBean", "uid:0").param("action", "quotes"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("quoteRows", org.hamcrest.Matchers.empty()))
                .andExpect(content().string(containsString("<b>Quotes</b>")));
        verify(tradeService).getQuote(MarketHandlers.NO_SYMBOL);
    }

    @Test
    void quotesShowsClosedOrderAlert() throws Exception {
        OrderDataBean order = new OrderDataBean();
        order.setOrderID(4242);
        order.setOrderStatus("closed");
        order.setOrderType("buy");
        order.setSymbol("s:1");
        when(tradeService.getClosedOrders("uid:0")).thenReturn(new ArrayList<>(List.of(order)));
        mockMvc.perform(get("/app").sessionAttr("uidBean", "uid:0")
                        .param("action", "quotes").param("symbols", "s:1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Alert: The following Order(s) have completed.")))
                .andExpect(content().string(containsString("<td>4242</td>")));
    }

    @Test
    void quotesRequiresLogin() throws Exception {
        mockMvc.perform(get("/app").param("action", "quotes").param("symbols", "s:1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("User Not Logged in")));
    }

    @Test
    void homeRendersTradeHome() throws Exception {
        when(tradeService.getAccountData("uid:0")).thenReturn(new AccountDataBean(7, 3, 1, new Date(), new Date(),
                new BigDecimal("1000.00"), new BigDecimal("1000.00"), "uid:0"));
        when(tradeService.getHoldings("uid:0")).thenReturn(List.of());
        when(tradeService.getMarketSummary()).thenReturn(new MarketSummaryDataBean(new BigDecimal("100.00"),
                new BigDecimal("100.00"), 0, List.of(), List.of()));
        mockMvc.perform(get("/app").sessionAttr("uidBean", "uid:0").param("action", "home"))
                .andExpect(status().isOk())
                .andExpect(view().name("tradehome"))
                .andExpect(model().attribute("results", ""))
                .andExpect(content().string(containsString("Welcome uid:0,")))
                .andExpect(content().string(containsString("DayTrader Stock Index (TSIA)")));
    }
}
