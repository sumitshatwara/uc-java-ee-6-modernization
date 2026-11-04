package io.daytrader.service;

import io.daytrader.MarketSummaryDataBean;
import io.daytrader.config.TradeConfig;
import io.daytrader.entity.AccountDataBean;
import io.daytrader.entity.AccountProfileDataBean;
import io.daytrader.entity.HoldingDataBean;
import io.daytrader.entity.OrderDataBean;
import io.daytrader.entity.QuoteDataBean;
import io.daytrader.repository.AccountProfileRepository;
import io.daytrader.repository.AccountRepository;
import io.daytrader.repository.HoldingRepository;
import io.daytrader.repository.OrderRepository;
import io.daytrader.repository.QuoteRepository;
import io.daytrader.util.FinancialUtils;
import io.daytrader.util.Log;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TradeService {
    private static final Object MARKET_SUMMARY_LOCK = new Object();
    private static long nextMarketSummary = System.currentTimeMillis();
    private static volatile MarketSummaryDataBean cachedMarketSummary = MarketSummaryDataBean.getRandomInstance();

    private final AccountProfileRepository profiles;
    private final AccountRepository accounts;
    private final HoldingRepository holdings;
    private final OrderRepository orders;
    private final QuoteRepository quotes;
    private final ObjectProvider<OrderQueue> orderQueue;

    public TradeService(AccountProfileRepository profiles, AccountRepository accounts,
                        HoldingRepository holdings, OrderRepository orders, QuoteRepository quotes,
                        ObjectProvider<OrderQueue> orderQueue) {
        this.profiles = profiles;
        this.accounts = accounts;
        this.holdings = holdings;
        this.orders = orders;
        this.quotes = quotes;
        this.orderQueue = orderQueue;
    }

    @Transactional(readOnly = true)
    public MarketSummaryDataBean getMarketSummary() {
        int interval = TradeConfig.getMarketSummaryIntervalValue();
        if (interval == 0) return getMarketSummaryInternal();
        if (interval < 0) return cachedMarketSummary;
        long currentTime = System.currentTimeMillis();
        if (currentTime > nextMarketSummary) {
            long oldNextMarketSummary = nextMarketSummary;
            boolean fetch = false;
            synchronized (MARKET_SUMMARY_LOCK) {
                if (oldNextMarketSummary == nextMarketSummary) {
                    fetch = true;
                    nextMarketSummary += interval * 1000L;
                    if (nextMarketSummary < currentTime) nextMarketSummary = currentTime + interval * 1000L;
                }
            }
            if (fetch) cachedMarketSummary = getMarketSummaryInternal();
        }
        return cachedMarketSummary;
    }

    @Transactional(readOnly = true)
    public MarketSummaryDataBean getMarketSummaryInternal() {
        List<QuoteDataBean> quoteList = quotes.findQuotesByChange();
        QuoteDataBean[] quoteArray = quoteList.toArray(new QuoteDataBean[0]);
        List<QuoteDataBean> gainers = new ArrayList<>(5);
        List<QuoteDataBean> losers = new ArrayList<>(5);
        BigDecimal tsia = FinancialUtils.ZERO;
        BigDecimal openTSIA = FinancialUtils.ZERO;
        double totalVolume = 0;
        if (quoteArray.length > 5) {
            for (int index = 0; index < 5; index++) gainers.add(quoteArray[index]);
            for (int index = quoteArray.length - 1; index >= quoteArray.length - 5; index--) {
                losers.add(quoteArray[index]);
            }
            for (QuoteDataBean quote : quoteArray) {
                tsia = tsia.add(quote.getPrice());
                openTSIA = openTSIA.add(quote.getOpen());
                totalVolume += quote.getVolume();
            }
            tsia = tsia.divide(new BigDecimal(quoteArray.length), FinancialUtils.ROUND);
            openTSIA = openTSIA.divide(new BigDecimal(quoteArray.length), FinancialUtils.ROUND);
        }
        return new MarketSummaryDataBean(tsia, openTSIA, totalVolume, gainers, losers);
    }

    public OrderDataBean buy(String userID, String symbol, double quantity, int orderProcessingMode) {
        AccountDataBean account = accountFor(userID);
        QuoteDataBean quote = quoteFor(symbol);
        OrderDataBean order = createOrder(account, quote, null, "buy", quantity);
        BigDecimal total = new BigDecimal(quantity).multiply(quote.getPrice()).add(order.getOrderFee());
        account.setBalance(account.getBalance().subtract(total));
        dispatch(order, orderProcessingMode);
        return order;
    }

    public OrderDataBean sell(String userID, Integer holdingID, int orderProcessingMode) {
        AccountDataBean account = accountFor(userID);
        HoldingDataBean holding = holdings.findById(holdingID).orElse(null);
        if (holding == null) {
            OrderDataBean cancelled = new OrderDataBean();
            cancelled.setOrderStatus("cancelled");
            return orders.save(cancelled);
        }
        QuoteDataBean quote = holding.getQuote();
        OrderDataBean order = createOrder(account, quote, holding, "sell", holding.getQuantity());
        holding.setPurchaseDate(new Timestamp(0));
        BigDecimal total = new BigDecimal(holding.getQuantity()).multiply(quote.getPrice()).subtract(order.getOrderFee());
        account.setBalance(account.getBalance().add(total));
        dispatch(order, orderProcessingMode);
        return order;
    }

    private void dispatch(OrderDataBean order, int mode) {
        if (mode == TradeConfig.SYNCH) {
            completeOrder(order.getOrderID(), false);
        } else if (mode == TradeConfig.ASYNCH_2PHASE) {
            OrderQueue queue = orderQueue.getIfAvailable();
            if (queue == null) {
                Log.error("No OrderQueue bean is configured; processing the order synchronously.");
                completeOrder(order.getOrderID(), false);
            } else {
                queue.submit(order.getOrderID(), true);
            }
        }
    }

    public OrderDataBean processOrder(Integer orderID, boolean twoPhase) throws Exception {
        return completeOrder(orderID, twoPhase);
    }

    public OrderDataBean completeOrder(Integer orderID, boolean twoPhase) {
        OrderDataBean order = orders.findById(orderID).orElse(null);
        if (order == null) return null;
        if (order.isCompleted()) throw new IllegalStateException("Order is already completed: " + orderID);
        if (order.isBuy()) {
            HoldingDataBean holding = createHolding(order.getAccount(), order.getQuote(),
                    order.getQuantity(), order.getPrice());
            order.setHolding(holding);
        } else if (order.isSell()) {
            HoldingDataBean holding = order.getHolding();
            if (holding == null) {
                order.cancel();
                return order;
            }
            holdings.delete(holding);
            order.setHolding(null);
        }
        order.setOrderStatus("closed");
        order.setCompletionDate(new Timestamp(System.currentTimeMillis()));
        return orders.save(order);
    }

    public void cancelOrder(Integer orderID, boolean twoPhase) {
        orders.findById(orderID).ifPresent(OrderDataBean::cancel);
    }

    @Transactional(readOnly = true)
    public Collection<OrderDataBean> getOrders(String userID) {
        return orders.findByAccount_Profile_UserIDOrderByOpenDateDesc(userID);
    }

    public Collection<OrderDataBean> getClosedOrders(String userID) {
        Collection<OrderDataBean> closedOrders =
                orders.findByAccount_Profile_UserIDAndOrderStatusOrderByOpenDateDesc(userID, "closed");
        for (OrderDataBean order : closedOrders) {
            order.getQuote();
            order.setOrderStatus("completed");
        }
        return closedOrders;
    }

    public QuoteDataBean createQuote(String symbol, String companyName, BigDecimal price) {
        QuoteDataBean quote = new QuoteDataBean(symbol, companyName, 0, price, price, price, price, 0);
        return quotes.save(quote);
    }

    @Transactional(readOnly = true)
    public QuoteDataBean getQuote(String symbol) {
        return quotes.findById(symbol).orElse(null);
    }

    @Transactional(readOnly = true)
    public Collection<QuoteDataBean> getAllQuotes() {
        return quotes.findAll();
    }

    public QuoteDataBean updateQuotePriceVolume(String symbol, BigDecimal changeFactor, double sharesTraded) {
        QuoteDataBean quote = quoteFor(symbol);
        BigDecimal oldPrice = quote.getPrice();
        if (oldPrice.compareTo(TradeConfig.PENNY_STOCK_PRICE) == 0) {
            changeFactor = TradeConfig.PENNY_STOCK_RECOVERY_MIRACLE_MULTIPLIER;
        }
        BigDecimal newPrice = oldPrice.multiply(changeFactor).setScale(FinancialUtils.SCALE, FinancialUtils.ROUND);
        quote.setPrice(newPrice);
        quote.setVolume(quote.getVolume() + sharesTraded);
        quote.setChange(newPrice.subtract(oldPrice).doubleValue());
        return quotes.save(quote);
    }

    @Transactional(readOnly = true)
    public Collection<HoldingDataBean> getHoldings(String userID) {
        return holdings.findByAccount_Profile_UserID(userID);
    }

    @Transactional(readOnly = true)
    public HoldingDataBean getHolding(Integer holdingID) {
        return holdings.findById(holdingID).orElse(null);
    }

    @Transactional(readOnly = true)
    public AccountDataBean getAccountData(String userID) {
        return profiles.findById(userID).map(AccountProfileDataBean::getAccount).orElse(null);
    }

    @Transactional(readOnly = true)
    public AccountProfileDataBean getAccountProfileData(String userID) {
        return profiles.findById(userID).orElse(null);
    }

    public AccountProfileDataBean updateAccountProfile(AccountProfileDataBean profileData) {
        AccountProfileDataBean profile = profiles.findById(profileData.getUserID())
                .orElseThrow(() -> new IllegalArgumentException("No such user: " + profileData.getUserID()));
        profile.setAddress(profileData.getAddress());
        profile.setPassword(profileData.getPassword());
        profile.setFullName(profileData.getFullName());
        profile.setCreditCard(profileData.getCreditCard());
        profile.setEmail(profileData.getEmail());
        return profiles.save(profile);
    }

    public AccountDataBean login(String userID, String password) {
        AccountProfileDataBean profile = profiles.findById(userID)
                .orElseThrow(() -> new IllegalArgumentException("No such user: " + userID));
        AccountDataBean account = profile.getAccount();
        account.login(password);
        return account;
    }

    public void logout(String userID) {
        AccountDataBean account = accountFor(userID);
        account.logout();
    }

    public AccountDataBean register(String userID, String password, String fullName, String address,
                                    String email, String creditCard, BigDecimal openBalance) {
        if (profiles.existsById(userID)) return null;
        AccountProfileDataBean profile = new AccountProfileDataBean(userID, password, fullName,
                address, email, creditCard);
        AccountDataBean account = new AccountDataBean(0, 0, null, new Timestamp(System.currentTimeMillis()),
                openBalance, openBalance, userID);
        profile.setAccount(account);
        account.setProfile(profile);
        return accounts.save(account);
    }

    public void resetTrade(boolean deleteAll) {
        orders.deleteAllInBatch();
        holdings.deleteAllInBatch();
        accounts.deleteAllInBatch();
        profiles.deleteAllInBatch();
        if (deleteAll) quotes.deleteAllInBatch();
    }

    public double investmentReturn(double investment, double netValue) {
        return (netValue - investment) / investment;
    }

    private AccountDataBean accountFor(String userID) {
        return profiles.findById(userID).map(AccountProfileDataBean::getAccount)
                .orElseThrow(() -> new IllegalArgumentException("No such user: " + userID));
    }

    private QuoteDataBean quoteFor(String symbol) {
        return quotes.findById(symbol).orElseThrow(() -> new IllegalArgumentException("No such quote: " + symbol));
    }

    private OrderDataBean createOrder(AccountDataBean account, QuoteDataBean quote, HoldingDataBean holding,
                                      String orderType, double quantity) {
        OrderDataBean order = new OrderDataBean(orderType, "open", new Timestamp(System.currentTimeMillis()),
                null, quantity, quote.getPrice().setScale(FinancialUtils.SCALE, FinancialUtils.ROUND),
                TradeConfig.getOrderFee(orderType), account, quote, holding);
        return orders.save(order);
    }

    private HoldingDataBean createHolding(AccountDataBean account, QuoteDataBean quote,
                                          double quantity, BigDecimal purchasePrice) {
        HoldingDataBean holding = new HoldingDataBean(quantity, purchasePrice,
                new Timestamp(System.currentTimeMillis()), account, quote);
        return holdings.save(holding);
    }
}
