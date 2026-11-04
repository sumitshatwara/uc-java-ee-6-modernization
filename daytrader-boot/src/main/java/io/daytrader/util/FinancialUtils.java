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
package io.daytrader.util;

import io.daytrader.entity.HoldingDataBean;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

public final class FinancialUtils {
    public static final int SCALE = 2;
    public static final RoundingMode ROUND = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);
    public static final BigDecimal ONE = BigDecimal.ONE.setScale(SCALE);
    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100).setScale(SCALE);

    private FinancialUtils() { }

    public static BigDecimal computeGain(BigDecimal currentBalance, BigDecimal openBalance) {
        return currentBalance.subtract(openBalance).setScale(SCALE, ROUND);
    }
    public static BigDecimal computeGainPercent(BigDecimal currentBalance, BigDecimal openBalance) {
        if (openBalance == null || openBalance.signum() == 0) return ZERO;
        return currentBalance.divide(openBalance, ROUND).subtract(ONE).multiply(HUNDRED);
    }
    public static BigDecimal computeHoldingsTotal(Collection<HoldingDataBean> holdings) {
        if (holdings == null) return ZERO;
        return holdings.stream()
                .map(holding -> holding.getPurchasePrice().multiply(BigDecimal.valueOf(holding.getQuantity())))
                .reduce(ZERO, BigDecimal::add).setScale(SCALE, ROUND);
    }
    public static String printGainHTML(BigDecimal gain) {
        String color = gain.signum() < 0 ? "#ff0000" : "#009900";
        String arrow = gain.signum() < 0 ? "arrowdown.gif" : "arrowup.gif";
        return "<FONT color=\"" + color + "\">" + gain.setScale(SCALE, ROUND)
                + "</FONT><IMG src=\"images/" + arrow + "\" width=\"10\" height=\"10\" border=\"0\"></IMG>";
    }
    public static String printChangeHTML(double change) {
        String color = change < 0 ? "#ff0000" : "#009900";
        String arrow = change < 0 ? "arrowdown.gif" : "arrowup.gif";
        return "<FONT color=\"" + color + "\">" + change
                + "</FONT><IMG src=\"images/" + arrow + "\" width=\"10\" height=\"10\" border=\"0\"></IMG>";
    }
    public static String printGainPercentHTML(BigDecimal gain) {
        String prefix = gain.signum() < 0 ? "(<B><FONT color=\"#ff0000\">" : "(<B><FONT color=\"#009900\">+";
        String arrow = gain.signum() < 0 ? "arrowdown.gif" : "arrowup.gif";
        return prefix + gain.setScale(SCALE, ROUND) + "%</FONT></B>)<IMG src=\"images/" + arrow
                + "\" width=\"10\" height=\"10\" border=\"0\"></IMG>";
    }
    public static String printQuoteLink(String symbol) {
        return "<A href=\"app?action=quotes&symbols=" + symbol + "\">" + symbol + "</A>";
    }
}
