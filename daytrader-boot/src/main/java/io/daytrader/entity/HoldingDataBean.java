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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

@Entity(name = "holdingejb")
@Table(name = "holdingejb")
public class HoldingDataBean implements Serializable {
    @TableGenerator(name = "holdingIdGen", table = "KEYGENEJB", pkColumnName = "KEYNAME",
            valueColumnName = "KEYVAL", pkColumnValue = "holding", allocationSize = 1000)
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "holdingIdGen")
    @Column(name = "HOLDINGID", nullable = false)
    private Integer holdingID;
    @Column(name = "QUANTITY", nullable = false)
    private double quantity;
    @Column(name = "PURCHASEPRICE")
    private BigDecimal purchasePrice;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "PURCHASEDATE")
    private Date purchaseDate;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCOUNT_ACCOUNTID")
    private AccountDataBean account;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "QUOTE_SYMBOL")
    private QuoteDataBean quote;
    @Transient
    private String quoteID;

    public HoldingDataBean() { }
    public HoldingDataBean(Integer holdingID, double quantity, BigDecimal purchasePrice,
                           Date purchaseDate, String quoteID) {
        this.holdingID = holdingID;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.purchaseDate = purchaseDate;
        this.quoteID = quoteID;
    }
    public HoldingDataBean(double quantity, BigDecimal purchasePrice, Date purchaseDate,
                           AccountDataBean account, QuoteDataBean quote) {
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.purchaseDate = purchaseDate;
        this.account = account;
        this.quote = quote;
    }
    public static HoldingDataBean getRandomInstance() {
        return new HoldingDataBean(TradeConfig.rndInt(100000), TradeConfig.rndQuantity(),
                TradeConfig.rndBigDecimal(1000.0f), new Date(TradeConfig.rndInt(Integer.MAX_VALUE)),
                TradeConfig.rndSymbol());
    }
    public Integer getHoldingID() { return holdingID; }
    public void setHoldingID(Integer holdingID) { this.holdingID = holdingID; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public Date getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(Date purchaseDate) { this.purchaseDate = purchaseDate; }
    public AccountDataBean getAccount() { return account; }
    public void setAccount(AccountDataBean account) { this.account = account; }
    public QuoteDataBean getQuote() { return quote; }
    public void setQuote(QuoteDataBean quote) { this.quote = quote; }
    public String getQuoteID() { return quote == null ? quoteID : quote.getSymbol(); }
    public void setQuoteID(String quoteID) { this.quoteID = quoteID; }
    public String toHTML() {
        return "<BR>Holding Data for holding: " + holdingID + "</B><LI>      quantity:" + quantity
                + "</LI><LI> purchasePrice:" + purchasePrice + "</LI><LI>  purchaseDate:" + purchaseDate
                + "</LI><LI>       quoteID:" + getQuoteID() + "</LI>";
    }
    @Override public String toString() {
        return "\n\tHolding Data for holding: " + holdingID + "\n\t\t      quantity:" + quantity
                + "\n\t\t purchasePrice:" + purchasePrice + "\n\t\t  purchaseDate:" + purchaseDate
                + "\n\t\t       quoteID:" + getQuoteID();
    }
    @Override public int hashCode() { return Objects.hashCode(holdingID); }
    @Override public boolean equals(Object object) {
        return object instanceof HoldingDataBean other && Objects.equals(holdingID, other.holdingID);
    }
}
