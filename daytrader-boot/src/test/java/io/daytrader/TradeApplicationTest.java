package io.daytrader;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import io.daytrader.entity.AccountDataBean;
import io.daytrader.entity.OrderDataBean;
import io.daytrader.entity.QuoteDataBean;
import io.daytrader.repository.OrderRepository;
import io.daytrader.util.FinancialUtils;
import io.daytrader.web.ActionHandler;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TradeApplicationTest.HandlerConfiguration.class)
class TradeApplicationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderRepository orderRepository;

    @Test
    void applicationContextLoads() { }

    @Test
    void nullActionRendersWelcome() throws Exception {
        mockMvc.perform(get("/app"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DayTrader Login")));
    }

    @Test
    void loginRequiredActionRendersLegacyMessage() throws Exception {
        mockMvc.perform(get("/app").param("action", "protected"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("User Not Logged in")));
    }

    @Test
    void queryUidCannotBypassSessionAuthentication() throws Exception {
        mockMvc.perform(get("/app").param("action", "home").param("uid", "uid:0"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("User Not Logged in")));
    }

    @Test
    void unknownActionRendersLegacyMessageForAuthenticatedSession() throws Exception {
        mockMvc.perform(get("/app").sessionAttr("uidBean", "uid:0").param("action", "missing"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TradeAppServlet: Invalid Actionmissing")));
    }

    @Test
    void handlerFailureUsesErrorPageAndStatus500() throws Exception {
        mockMvc.perform(get("/app").sessionAttr("uidBean", "uid:0").param("action", "explode"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DayTrader Error")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "An Error has occured during DayTrader processing")));
    }

    @Test
    void closedOrdersAreRenderedAsCompletedAndConsumed() throws Exception {
        QuoteDataBean quote = new QuoteDataBean("s:1000");
        OrderDataBean order = new OrderDataBean();
        order.setOrderID(401);
        order.setOrderType("buy");
        order.setOrderStatus("closed");
        order.setQuote(quote);
        when(orderRepository.findByAccount_Profile_UserIDAndOrderStatusOrderByOpenDateDesc("uid:0", "closed"))
                .thenAnswer(invocation -> "closed".equals(order.getOrderStatus())
                        ? new ArrayList<>(List.of(order)) : List.of());

        mockMvc.perform(get("/app").sessionAttr("uidBean", "uid:0").param("action", "alert-test"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("completed")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("401")));
        mockMvc.perform(get("/app").sessionAttr("uidBean", "uid:0").param("action", "alert-test"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("The following Order(s) have completed."))));
    }

    @Test
    void actuatorHealthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @TestConfiguration
    static class HandlerConfiguration {
        @Bean
        ActionHandler protectedAction() {
            return new ActionHandler() {
                @Override public Set<String> actions() { return Set.of("protected"); }
                @Override public String handle(io.daytrader.web.ActionRequest request, Model model) { return "welcome"; }
            };
        }

        @Bean
        ActionHandler exceptionAction() {
            return new ActionHandler() {
                @Override public Set<String> actions() { return Set.of("explode"); }
                @Override public boolean requiresLogin() { return false; }
                @Override public String handle(io.daytrader.web.ActionRequest request, Model model) {
                    throw new IllegalStateException("handler exploded");
                }
            };
        }

        @Bean
        ActionHandler closedOrdersAction() {
            return new ActionHandler() {
                @Override public Set<String> actions() { return Set.of("alert-test"); }
                @Override public String handle(io.daytrader.web.ActionRequest request, Model model) {
                    AccountDataBean account = new AccountDataBean(1, 0, 0, new Date(), new Date(),
                            new BigDecimal("100.00"), new BigDecimal("100.00"), "uid:0");
                    model.addAttribute("accountData", account);
                    model.addAttribute("accountCreatedDate",
                            new Date(account.getCreationDate().getTime()).toString());
                    model.addAttribute("holdingDataBeans", List.of());
                    model.addAttribute("marketSummaryData",
                            new io.daytrader.MarketSummaryDataBean(new BigDecimal("0.00"),
                                    new BigDecimal("0.00"), 0, List.of(), List.of()));
                    model.addAttribute("marketSummaryDate",
                            new java.sql.Date(System.currentTimeMillis()).toString());
                    model.addAttribute("marketGainPercentHtml",
                            FinancialUtils.printGainPercentHTML(FinancialUtils.ZERO).substring(1));
                    model.addAttribute("quoteGainHtml", java.util.Map.of());
                    model.addAttribute("holdingsTotal", FinancialUtils.ZERO);
                    model.addAttribute("sumOfCashHoldings", account.getBalance());
                    model.addAttribute("gainHtml", FinancialUtils.printGainHTML(FinancialUtils.ZERO));
                    model.addAttribute("gainPercentHtml", FinancialUtils.printGainPercentHTML(FinancialUtils.ZERO));
                    model.addAttribute("pageDate", new Date());
                    model.addAttribute("sessionCreationDate", new Date().toString());
                    model.addAttribute("closedOrders", request.http().getAttribute("closedOrders"));
                    return "tradehome";
                }
            };
        }
    }
}
