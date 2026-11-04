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
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity(name = "accountprofileejb")
@Table(name = "accountprofileejb")
public class AccountProfileDataBean implements Serializable {
    @Id
    @Column(name = "USERID", nullable = false)
    private String userID;
    @Column(name = "PASSWD")
    private String passwd;
    @Column(name = "FULLNAME")
    private String fullName;
    @Column(name = "ADDRESS")
    private String address;
    @Column(name = "EMAIL")
    private String email;
    @Column(name = "CREDITCARD")
    private String creditCard;
    @OneToOne(mappedBy = "profile", fetch = FetchType.LAZY)
    private AccountDataBean account;

    public AccountProfileDataBean() { }
    public AccountProfileDataBean(String userID, String password, String fullName, String address,
                                  String email, String creditCard) {
        this.userID = userID;
        this.passwd = password;
        this.fullName = fullName;
        this.address = address;
        this.email = email;
        this.creditCard = creditCard;
    }
    public static AccountProfileDataBean getRandomInstance() {
        return new AccountProfileDataBean(TradeConfig.rndUserID(), TradeConfig.rndUserID(),
                TradeConfig.rndFullName(), TradeConfig.rndAddress(),
                TradeConfig.rndEmail(TradeConfig.rndUserID()), TradeConfig.rndCreditCard());
    }
    public String getUserID() { return userID; }
    public void setUserID(String userID) { this.userID = userID; }
    public String getPassword() { return passwd; }
    public void setPassword(String password) { passwd = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCreditCard() { return creditCard; }
    public void setCreditCard(String creditCard) { this.creditCard = creditCard; }
    public AccountDataBean getAccount() { return account; }
    public void setAccount(AccountDataBean account) { this.account = account; }
    public String toHTML() {
        return "<BR>Account Profile Data for userID: <B>" + userID + "</B><LI>   passwd:" + passwd
                + "</LI><LI>   fullName:" + fullName + "</LI><LI>    address:" + address
                + "</LI><LI>      email:" + email + "</LI><LI> creditCard:" + creditCard + "</LI>";
    }
    @Override public String toString() {
        return "\n\tAccount Profile Data for userID:" + userID + "\n\t\t   passwd:" + passwd
                + "\n\t\t   fullName:" + fullName + "\n\t\t    address:" + address
                + "\n\t\t      email:" + email + "\n\t\t creditCard:" + creditCard;
    }
    @Override public int hashCode() { return Objects.hashCode(userID); }
    @Override public boolean equals(Object object) {
        return object instanceof AccountProfileDataBean other && Objects.equals(userID, other.userID);
    }
}
