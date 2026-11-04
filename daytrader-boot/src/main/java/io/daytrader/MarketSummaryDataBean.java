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
package io.daytrader;

import io.daytrader.entity.QuoteDataBean;
import io.daytrader.config.TradeConfig;
import io.daytrader.util.FinancialUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class MarketSummaryDataBean {
    private BigDecimal TSIA;
    private BigDecimal openTSIA;
    private double volume;
    private Collection<QuoteDataBean> topGainers;
    private Collection<QuoteDataBean> topLosers;
    private Date summaryDate;

    public MarketSummaryDataBean() { }
    public MarketSummaryDataBean(BigDecimal tsia, BigDecimal openTSIA, double volume,
                                 Collection<QuoteDataBean> topGainers, Collection<QuoteDataBean> topLosers) {
        this.TSIA = tsia;
        this.openTSIA = openTSIA;
        this.volume = volume;
        this.topGainers = topGainers;
        this.topLosers = topLosers;
        this.summaryDate = new java.sql.Date(System.currentTimeMillis());
    }
    public static MarketSummaryDataBean getRandomInstance() {
        Collection<QuoteDataBean> gainers = new ArrayList<>();
        Collection<QuoteDataBean> losers = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            gainers.add(QuoteDataBean.getRandomInstance());
            losers.add(QuoteDataBean.getRandomInstance());
        }
        return new MarketSummaryDataBean(TradeConfig.rndBigDecimal(1000000.0f),
                TradeConfig.rndBigDecimal(1000000.0f), TradeConfig.rndQuantity(), gainers, losers);
    }
    public BigDecimal getTSIA() { return TSIA; }
    public void setTSIA(BigDecimal TSIA) { this.TSIA = TSIA; }
    public BigDecimal getOpenTSIA() { return openTSIA; }
    public void setOpenTSIA(BigDecimal openTSIA) { this.openTSIA = openTSIA; }
    public double getVolume() { return volume; }
    public void setVolume(double volume) { this.volume = volume; }
    public Collection<QuoteDataBean> getTopGainers() { return topGainers; }
    public void setTopGainers(Collection<QuoteDataBean> topGainers) { this.topGainers = topGainers; }
    public Collection<QuoteDataBean> getTopLosers() { return topLosers; }
    public void setTopLosers(Collection<QuoteDataBean> topLosers) { this.topLosers = topLosers; }
    public Date getSummaryDate() { return summaryDate; }
    public void setSummaryDate(Date summaryDate) { this.summaryDate = summaryDate; }
    public BigDecimal getGainPercent() { return FinancialUtils.computeGainPercent(TSIA, openTSIA); }
}
