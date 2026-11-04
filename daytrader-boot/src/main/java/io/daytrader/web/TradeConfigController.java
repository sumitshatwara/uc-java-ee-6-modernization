package io.daytrader.web;

import io.daytrader.config.TradeConfig;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TradeConfigController {
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final ResourceLoader resources;
    private final TradeConfig config;

    public TradeConfigController(JdbcTemplate jdbc, DataSource dataSource,
                                 ResourceLoader resources, TradeConfig config) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.resources = resources;
        this.config = config;
    }

    @GetMapping("/config")
    public ResponseEntity<String> configure(@RequestParam String action) throws Exception {
        return switch (action) {
            case "buildDBTables" -> ResponseEntity.ok(createTables());
            case "buildDB" -> ResponseEntity.ok(seedDatabase());
            default -> ResponseEntity.badRequest().body("TradeConfigServlet: Invalid action " + action);
        };
    }

    private String createTables() throws Exception {
        String ddl = databaseIsPostgres() ? "classpath:db/ddl/postgresql/Table.ddl" : "classpath:db/ddl/h2/Table.ddl";
        Resource resource = resources.getResource(ddl);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(),
                StandardCharsets.UTF_8)); Connection connection = dataSource.getConnection()) {
            StringBuilder statement = new StringBuilder();
            for (String line; (line = reader.readLine()) != null;) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                statement.append(' ').append(trimmed);
                if (!trimmed.endsWith(";")) continue;
                String sql = statement.toString().replace(";", "").trim();
                try (var sqlStatement = connection.createStatement()) {
                    sqlStatement.execute(sql);
                } catch (Exception e) {
                    if (!sql.regionMatches(true, 0, "DROP TABLE", 0, "DROP TABLE".length())) throw e;
                }
                statement.setLength(0);
            }
        }
        return "TradeBuildDB: tables successfully created!";
    }

    private String seedDatabase() {
        long started = System.nanoTime();
        jdbc.update("DELETE FROM orderejb");
        jdbc.update("DELETE FROM holdingejb");
        jdbc.update("DELETE FROM accountejb");
        jdbc.update("DELETE FROM accountprofileejb");
        jdbc.update("DELETE FROM quoteejb");
        jdbc.update("DELETE FROM keygenejb");
        jdbc.update("INSERT INTO keygenejb (keyname, keyval) VALUES ('account', 1000000), ('holding', 1000000), ('order', 1000000)");

        List<Object[]> quoteRows = new ArrayList<>(1000);
        BigDecimal[] prices = new BigDecimal[config.getMaxQuotes()];
        for (int i = 0; i < config.getMaxQuotes(); i++) {
            String symbol = "s:" + i;
            BigDecimal price = BigDecimal.valueOf(TradeConfig.rndPrice()).setScale(2);
            prices[i] = price;
            quoteRows.add(new Object[]{price, price, 0.0, price, price, "S" + i + " Incorporated", symbol, 0.0});
            if (quoteRows.size() == 1000 || i == config.getMaxQuotes() - 1) {
                jdbc.batchUpdate("INSERT INTO quoteejb (low, open1, volume, price, high, companyname, symbol, change1) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", quoteRows);
                quoteRows.clear();
            }
        }

        BigDecimal[] cash = new BigDecimal[config.getMaxUsers()];
        Timestamp now = Timestamp.from(Instant.now());
        List<Object[]> profiles = new ArrayList<>(1000);
        for (int i = 0; i < config.getMaxUsers(); i++) {
            String userID = "uid:" + i;
            String fullName = TradeConfig.rndFullName();
            String email = TradeConfig.rndEmail(userID);
            String address = TradeConfig.rndAddress();
            String creditCard = TradeConfig.rndCreditCard();
            BigDecimal balance = i == 0 ? new BigDecimal("1000000.00")
                    : BigDecimal.valueOf(200000 + TradeConfig.rndInt(100000)).setScale(2);
            cash[i] = balance;
            int accountID = i + 1;
            profiles.add(new Object[]{userID, "xxx", fullName, address, email, creditCard});
            if (profiles.size() == 1000 || i == config.getMaxUsers() - 1) {
                jdbc.batchUpdate("INSERT INTO accountprofileejb (userid, passwd, fullname, address, email, creditcard) "
                        + "VALUES (?, ?, ?, ?, ?, ?)", profiles);
                List<Object[]> accountRows = new ArrayList<>(profiles.size());
                int start = i - profiles.size() + 1;
                for (int j = start; j <= i; j++) {
                    accountRows.add(new Object[]{now, cash[j], 0, cash[j], j + 1, null, 0, "uid:" + j});
                }
                jdbc.batchUpdate("INSERT INTO accountejb (creationdate, openbalance, logoutcount, balance, accountid, "
                        + "lastlogin, logincount, profile_userid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", accountRows);
                profiles.clear();
            }
        }

        List<Object[]> holdingRows = new ArrayList<>(1000);
        List<Object[]> orderRows = new ArrayList<>(1000);
        double[] volumes = new double[config.getMaxQuotes()];
        int holdingID = 0;
        int orderID = 0;
        for (int i = 0; i < config.getMaxUsers(); i++) {
            int holdingCount = TradeConfig.rndInt(config.getMaxHoldings() + 1);
            for (int j = 0; j < holdingCount; j++) {
                int quoteIndex = TradeConfig.rndInt(Math.max(1, config.getMaxQuotes() - 1));
                String symbol = "s:" + quoteIndex;
                double quantity = TradeConfig.rndQuantity();
                BigDecimal price = prices[quoteIndex];
                int accountID = i + 1;
                int currentHolding = ++holdingID;
                int currentOrder = ++orderID;
                BigDecimal total = price.multiply(BigDecimal.valueOf(quantity)).add(TradeConfig.getOrderFee("buy"));
                cash[i] = cash[i].subtract(total);
                volumes[quoteIndex] += quantity;
                holdingRows.add(new Object[]{price, currentHolding, quantity, now, accountID, symbol});
                orderRows.add(new Object[]{TradeConfig.getOrderFee("buy"), now, "buy", "closed", price, quantity,
                        now, currentOrder, accountID, symbol, currentHolding});
                if (holdingRows.size() == 1000) {
                    flushSeedOrders(holdingRows, orderRows);
                }
            }
        }
        if (!holdingRows.isEmpty()) flushSeedOrders(holdingRows, orderRows);

        List<Object[]> accountUpdates = new ArrayList<>(1000);
        for (int i = 0; i < cash.length; i++) {
            accountUpdates.add(new Object[]{cash[i], i + 1});
            if (accountUpdates.size() == 1000 || i == cash.length - 1) {
                jdbc.batchUpdate("UPDATE accountejb SET balance=? WHERE accountid=?", accountUpdates);
                accountUpdates.clear();
            }
        }
        List<Object[]> quoteUpdates = new ArrayList<>(1000);
        for (int i = 0; i < volumes.length; i++) {
            quoteUpdates.add(new Object[]{volumes[i], "s:" + i});
            if (quoteUpdates.size() == 1000 || i == volumes.length - 1) {
                jdbc.batchUpdate("UPDATE quoteejb SET volume=? WHERE symbol=?", quoteUpdates);
                quoteUpdates.clear();
            }
        }
        jdbc.update("UPDATE keygenejb SET keyval=? WHERE keyname='account'", 1000000);
        jdbc.update("UPDATE keygenejb SET keyval=? WHERE keyname='holding'", 1000000 + holdingID);
        jdbc.update("UPDATE keygenejb SET keyval=? WHERE keyname='order'", 1000000 + orderID);
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        return "Registering " + config.getMaxUsers() + " Users; created " + config.getMaxQuotes()
                + " Quotes in " + elapsedMillis + " ms";
    }

    private void flushSeedOrders(List<Object[]> holdingRows, List<Object[]> orderRows) {
        jdbc.batchUpdate("INSERT INTO holdingejb (purchaseprice, holdingid, quantity, purchasedate, "
                + "account_accountid, quote_symbol) VALUES (?, ?, ?, ?, ?, ?)", holdingRows);
        jdbc.batchUpdate("INSERT INTO orderejb (orderfee, completiondate, ordertype, orderstatus, price, quantity, "
                + "opendate, orderid, account_accountid, quote_symbol, holding_holdingid) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", orderRows);
        holdingRows.clear();
        orderRows.clear();
    }

    private boolean databaseIsPostgres() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgres");
        }
    }
}
