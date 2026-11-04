package io.daytrader.web.account;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.daytrader.config.TradeConfig;
import io.daytrader.entity.AccountDataBean;
import io.daytrader.entity.AccountProfileDataBean;
import io.daytrader.service.TradeService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class AccountHandlersTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TradeService tradeService;

    private String uid;

    @BeforeEach
    void buildDatabase() throws Exception {
        mockMvc.perform(get("/config").param("action", "buildDBTables"))
                .andExpect(status().isOk());
        uid = "account-" + UUID.randomUUID();
    }

    @Test
    void registrationLogsInAndReloadsTheOpeningBalance() throws Exception {
        MvcResult result = register(uid, "pw", "pw", "Registered User", "100000")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Welcome " + uid + ",")))
                .andExpect(content().string(containsString("$ 100000.00")))
                .andExpect(content().string(not(containsString("Registration operation succeeded"))))
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        assertNotNull(session);
        assertEquals(uid, session.getAttribute("uidBean"));
        assertEquals(1, tradeService.getAccountData(uid).getLoginCount());
    }

    @Test
    void passwordMismatchReturnsPrefilledRegistrationFormWithoutCreatingAccount() throws Exception {
        MvcResult result = mockMvc.perform(postRegistration(uid, "pw", "different", "Submitted User", "10000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registration operation failed, your passwords did not match")))
                .andExpect(content().string(containsString("DayTrader Registration")))
                .andExpect(content().string(containsString("value=\"Submitted User\"")))
                .andReturn();

        assertNull(result.getRequest().getSession(false));
        assertNull(tradeService.getAccountData(uid));
    }

    @Test
    void emptyPasswordsReturnRegistrationError() throws Exception {
        mockMvc.perform(postRegistration(uid, "", "", "Empty Password User", "10000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registration operation failed, your passwords did not match")))
                .andExpect(content().string(containsString("DayTrader Registration")));

        assertNull(tradeService.getAccountData(uid));
    }

    @Test
    void duplicateUserReturnsRegistrationFailure() throws Exception {
        register(uid, "pw", "pw", "First User", "10000")
                .andExpect(status().isOk());

        mockMvc.perform(postRegistration(uid, "pw", "pw", "Second User", "10000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registration operation failed;")));
    }

    @Test
    void invalidOpeningBalanceRendersErrorPage() throws Exception {
        mockMvc.perform(postRegistration(uid, "pw", "pw", "Invalid Balance User", "abc"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("DayTrader Error")));
    }

    @Test
    void accountPageShowsProfileAndKeepsLegacyZeroOrderCount() throws Exception {
        createUser("Account User", "account@example.com");
        MockHttpSession session = sessionFor(uid);

        mockMvc.perform(get("/app").session(session).param("action", "account"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Total Orders: </B>0")))
                .andExpect(content().string(containsString("DayTrader Account Information")))
                .andExpect(content().string(containsString("value=\"Account User\"")))
                .andExpect(content().string(containsString("value=\"account@example.com\"")))
                .andExpect(content().string(containsString("value=\"4111111111111111\"")))
                .andExpect(content().string(containsString("100000.00")));

        String symbol = "s:acct-" + UUID.randomUUID();
        tradeService.createQuote(symbol, "Account Test Quote", new BigDecimal("10.00"));
        tradeService.buy(uid, symbol, 10, TradeConfig.SYNCH);

        mockMvc.perform(get("/app").session(session).param("action", "account"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Total Orders: </B>0")));
    }

    @Test
    void updateProfileChangesProfileAndPassword() throws Exception {
        createUser("Original User", "original@example.com");

        mockMvc.perform(post("/app").session(sessionFor(uid))
                        .param("action", "update_profile")
                        .param("password", "new-password")
                        .param("cpassword", "new-password")
                        .param("fullname", "Updated User")
                        .param("address", "2 New Street")
                        .param("creditcard", "4111111111111111")
                        .param("email", "updated@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Account profile update successful")))
                .andExpect(content().string(containsString("value=\"Updated User\"")))
                .andExpect(content().string(containsString("value=\"updated@example.com\"")));

        AccountProfileDataBean profile = tradeService.getAccountProfileData(uid);
        assertEquals("Updated User", profile.getFullName());
        assertEquals("2 New Street", profile.getAddress());
        assertEquals("updated@example.com", profile.getEmail());
        assertNotNull(tradeService.login(uid, "new-password"));
    }

    @Test
    void mismatchedUpdatePasswordsLeaveProfileUnchanged() throws Exception {
        createUser("Original User", "original@example.com");

        mockMvc.perform(post("/app").session(sessionFor(uid))
                        .param("action", "update_profile")
                        .param("password", "new-password")
                        .param("cpassword", "different")
                        .param("fullname", "Changed User")
                        .param("address", "2 New Street")
                        .param("creditcard", "4111111111111111")
                        .param("email", "changed@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Update profile error: passwords do not match")));

        AccountProfileDataBean profile = tradeService.getAccountProfileData(uid);
        assertEquals("Original User", profile.getFullName());
        assertEquals("original@example.com", profile.getEmail());
        assertNotNull(tradeService.login(uid, "pw"));
    }

    @Test
    void emptyEmailReturnsRequiredFieldsErrorAndLeavesProfileUnchanged() throws Exception {
        createUser("Original User", "original@example.com");

        mockMvc.perform(post("/app").session(sessionFor(uid))
                        .param("action", "update_profile")
                        .param("password", "pw")
                        .param("cpassword", "pw")
                        .param("fullname", "Changed User")
                        .param("address", "2 New Street")
                        .param("creditcard", "4111111111111111")
                        .param("email", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Update profile error: please fill in all profile information fields")));

        AccountProfileDataBean profile = tradeService.getAccountProfileData(uid);
        assertEquals("Original User", profile.getFullName());
        assertEquals("original@example.com", profile.getEmail());
    }

    @Test
    void registrationPageUsesJspDefaults() throws Exception {
        mockMvc.perform(get("/register.jsp"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"10000\"")))
                .andExpect(content().string(containsString("value=\"123-fake-ccnum-456\"")));
    }

    private ResultActions register(String userID, String password, String confirmPassword,
                                   String fullName, String money) throws Exception {
        return mockMvc.perform(postRegistration(userID, password, confirmPassword, fullName, money));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postRegistration(
            String userID, String password, String confirmPassword, String fullName, String money) {
        return post("/app")
                .param("action", "register")
                .param("user id", userID)
                .param("passwd", password)
                .param("confirm passwd", confirmPassword)
                .param("Full Name", fullName)
                .param("Credit Card Number", "4111111111111111")
                .param("money", money)
                .param("email", "test@example.com")
                .param("snail mail", "1 Main Street");
    }

    private void createUser(String fullName, String email) {
        tradeService.register(uid, "pw", fullName, "1 Main Street", email,
                "4111111111111111", new BigDecimal("100000"));
    }

    private static MockHttpSession sessionFor(String userID) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("uidBean", userID);
        return session;
    }
}
