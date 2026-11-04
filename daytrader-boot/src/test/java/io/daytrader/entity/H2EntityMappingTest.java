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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:entity-mapping;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class H2EntityMappingTest {

    private static final List<Class<?>> ENTITIES = List.of(AccountDataBean.class, HoldingDataBean.class,
            OrderDataBean.class, QuoteDataBean.class, AccountProfileDataBean.class);

    private static final Map<String, Set<String>> COLUMNS = Map.of(
            "accountejb", Set.of("creationdate", "openbalance", "logoutcount", "balance", "accountid",
                    "lastlogin", "logincount", "profile_userid"),
            "holdingejb", Set.of("purchaseprice", "holdingid", "quantity", "purchasedate", "account_accountid",
                    "quote_symbol"),
            "orderejb", Set.of("orderfee", "completiondate", "ordertype", "orderstatus", "price", "quantity",
                    "opendate", "orderid", "account_accountid", "quote_symbol", "holding_holdingid"),
            "quoteejb", Set.of("low", "open1", "volume", "price", "high", "companyname", "symbol", "change1"),
            "accountprofileejb", Set.of("address", "passwd", "userid", "email", "creditcard", "fullname"),
            "keygenejb", Set.of("keyval", "keyname"));

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void loadProductionDdl(@Autowired DataSource dataSource) {
        ResourceDatabasePopulator schema = new ResourceDatabasePopulator(
                new ClassPathResource("db/ddl/h2/Table.ddl"));
        schema.setCommentPrefixes("#", "--");
        schema.setIgnoreFailedDrops(true);
        schema.execute(dataSource);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (String key : List.of("account", "holding", "order")) {
            jdbc.update("insert into keygenejb (keyname, keyval) values (?, ?)", key, 20000);
        }
    }

    @Test
    void allSixTablesHaveTheLegacyColumnsAndPrimaryKeys() {
        COLUMNS.forEach((table, expected) -> assertEquals(expected, new HashSet<>(jdbc.queryForList(
                "select column_name from information_schema.columns where table_schema='public' and table_name=?",
                String.class, table)), table));
        Map.of("accountejb", "accountid", "holdingejb", "holdingid", "orderejb", "orderid",
                "quoteejb", "symbol", "accountprofileejb", "userid", "keygenejb", "keyname")
                .forEach((table, key) -> assertEquals(List.of(key), jdbc.queryForList("""
                        select k.column_name from information_schema.key_column_usage k
                        join information_schema.table_constraints c
                          on c.constraint_catalog=k.constraint_catalog
                         and c.constraint_schema=k.constraint_schema and c.constraint_name=k.constraint_name
                        where c.constraint_type='PRIMARY KEY' and c.table_schema='public' and c.table_name=?
                        """, String.class, table), table));
        assertEquals("NO", column("keygenejb", "keyval").get("is_nullable"));
        assertEquals("integer", column("keygenejb", "keyval").get("data_type"));
        assertEquals("NO", column("keygenejb", "keyname").get("is_nullable"));
        assertEquals(250L, ((Number) column("keygenejb", "keyname").get("character_maximum_length")).longValue());
    }

    @Test
    void everyEntityColumnAndJoinMapsToTheProductionDdl() {
        for (Class<?> entity : ENTITIES) {
            String table = entity.getAnnotation(Table.class).name().toLowerCase(Locale.ROOT);
            Set<String> mappedColumns = new HashSet<>();
            for (Field field : entity.getDeclaredFields()) {
                Column column = field.getAnnotation(Column.class);
                JoinColumn join = field.getAnnotation(JoinColumn.class);
                if (column == null && join == null) continue;
                String name = (column == null ? join.name() : column.name()).toLowerCase(Locale.ROOT);
                mappedColumns.add(name);
                Map<String, Object> ddl = column(table, name);
                assertEquals(column == null || column.nullable() ? "YES" : "NO", ddl.get("is_nullable"),
                        table + "." + name);
                if (field.getType() == BigDecimal.class) {
                    assertEquals(14, ((Number) ddl.get("numeric_precision")).intValue(), name);
                    assertEquals(2, ((Number) ddl.get("numeric_scale")).intValue(), name);
                }
                if (field.getType() == String.class || name.endsWith("userid") || name.endsWith("symbol")) {
                    assertEquals(250L, ((Number) ddl.get("character_maximum_length")).longValue(), name);
                }
            }
            assertEquals(COLUMNS.get(table), mappedColumns, entity.getSimpleName());
        }
    }

    @Test
    void tableGeneratorsRetainLegacyKeySegmentsAndThousandIdBlocks() throws Exception {
        assertGenerator(AccountDataBean.class, "accountID", "accountIdGen", "account");
        assertGenerator(HoldingDataBean.class, "holdingID", "holdingIdGen", "holding");
        assertGenerator(OrderDataBean.class, "orderID", "orderIdGen", "order");
    }

    @Test
    void entityGraphRoundTripsThroughDdlAndReservesIdBlocksInKeygen() {
        Timestamp now = Timestamp.valueOf("2024-01-02 03:04:05");
        AccountProfileDataBean profile = new AccountProfileDataBean("uid:mapping", "pw", "Mapping Tester",
                "1 Test Street", "mapping@example.com", "1111-2222");
        AccountDataBean account = new AccountDataBean(2, 1, now, now,
                new BigDecimal("1234.56"), new BigDecimal("2000.00"), profile.getUserID());
        account.setProfile(profile);
        entityManager.persist(account);
        QuoteDataBean quote = new QuoteDataBean("s:mapping", "Mapping Incorporated", 125.0,
                new BigDecimal("12.34"), new BigDecimal("12.00"), new BigDecimal("11.00"),
                new BigDecimal("13.00"), 0.34);
        entityManager.persist(quote);
        HoldingDataBean holding = new HoldingDataBean(10.0, new BigDecimal("12.00"), now, account, quote);
        entityManager.persist(holding);
        OrderDataBean order = new OrderDataBean("buy", "closed", now, now, 10.0,
                quote.getPrice(), new BigDecimal("24.95"), account, quote, holding);
        entityManager.persistAndFlush(order);
        entityManager.clear();

        assertRow("accountejb", "accountid", account.getAccountID(), Map.of(
                "creationdate", now, "openbalance", new BigDecimal("2000.00"), "logoutcount", 1,
                "balance", new BigDecimal("1234.56"), "accountid", account.getAccountID(),
                "lastlogin", now, "logincount", 2, "profile_userid", profile.getUserID()));
        assertRow("accountprofileejb", "userid", profile.getUserID(), Map.of(
                "address", "1 Test Street", "passwd", "pw", "userid", profile.getUserID(),
                "email", "mapping@example.com", "creditcard", "1111-2222", "fullname", "Mapping Tester"));
        assertRow("quoteejb", "symbol", quote.getSymbol(), Map.of(
                "low", new BigDecimal("11.00"), "open1", new BigDecimal("12.00"), "volume", 125.0,
                "price", new BigDecimal("12.34"), "high", new BigDecimal("13.00"),
                "companyname", "Mapping Incorporated", "symbol", quote.getSymbol(), "change1", 0.34));
        assertRow("holdingejb", "holdingid", holding.getHoldingID(), Map.of(
                "purchaseprice", new BigDecimal("12.00"), "holdingid", holding.getHoldingID(), "quantity", 10.0,
                "purchasedate", now, "account_accountid", account.getAccountID(), "quote_symbol", quote.getSymbol()));
        assertRow("orderejb", "orderid", order.getOrderID(), Map.ofEntries(
                Map.entry("orderfee", new BigDecimal("24.95")), Map.entry("completiondate", now),
                Map.entry("ordertype", "buy"), Map.entry("orderstatus", "closed"),
                Map.entry("price", new BigDecimal("12.34")), Map.entry("quantity", 10.0),
                Map.entry("opendate", now), Map.entry("orderid", order.getOrderID()),
                Map.entry("account_accountid", account.getAccountID()), Map.entry("quote_symbol", quote.getSymbol()),
                Map.entry("holding_holdingid", holding.getHoldingID())));

        OrderDataBean loaded = entityManager.find(OrderDataBean.class, order.getOrderID());
        assertEquals(profile.getUserID(), loaded.getAccount().getProfile().getUserID());
        assertEquals(holding.getHoldingID(), loaded.getHolding().getHoldingID());
        assertEquals(quote.getSymbol(), loaded.getQuote().getSymbol());
        assertEquals(List.of(holding.getHoldingID()), loaded.getAccount().getHoldings().stream()
                .map(HoldingDataBean::getHoldingID).toList());
        assertEquals(List.of(order.getOrderID()), loaded.getAccount().getOrders().stream()
                .map(OrderDataBean::getOrderID).toList());
        assertEquals(account.getAccountID(), loaded.getAccount().getProfile().getAccount().getAccountID());

        assertBlockAllocation("account", account.getAccountID(), () ->
                entityManager.persist(new AccountDataBean()).getAccountID());
        assertBlockAllocation("holding", holding.getHoldingID(), () ->
                entityManager.persist(new HoldingDataBean()).getHoldingID());
        assertBlockAllocation("order", order.getOrderID(), () ->
                entityManager.persist(new OrderDataBean()).getOrderID());
        entityManager.flush();
    }

    private void assertGenerator(Class<?> entity, String id, String name, String key) throws Exception {
        Field field = entity.getDeclaredField(id);
        TableGenerator generator = field.getAnnotation(TableGenerator.class);
        assertNotNull(generator);
        assertEquals(name, generator.name());
        assertEquals("KEYGENEJB", generator.table());
        assertEquals("KEYNAME", generator.pkColumnName());
        assertEquals("KEYVAL", generator.valueColumnName());
        assertEquals(key, generator.pkColumnValue());
        assertEquals(1000, generator.allocationSize());
        assertEquals(GenerationType.TABLE, field.getAnnotation(GeneratedValue.class).strategy());
        assertEquals(generator.name(), field.getAnnotation(GeneratedValue.class).generator());
    }

    private void assertBlockAllocation(String key, int firstId, IntSupplier persist) {
        int before = keyValue(key);
        assertTrue(before > 20000, key + " must reserve from the existing KEYGENEJB row");
        for (int i = 1; i <= 1000; i++) {
            assertEquals(firstId + i, persist.getAsInt(), key + " ID " + i);
        }
        assertEquals(before + 1000, keyValue(key), key + " must reserve exactly one additional block");
    }

    private int keyValue(String key) {
        return jdbc.queryForObject("select keyval from keygenejb where keyname=?", Integer.class, key);
    }

    private Map<String, Object> column(String table, String name) {
        return jdbc.queryForMap("""
                select * from information_schema.columns
                where table_schema='public' and table_name=? and column_name=?
                """, table, name);
    }

    private void assertRow(String table, String key, Object id, Map<String, Object> expected) {
        assertEquals(expected, jdbc.queryForMap("select * from " + table + " where " + key + "=?", id), table);
    }
}
