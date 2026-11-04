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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.junit.jupiter.api.Test;

/**
 * The entities carry no Jakarta Validation constraints; their "validation" is the JPA mapping
 * (table/column names, non-null ids) plus id-based equality. These tests pin that contract,
 * which a Spring Data JPA port must keep to read the same schema.
 */
class EntityMappingTest {

    private static void assertMapped(Class<?> entity, String table, String idField, String idColumn) throws Exception {
        assertEquals(table, entity.getAnnotation(Entity.class).name());
        assertEquals(table, entity.getAnnotation(Table.class).name());
        Field id = entity.getDeclaredField(idField);
        assertNotNull(id.getAnnotation(Id.class), entity.getSimpleName() + "." + idField + " is not @Id");
        Column column = id.getAnnotation(Column.class);
        assertEquals(idColumn, column.name());
        assertFalse(column.nullable(), idColumn + " must be NOT NULL");
    }

    @Test
    void entitiesMapToTheLegacyTablesWithNonNullIds() throws Exception {
        assertMapped(AccountDataBean.class, "accountejb", "accountID", "ACCOUNTID");
        assertMapped(AccountProfileDataBean.class, "accountprofileejb", "userID", "USERID");
        assertMapped(HoldingDataBean.class, "holdingejb", "holdingID", "HOLDINGID");
        assertMapped(OrderDataBean.class, "orderejb", "orderID", "ORDERID");
        assertMapped(QuoteDataBean.class, "quoteejb", "symbol", "SYMBOL");
    }

    @Test
    void equalityIsByIdentifierOnly() {
        Date now = new Date();
        HoldingDataBean h1 = new HoldingDataBean(7, 1.0, new BigDecimal("1.00"), now, "s:1");
        HoldingDataBean h2 = new HoldingDataBean(7, 99.0, new BigDecimal("9.00"), now, "s:2");
        assertEquals(h1, h2);
        assertEquals(h1.hashCode(), h2.hashCode());
        assertNotEquals(h1, new HoldingDataBean(8, 1.0, new BigDecimal("1.00"), now, "s:1"));
        assertEquals(new QuoteDataBean("s:3"), new QuoteDataBean("s:3"));
        assertNotEquals(new QuoteDataBean("s:3"), new QuoteDataBean("s:4"));
    }

    @Test
    void orderTypeAndStatusPredicates() {
        OrderDataBean order = new OrderDataBean(1, "BUY", "open", new Date(), null, 10.0,
                new BigDecimal("145.06"), new BigDecimal("24.95"), "s:1");
        assertTrue(order.isBuy());
        assertFalse(order.isSell());
        assertTrue(order.isOpen());
        assertFalse(order.isCompleted());
        order.setOrderStatus("processing");
        assertTrue(order.isOpen());
        order.setOrderStatus("closed");
        assertFalse(order.isOpen());
        assertFalse(order.isCompleted());
    }

    @Test
    void cancelMarksOrderCancelledAndCompleted() {
        OrderDataBean order = new OrderDataBean(2, "sell", "open", new Date(), null, 1.0,
                BigDecimal.ONE, BigDecimal.ONE, "s:2");
        order.cancel();
        assertEquals("cancelled", order.getOrderStatus());
        assertTrue(order.isCompleted());
        assertFalse(order.isOpen());
    }
}
