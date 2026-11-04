package io.daytrader.web.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.daytrader.entity.AccountDataBean;
import io.daytrader.service.TradeService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
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
class AuthActionHandlerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TradeService tradeService;

    private String uid;

    @BeforeEach
    void registerUser() throws Exception {
        mockMvc.perform(get("/config").param("action", "buildDBTables"))
                .andExpect(status().isOk());
        uid = "auth-" + UUID.randomUUID();
        tradeService.register(uid, "pw", "Auth User", "1 Main Street",
                uid + "@example.com", "4111111111111111", new BigDecimal("100000"));
    }

    @Test
    void loginSuccess() throws Exception {
        MvcResult result = mockMvc.perform(post("/app")
                        .param("action", "login")
                        .param("uid", uid)
                        .param("passwd", "pw"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Ready to Trade"))))
                .andExpect(content().string(containsString("Welcome " + uid)))
                .andExpect(content().string(containsString("User Statistics")))
                .andExpect(model().attribute("results", "Ready to Trade"))
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        assertNotNull(session);
        assertEquals(uid, session.getAttribute("uidBean"));
        assertInstanceOf(Date.class, session.getAttribute("sessionCreationDate"));
    }

    @Test
    void badPasswordReturnsServerErrorWithoutPasswordInExceptionMessage() throws Exception {
        mockMvc.perform(post("/app")
                        .param("action", "login")
                        .param("uid", uid)
                        .param("passwd", "wrong"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("DayTrader Error")))
                .andExpect(content().string(containsString("An Error has occured during DayTrader processing")))
                .andExpect(content().string(not(containsString("with password"))));
    }

    @Test
    void unknownUserReturnsServerError() throws Exception {
        mockMvc.perform(post("/app")
                        .param("action", "login")
                        .param("uid", uid + "-missing")
                        .param("passwd", "pw"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void logoutInvalidatesSessionAndReturnsToLoginPage() throws Exception {
        MvcResult login = mockMvc.perform(post("/app")
                        .param("action", "login")
                        .param("uid", uid)
                        .param("passwd", "pw"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        int logoutCount = tradeService.getAccountData(uid).getLogoutCount();

        mockMvc.perform(get("/app").session(session).param("action", "logout"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DayTrader Login")));

        assertTrue(session.isInvalid());
        AccountDataBean account = tradeService.getAccountData(uid);
        assertEquals(logoutCount + 1, account.getLogoutCount());

        mockMvc.perform(get("/app").param("action", "home"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("User Not Logged in")));
    }
}
