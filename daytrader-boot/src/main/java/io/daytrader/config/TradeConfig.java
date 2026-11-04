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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "daytrader")
public class TradeConfig {
    public static final int EJB3 = 0;
    public static final int SYNCH = 0;
    public static final int ASYNCH = 1;
    public static final int ASYNCH_2PHASE = 2;
    public static final BigDecimal PENNY_STOCK_PRICE = new BigDecimal("0.01");
    public static final BigDecimal PENNY_STOCK_RECOVERY_MIRACLE_MULTIPLIER = new BigDecimal("100.00");
    private static final BigDecimal ORDER_FEE = new BigDecimal("24.95");
    private static final BigDecimal CASH_FEE = new BigDecimal("0.00");
    private static final Random RANDOM = new Random();
    private static volatile int maxUsers = 15000;
    private static volatile int maxQuotes = 10000;
    private static volatile int maxHoldings = 10;
    private static volatile boolean updateQuotePrices = true;
    private static volatile boolean publishQuotePriceChange;
    private static volatile int marketSummaryInterval = 20;
    private static volatile int primIterations = 1;
    private static volatile boolean trace;
    private static List<Integer> userDeck;
    private static int userDeckPosition;

    private String runtimeMode = "EJB3";
    private String orderProcessingMode = "SYNCH";
    private int maxUsersProperty = 15000;
    private int maxQuotesProperty = 10000;
    private int maxHoldingsProperty = 10;
    private int keyBlockSize = 1000;

    public String getRuntimeMode() { return runtimeMode; }
    public void setRuntimeMode(String runtimeMode) { this.runtimeMode = runtimeMode; }
    public String getOrderProcessingMode() { return orderProcessingMode; }
    public void setOrderProcessingMode(String orderProcessingMode) { this.orderProcessingMode = orderProcessingMode; }
    public int getMaxUsers() { return maxUsersProperty; }
    public void setMaxUsers(int value) { maxUsersProperty = value; maxUsers = value; }
    public int getMaxQuotes() { return maxQuotesProperty; }
    public void setMaxQuotes(int value) { maxQuotesProperty = value; maxQuotes = value; }
    public int getMaxHoldings() { return maxHoldingsProperty; }
    public void setMaxHoldings(int value) { maxHoldingsProperty = value; maxHoldings = value; }
    public int getKeyBlockSize() { return keyBlockSize; }
    public void setKeyBlockSize(int keyBlockSize) { this.keyBlockSize = keyBlockSize; }
    public boolean isUpdateQuotePrices() { return updateQuotePrices; }
    public void setUpdateQuotePrices(boolean updateQuotePrices) {
        TradeConfig.updateQuotePrices = updateQuotePrices;
    }
    public boolean isPublishQuotePriceChange() { return publishQuotePriceChange; }
    public void setPublishQuotePriceChange(boolean publishQuotePriceChange) {
        TradeConfig.publishQuotePriceChange = publishQuotePriceChange;
    }
    public int getMarketSummaryInterval() { return marketSummaryInterval; }
    public void setMarketSummaryInterval(int marketSummaryInterval) {
        TradeConfig.marketSummaryInterval = marketSummaryInterval;
    }
    public int getPrimIterations() { return primIterations; }
    public void setPrimIterations(int primIterations) {
        TradeConfig.primIterations = primIterations;
    }

    public static int getMAX_USERS() { return maxUsers; }
    public static void setMAX_USERS(int value) { maxUsers = value; userDeck = null; }
    public static int getMAX_QUOTES() { return maxQuotes; }
    public static void setMAX_QUOTES(int value) { maxQuotes = value; }
    public static int getMAX_HOLDINGS() { return maxHoldings; }
    public static void setMAX_HOLDINGS(int value) { maxHoldings = value; }
    public static boolean getUpdateQuotePrices() { return updateQuotePrices; }
    public static boolean getPublishQuotePriceChange() { return publishQuotePriceChange; }
    public static boolean getTrace() { return trace; }
    public static void setTrace(boolean enabled) { trace = enabled; }
    public static BigDecimal getOrderFee(String orderType) {
        return "buy".equalsIgnoreCase(orderType) || "sell".equalsIgnoreCase(orderType) ? ORDER_FEE : CASH_FEE;
    }
    public static BigDecimal getCashFee() { return CASH_FEE; }
    public static double random() { return RANDOM.nextDouble(); }
    public static int rndInt(int bound) { return bound <= 0 ? 0 : RANDOM.nextInt(bound); }
    public static float rndFloat(int bound) { return (float) (random() * bound); }
    public static BigDecimal rndBigDecimal(float bound) {
        return BigDecimal.valueOf(random() * bound).setScale(2, java.math.RoundingMode.HALF_UP);
    }
    public static float rndPrice() { return rndInt(200) + 1.0f; }
    public static float rndQuantity() { return rndInt(200) + 1.0f; }
    public static String rndSymbol() { return "s:" + rndInt(Math.max(1, maxQuotes - 1)); }
    public static String rndAddress() { return rndInt(1000) + " Main Street"; }
    public static String rndBalance() { return rndBigDecimal(1000000).toString(); }
    public static String rndCreditCard() { return "411111111111" + String.format("%04d", rndInt(10000)); }
    public static String rndEmail(String userID) { return userID + "@" + rndInt(100) + ".com"; }
    public static String rndFullName() { return "first:" + rndInt(1000) + " last:" + rndInt(5000); }
    public static boolean rndBoolean() { return RANDOM.nextBoolean(); }
    public static BigDecimal getRandomPriceChangeFactor() {
        double factor = 1 + (random() < .5 ? -1 : 1) * rndFloat(1) * .2;
        return BigDecimal.valueOf(factor).setScale(2, java.math.RoundingMode.HALF_UP);
    }
    public static String rndSymbols() {
        int count = rndInt(5);
        StringBuilder symbols = new StringBuilder();
        for (int i = 0; i <= count; i++) {
            if (!symbols.isEmpty()) symbols.append(',');
            symbols.append(rndSymbol());
        }
        return symbols.toString();
    }
    public static synchronized String rndUserID() {
        if (userDeck == null || userDeck.size() != maxUsers) {
            userDeck = new ArrayList<>();
            for (int i = 0; i < maxUsers; i++) userDeck.add(i);
            Collections.shuffle(userDeck, RANDOM);
            userDeckPosition = 0;
        }
        if (userDeckPosition >= maxUsers) userDeckPosition = 0;
        return "uid:" + userDeck.get(userDeckPosition++);
    }
}
