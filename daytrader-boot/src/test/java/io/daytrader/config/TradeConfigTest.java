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
package io.daytrader.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class TradeConfigTest {

    private int maxUsers;
    private int maxQuotes;

    @BeforeEach
    void saveStaticConfig() {
        maxUsers = TradeConfig.getMAX_USERS();
        maxQuotes = TradeConfig.getMAX_QUOTES();
    }

    @AfterEach
    void restoreStaticConfig() {
        TradeConfig.setMAX_USERS(maxUsers);
        TradeConfig.setMAX_QUOTES(maxQuotes);
    }

    @Test
    void defaultsMatchTheShippedConfiguration() {
        TradeConfig config = new TradeConfig();
        assertEquals("EJB3", config.getRuntimeMode());
        assertEquals("SYNCH", config.getOrderProcessingMode());
        assertEquals(15000, TradeConfig.getMAX_USERS());
        assertEquals(10000, TradeConfig.getMAX_QUOTES());
        assertEquals(15000, config.getMaxUsers());
        assertEquals(10000, config.getMaxQuotes());
        assertEquals(1000, config.getKeyBlockSize());
    }

    @Test
    void springPropertiesBindRuntimeAndOrderProcessingModes() {
        TradeConfig config = bind(Map.of(
                "daytrader.runtime-mode", "EJB3",
                "daytrader.order-processing-mode", "SYNCH"));
        assertEquals("EJB3", config.getRuntimeMode());
        assertEquals("SYNCH", config.getOrderProcessingMode());
    }

    @Test
    void numericLimitsAreParsedFromSpringProperties() {
        TradeConfig config = bind(Map.of("daytrader.max-users", " 200 ", "daytrader.max-quotes", "50"));
        assertEquals(200, TradeConfig.getMAX_USERS());
        assertEquals(50, TradeConfig.getMAX_QUOTES());
        assertEquals(200, config.getMaxUsers());
        assertEquals(50, config.getMaxQuotes());
    }

    @Test
    void absentSpringPropertiesLeaveSettingsUnchanged() {
        TradeConfig config = new TradeConfig();
        new Binder(new MapConfigurationPropertySource(Map.of("daytrader.unknown", "ignored")))
                .bind("daytrader", Bindable.ofInstance(config));
        assertEquals("EJB3", config.getRuntimeMode());
        assertEquals("SYNCH", config.getOrderProcessingMode());
        assertEquals(maxUsers, TradeConfig.getMAX_USERS());
        assertEquals(maxQuotes, TradeConfig.getMAX_QUOTES());
    }

    @Test
    void orderFeeAppliesToBuyAndSellOnly() {
        assertEquals(new BigDecimal("24.95"), TradeConfig.getOrderFee("buy"));
        assertEquals(new BigDecimal("24.95"), TradeConfig.getOrderFee("SELL"));
        assertEquals(0, BigDecimal.ZERO.compareTo(TradeConfig.getOrderFee("cash")));
    }

    @Test
    void userDeckWrapsAtMaxUsers() {
        TradeConfig.setMAX_USERS(3);
        boolean sawUid0 = false;
        for (int i = 0; i < 6; i++) {
            String userId = TradeConfig.rndUserID();
            assertTrue(userId.matches("uid:[0-2]"), userId);
            sawUid0 |= userId.equals("uid:0");
        }
        assertTrue(sawUid0);
    }

    @Test
    void randomSymbolsStayInsideTheQuotePopulation() {
        TradeConfig.setMAX_QUOTES(5);
        for (int i = 0; i < 200; i++) {
            int n = Integer.parseInt(TradeConfig.rndSymbol().substring(2));
            assertTrue(n >= 0 && n < 4, "s:" + n);
        }
    }

    @Test
    void priceChangeFactorStaysWithinTwentyPercent() {
        for (int i = 0; i < 500; i++) {
            BigDecimal factor = TradeConfig.getRandomPriceChangeFactor();
            assertTrue(factor.compareTo(new BigDecimal("0.80")) >= 0
                            && factor.compareTo(new BigDecimal("1.20")) <= 0,
                    factor.toPlainString());
            assertEquals(2, factor.scale());
        }
    }

    private TradeConfig bind(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(properties))
                .bind("daytrader", Bindable.of(TradeConfig.class)).get();
    }
}
