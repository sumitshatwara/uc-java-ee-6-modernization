/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.daytrader.rest;

import java.util.HashMap;
import java.util.Iterator;
import org.springframework.stereotype.Component;

@Component
public class AddressBookDatabase {

    // Preserve legacy HashMap iteration order for the recorded REST responses.
    private final HashMap<String, Address> addressMap = new HashMap<>();

    public AddressBookDatabase() {
        addressMap.put("Entry5", new Address("Entry5", "12345", "Address", "City", "State", "Country"));
        addressMap.put("Entry4", new Address("Entry4", "12345", "Address", "City", "State", "Country"));
        addressMap.put("Entry3", new Address("Entry3", "12345", "Address", "City", "State", "Country"));
        addressMap.put("Entry2", new Address("Entry2", "12345", "Address", "City", "State", "Country"));
        addressMap.put("Entry1", new Address("Entry1", "12345", "Address", "City", "State", "Country"));
    }

    public Address getAddress(String entryName) {
        return addressMap.get(entryName);
    }

    public void storeAddress(String entryName, Address address) {
        addressMap.put(entryName, address);
    }

    public Iterator<Address> getAddresses() {
        return addressMap.values().iterator();
    }

    public void removeAddress(String entryName) {
        addressMap.remove(entryName);
    }

    public void clearEntries() {
        addressMap.clear();
    }

}
