package io.daytrader;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import io.daytrader.web.ActionHandler;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TradeApplicationTest.HandlerConfiguration.class)
class TradeApplicationTest {
    @Autowired
    private MockMvc mockMvc;

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
    void unknownActionRendersLegacyMessageForAuthenticatedSession() throws Exception {
        mockMvc.perform(get("/app").sessionAttr("uidBean", "uid:0").param("action", "missing"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TradeAppServlet: Invalid Actionmissing")));
    }

    @Test
    void handlerFailureUsesErrorPageAndStatus500() throws Exception {
        mockMvc.perform(get("/app").param("action", "explode"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DayTrader Error")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "An Error has occured during DayTrader processing")));
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
    }
}
