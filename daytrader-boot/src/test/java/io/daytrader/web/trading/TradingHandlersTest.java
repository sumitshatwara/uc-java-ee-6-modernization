package io.daytrader.web.trading;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.daytrader.config.TradeConfig;
import io.daytrader.entity.HoldingDataBean;
import io.daytrader.service.TradeService;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TradingHandlersTest {
    private static final Pattern LEGACY_DATE =
            Pattern.compile("\\w{3} \\w{3} \\d{2} \\d{2}:\\d{2}:\\d{2} \\w+ \\d{4}");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TradeService tradeService;

    private String uid;
    private String symbol;
    private MockHttpSession session;

    @BeforeEach
    void createTradingUserAndQuote() throws Exception {
        mockMvc.perform(get("/config").param("action", "buildDBTables"))
                .andExpect(status().isOk());
        uid = "trading-" + UUID.randomUUID();
        symbol = "s:trading-" + UUID.randomUUID();
        tradeService.register(uid, "pw", "Trading User", "1 Main Street", "trading@example.com",
                "4111111111111111", new BigDecimal("100000"));
        tradeService.createQuote(symbol, "Trading Test Quote", new BigDecimal("10.00"));
        session = new MockHttpSession();
        session.setAttribute("uidBean", uid);
    }

    @Test
    void buyPortfolioAndSellPreserveOrderAndBalanceBehavior() throws Exception {
        MvcResult buyResult = mockMvc.perform(get("/app").session(session).param("action", "buy")
                        .param("symbol", symbol).param("quantity", "10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DayTrader New Orders")))
                .andExpect(content().string(containsString("has been submitted for processing")))
                .andExpect(content().string(containsString("closed")))
                .andExpect(content().string(containsString("24.95")))
                .andExpect(content().string(containsString("buy 10.0")))
                .andReturn();
        assertLegacyDates(buyResult.getResponse().getContentAsString());
        assertEquals(new BigDecimal("99875.05"), tradeService.getAccountData(uid).getBalance());

        mockMvc.perform(get("/app").session(session).param("action", "portfolio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Number of Holdings: </b><span>1</span>")))
                .andExpect(content().string(containsString("app?action=sell&amp;holdingID=")))
                .andExpect(content().string(containsString("purchase price")));
        Collection<HoldingDataBean> holdings = tradeService.getHoldings(uid);
        assertEquals(1, holdings.size());
        HoldingDataBean holding = holdings.iterator().next();

        MvcResult sellResult = mockMvc.perform(get("/app").session(session).param("action", "sell")
                        .param("holdingID", holding.getHoldingID().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("sell 10.0")))
                .andReturn();
        assertLegacyDates(sellResult.getResponse().getContentAsString());
        assertEquals(new BigDecimal("99950.10"), tradeService.getAccountData(uid).getBalance());

        mockMvc.perform(get("/app").session(session).param("action", "portfolio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">$ 0</td>")));
    }

    @Test
    void portfolioNoEdgeIsAnAlias() throws Exception {
        mockMvc.perform(get("/app").session(session).param("action", "portfolioNoEdge"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DayTrader Portfolio")));
    }

    @Test
    void missingHoldingRendersCancelledOrderWithNullValues() throws Exception {
        mockMvc.perform(get("/app").session(session).param("action", "sell").param("holdingID", "99999999"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("cancelled")))
                .andExpect(content().string(containsString(">null</td>")));
    }

    @Test
    void invalidQuantityIncludesHomeAndOrderPage() throws Exception {
        mockMvc.perform(get("/app").session(session).param("action", "buy")
                        .param("symbol", symbol).param("quantity", "abc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DayTrader Home")))
                .andExpect(content().string(containsString("DayTrader New Orders")));
    }

    @Test
    void unknownSymbolAndInvalidHoldingIdRenderErrorPage() throws Exception {
        mockMvc.perform(get("/app").session(session).param("action", "buy")
                        .param("symbol", "unknown-" + UUID.randomUUID()).param("quantity", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("DayTrader Error")));

        mockMvc.perform(get("/app").session(session).param("action", "sell").param("holdingID", "x"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("DayTrader Error")));
    }

    @Test
    void portfolioDatesUseLegacyDateToStringFormat() throws Exception {
        tradeService.buy(uid, symbol, 1, TradeConfig.SYNCH);
        MvcResult result = mockMvc.perform(get("/app").session(session).param("action", "portfolio"))
                .andExpect(status().isOk())
                .andReturn();
        assertLegacyDates(result.getResponse().getContentAsString());
    }

    private static void assertLegacyDates(String html) {
        assertTrue(LEGACY_DATE.matcher(html).find(), "Expected a Date.toString timestamp in rendered markup");
        assertTrue(!html.matches("(?s).*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*"),
                "Rendered timestamp should not use SQL Timestamp.toString format");
    }
}
