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

@Entity(name = "orderejb")
@Table(name = "orderejb")
public class OrderDataBean implements Serializable {
    @TableGenerator(name = "orderIdGen", table = "KEYGENEJB", pkColumnName = "KEYNAME",
            valueColumnName = "KEYVAL", pkColumnValue = "order", allocationSize = 1000)
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "orderIdGen")
    @Column(name = "ORDERID", nullable = false)
    private Integer orderID;
    @Column(name = "ORDERTYPE")
    private String orderType;
    @Column(name = "ORDERSTATUS")
    private String orderStatus;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "OPENDATE")
    private Date openDate;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "COMPLETIONDATE")
    private Date completionDate;
    @Column(name = "QUANTITY", nullable = false)
    private double quantity;
    @Column(name = "PRICE")
    private BigDecimal price;
    @Column(name = "ORDERFEE")
    private BigDecimal orderFee;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCOUNT_ACCOUNTID")
    private AccountDataBean account;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "QUOTE_SYMBOL")
    private QuoteDataBean quote;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HOLDING_HOLDINGID")
    private HoldingDataBean holding;
    @Transient
    private String symbol;

    public OrderDataBean() { }
    public OrderDataBean(Integer orderID, String orderType, String orderStatus, Date openDate,
                         Date completionDate, double quantity, BigDecimal price, BigDecimal orderFee,
                         String symbol) {
        this.orderID = orderID;
        this.orderType = orderType;
        this.orderStatus = orderStatus;
        this.openDate = openDate;
        this.completionDate = completionDate;
        this.quantity = quantity;
        this.price = price;
        this.orderFee = orderFee;
        this.symbol = symbol;
    }
    public OrderDataBean(String orderType, String orderStatus, Date openDate, Date completionDate,
                         double quantity, BigDecimal price, BigDecimal orderFee,
                         AccountDataBean account, QuoteDataBean quote, HoldingDataBean holding) {
        this.orderType = orderType;
        this.orderStatus = orderStatus;
        this.openDate = openDate;
        this.completionDate = completionDate;
        this.quantity = quantity;
        this.price = price;
        this.orderFee = orderFee;
        this.account = account;
        this.quote = quote;
        this.holding = holding;
    }
    public static OrderDataBean getRandomInstance() {
        return new OrderDataBean(TradeConfig.rndInt(100000), TradeConfig.rndBoolean() ? "buy" : "sell",
                "open", new Date(TradeConfig.rndInt(Integer.MAX_VALUE)),
                new Date(TradeConfig.rndInt(Integer.MAX_VALUE)), TradeConfig.rndQuantity(),
                TradeConfig.rndBigDecimal(1000.0f), TradeConfig.rndBigDecimal(1000.0f), TradeConfig.rndSymbol());
    }
    public Integer getOrderID() { return orderID; }
    public void setOrderID(Integer orderID) { this.orderID = orderID; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public Date getOpenDate() { return openDate; }
    public void setOpenDate(Date openDate) { this.openDate = openDate; }
    public Date getCompletionDate() { return completionDate; }
    public void setCompletionDate(Date completionDate) { this.completionDate = completionDate; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOrderFee() { return orderFee; }
    public void setOrderFee(BigDecimal orderFee) { this.orderFee = orderFee; }
    public AccountDataBean getAccount() { return account; }
    public void setAccount(AccountDataBean account) { this.account = account; }
    public QuoteDataBean getQuote() { return quote; }
    public void setQuote(QuoteDataBean quote) { this.quote = quote; }
    public HoldingDataBean getHolding() { return holding; }
    public void setHolding(HoldingDataBean holding) { this.holding = holding; }
    public String getSymbol() { return quote == null ? symbol : quote.getSymbol(); }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public boolean isBuy() { return "buy".equalsIgnoreCase(orderType); }
    public boolean isSell() { return "sell".equalsIgnoreCase(orderType); }
    public boolean isOpen() { return "open".equalsIgnoreCase(orderStatus) || "processing".equalsIgnoreCase(orderStatus); }
    public boolean isCompleted() { return "closed".equalsIgnoreCase(orderStatus) || "completed".equalsIgnoreCase(orderStatus); }
    public void cancel() { orderStatus = "cancelled"; }
    public String toHTML() {
        return "<BR>Order <B>" + orderID + "</B><LI>      orderType: " + orderType
                + "</LI><LI>    orderStatus: " + orderStatus + "</LI><LI>       openDate: " + openDate
                + "</LI><LI> completionDate: " + completionDate + "</LI><LI>       quantity: " + quantity
                + "</LI><LI>          price: " + price + "</LI><LI>       orderFee: " + orderFee
                + "</LI><LI>         symbol: " + getSymbol() + "</LI>";
    }
    @Override public String toString() {
        return "Order " + orderID + "\n\t      orderType: " + orderType + "\n\t    orderStatus: " + orderStatus
                + "\n\t       openDate: " + openDate + "\n\t completionDate: " + completionDate
                + "\n\t       quantity: " + quantity + "\n\t          price: " + price
                + "\n\t       orderFee: " + orderFee + "\n\t         symbol: " + getSymbol();
    }
    @Override public int hashCode() { return Objects.hashCode(orderID); }
    @Override public boolean equals(Object object) {
        return object instanceof OrderDataBean other && Objects.equals(orderID, other.orderID);
    }
}
