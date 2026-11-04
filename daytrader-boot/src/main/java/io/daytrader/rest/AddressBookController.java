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

import java.util.Iterator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/addresses")
public class AddressBookController {

    private final AddressBookDatabase database;

    public AddressBookController(AddressBookDatabase database) {
        this.database = database;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public AddressList getAddresses() {
        AddressList addressList = new AddressList();
        Iterator<Address> addresses = database.getAddresses();
        while (addresses.hasNext()) {
            addressList.getAddresses().add(addresses.next());
        }
        return addressList;
    }

    @GetMapping(path = "/search/{searchstring}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AddressList search(@PathVariable("searchstring") String searchString) {
        AddressList addressList = new AddressList();
        Iterator<Address> addresses = database.getAddresses();
        while (addresses.hasNext()) {
            Address address = addresses.next();
            if (address.getEntryName().startsWith(searchString)) {
                addressList.getAddresses().add(address);
            }
        }
        return addressList;
    }

    /**
     * The legacy JAX-RS 1.1 sub-resource locator had no HTTP method; its
     * only {@code @GET} method consumed XML or form-urlencoded, so a plain
     * GET with no matching Content-Type returned an empty 415; null returned
     * 404. Reproduce the recorded quirk (docs/BASELINE.md, parity 13-rest-entry)
     * explicitly; the unreachable XML representation is not ported.
     */
    @GetMapping("/{entryName}")
    public ResponseEntity<Void> getAddress(@PathVariable("entryName") String entryName) {
        if (database.getAddress(entryName) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
    }

}
