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
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;

@Entity(name = "accountejb")
@Table(name = "accountejb")
public class AccountDataBean implements Serializable {
    @TableGenerator(name = "accountIdGen", table = "KEYGENEJB", pkColumnName = "KEYNAME",
            valueColumnName = "KEYVAL", pkColumnValue = "account", allocationSize = 1000)
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "accountIdGen")
    @Column(name = "ACCOUNTID", nullable = false)
    private Integer accountID;
    @Column(name = "LOGINCOUNT", nullable = false)
    private int loginCount;
    @Column(name = "LOGOUTCOUNT", nullable = false)
    private int logoutCount;
    @Column(name = "LASTLOGIN")
    private Date lastLogin;
    @Column(name = "CREATIONDATE")
    private Date creationDate;
    @Column(name = "BALANCE")
    private BigDecimal balance;
    @Column(name = "OPENBALANCE")
    private BigDecimal openBalance;
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private Collection<OrderDataBean> orders;
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private Collection<HoldingDataBean> holdings;
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "PROFILE_USERID")
    private AccountProfileDataBean profile;
    @Transient
    private String profileID;

    public AccountDataBean() { }
    public AccountDataBean(Integer accountID, int loginCount, int logoutCount, Date lastLogin,
                           Date creationDate, BigDecimal balance, BigDecimal openBalance, String profileID) {
        this(loginCount, logoutCount, lastLogin, creationDate, balance, openBalance, profileID);
        this.accountID = accountID;
    }
    public AccountDataBean(int loginCount, int logoutCount, Date lastLogin, Date creationDate,
                           BigDecimal balance, BigDecimal openBalance, String profileID) {
        this.loginCount = loginCount;
        this.logoutCount = logoutCount;
        this.lastLogin = lastLogin;
        this.creationDate = creationDate;
        this.balance = balance;
        this.openBalance = openBalance;
        this.profileID = profileID;
    }
    public static AccountDataBean getRandomInstance() {
        return new AccountDataBean(TradeConfig.rndInt(100000), TradeConfig.rndInt(10000),
                TradeConfig.rndInt(10000), new Date(), new Date(TradeConfig.rndInt(Integer.MAX_VALUE)),
                TradeConfig.rndBigDecimal(1000000.0f), TradeConfig.rndBigDecimal(1000000.0f), TradeConfig.rndUserID());
    }
    public Integer getAccountID() { return accountID; }
    public void setAccountID(Integer accountID) { this.accountID = accountID; }
    public int getLoginCount() { return loginCount; }
    public void setLoginCount(int loginCount) { this.loginCount = loginCount; }
    public int getLogoutCount() { return logoutCount; }
    public void setLogoutCount(int logoutCount) { this.logoutCount = logoutCount; }
    public Date getLastLogin() { return lastLogin; }
    public void setLastLogin(Date lastLogin) { this.lastLogin = lastLogin; }
    public Date getCreationDate() { return creationDate; }
    public void setCreationDate(Date creationDate) { this.creationDate = creationDate; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getOpenBalance() { return openBalance; }
    public void setOpenBalance(BigDecimal openBalance) { this.openBalance = openBalance; }
    public Collection<OrderDataBean> getOrders() { return orders; }
    public void setOrders(Collection<OrderDataBean> orders) { this.orders = orders; }
    public Collection<HoldingDataBean> getHoldings() { return holdings; }
    public void setHoldings(Collection<HoldingDataBean> holdings) { this.holdings = holdings; }
    public AccountProfileDataBean getProfile() { return profile; }
    public void setProfile(AccountProfileDataBean profile) { this.profile = profile; }
    public String getProfileID() { return profile != null ? profile.getUserID() : profileID; }
    public void setProfileID(String profileID) { this.profileID = profileID; }
    public void login(String password) {
        if (profile == null || !Objects.equals(profile.getPassword(), password)) {
            throw new IllegalArgumentException("Incorrect password for user " + getProfileID());
        }
        loginCount++;
        lastLogin = new Date();
    }
    public void logout() { logoutCount++; }
    public String toHTML() {
        return "<BR>Account Data for account: <B>" + accountID + "</B><LI>   loginCount:" + loginCount
                + "</LI><LI>  logoutCount:" + logoutCount + "</LI><LI>    lastLogin:" + lastLogin
                + "</LI><LI> creationDate:" + creationDate + "</LI><LI>      balance:" + balance
                + "</LI><LI>  openBalance:" + openBalance + "</LI><LI>    profileID:" + getProfileID() + "</LI>";
    }
    @Override public String toString() {
        return "\n\tAccount Data for account: " + accountID + "\n\t\t   loginCount:" + loginCount
                + "\n\t\t  logoutCount:" + logoutCount + "\n\t\t    lastLogin:" + lastLogin
                + "\n\t\t creationDate:" + creationDate + "\n\t\t      balance:" + balance
                + "\n\t\t  openBalance:" + openBalance + "\n\t\t    profileID:" + getProfileID();
    }
    @Override public int hashCode() { return Objects.hashCode(accountID); }
    @Override public boolean equals(Object object) {
        return object instanceof AccountDataBean other && Objects.equals(accountID, other.accountID);
    }
}
