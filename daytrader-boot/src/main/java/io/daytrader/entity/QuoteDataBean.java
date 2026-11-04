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
package io.daytrader.entity;

import io.daytrader.config.TradeConfig;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity(name = "quoteejb")
@Table(name = "quoteejb")
public class QuoteDataBean implements Serializable {
    @Id
    @Column(name = "SYMBOL", nullable = false)
    private String symbol;
    @Column(name = "COMPANYNAME")
    private String companyName;
    @Column(name = "VOLUME", nullable = false)
    private double volume;
    @Column(name = "PRICE")
    private BigDecimal price;
    @Column(name = "OPEN1")
    private BigDecimal open1;
    @Column(name = "LOW")
    private BigDecimal low;
    @Column(name = "HIGH")
    private BigDecimal high;
    @Column(name = "CHANGE1", nullable = false)
    private double change1;

    public QuoteDataBean() { }
    public QuoteDataBean(String symbol) { this.symbol = symbol; }
    public QuoteDataBean(String symbol, String companyName, double volume, BigDecimal price,
                         BigDecimal open, BigDecimal low, BigDecimal high, double change) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.volume = volume;
        this.price = price;
        this.open1 = open;
        this.low = low;
        this.high = high;
        this.change1 = change;
    }
    public static QuoteDataBean getRandomInstance() {
        String symbol = TradeConfig.rndSymbol();
        return new QuoteDataBean(symbol, symbol + " Incorporated", TradeConfig.rndFloat(100000),
                TradeConfig.rndBigDecimal(1000.0f), TradeConfig.rndBigDecimal(1000.0f),
                TradeConfig.rndBigDecimal(1000.0f), TradeConfig.rndBigDecimal(1000.0f),
                TradeConfig.rndFloat(100000));
    }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public double getVolume() { return volume; }
    public void setVolume(double volume) { this.volume = volume; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOpen() { return open1; }
    public void setOpen(BigDecimal open) { open1 = open; }
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }
    public double getChange() { return change1; }
    public void setChange(double change) { change1 = change; }
    @Override public int hashCode() { return Objects.hashCode(symbol); }
    @Override public boolean equals(Object object) {
        return object instanceof QuoteDataBean other && Objects.equals(symbol, other.symbol);
    }
    public String toHTML() {
        return "<BR>Quote Data for: " + symbol + "<LI> companyName: " + companyName + "</LI><LI>      volume: "
                + volume + "</LI><LI>       price: " + price + "</LI><LI>        open1: " + open1
                + "</LI><LI>         low: " + low + "</LI><LI>        high: " + high
                + "</LI><LI>      change1: " + change1 + "</LI>";
    }
    @Override public String toString() {
        return "\n\tQuote Data for: " + symbol + "\n\t\t companyName: " + companyName
                + "\n\t\t      volume: " + volume + "\n\t\t       price: " + price
                + "\n\t\t        open1: " + open1 + "\n\t\t         low: " + low
                + "\n\t\t        high: " + high + "\n\t\t      change1: " + change1;
    }
}
