package com.example.exchange_server.engine;

import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.exchange_server.client.CompanyClient;
import com.example.exchange_server.dto.CompanyDTO;
import com.example.exchange_server.model.Order;
import com.example.exchange_server.model.OrderType;
import com.example.exchange_server.model.Trade;
import com.example.exchange_server.repository.TradeRepository;
import com.example.exchange_server.util.ValidatePrice;

@Service
public class MatchingEngine {

    private static final Logger log = LoggerFactory.getLogger(MatchingEngine.class);

    private final OrderBook orderBook;
    private final TradeRepository tradeRepository;
    private final CompanyClient companyClient;
    private final ValidatePrice validatePrice;

    public MatchingEngine(OrderBook orderBook,
                          TradeRepository tradeRepository,
                          CompanyClient companyClient,
                          ValidatePrice validatePrice) {
        this.orderBook = orderBook;
        this.tradeRepository = tradeRepository;
        this.companyClient = companyClient;
        this.validatePrice = validatePrice;
    }

    /**
     * Acquires the per-company lock (owned by OrderBook, the single source of truth)
     * then runs the matching loop. Orders for different companies never block each other.
     */
    public void processOrder(Order order) {
        ReentrantLock lock = orderBook.lockFor(order.getCompanyId());
        lock.lock();
        try {
            if (order.getType() == OrderType.BUY) {
                matchBuyOrder(order);
                if (order.getQuantity() > 0) {
                    orderBook.addBuy(order);
                }
            } else {
                matchSellOrder(order);
                if (order.getQuantity() > 0) {
                    orderBook.addSell(order);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    // -------------------------------------------------------------------------
    // Matching loops — all queue access through OrderBook's API, inside the lock
    // -------------------------------------------------------------------------

    private void matchBuyOrder(Order buyOrder) {
        String companyId = buyOrder.getCompanyId();
        while (orderBook.hasSells(companyId) && buyOrder.getQuantity() > 0) {
            Order sellOrder = orderBook.peekBestSell(companyId);
            if (sellOrder.getPrice() <= buyOrder.getPrice()) {
                executeTrade(buyOrder, sellOrder);
                if (sellOrder.getQuantity() == 0) {
                    orderBook.pollBestSell(companyId);
                }
            } else {
                break;
            }
        }
    }

    private void matchSellOrder(Order sellOrder) {
        String companyId = sellOrder.getCompanyId();
        while (orderBook.hasBuys(companyId) && sellOrder.getQuantity() > 0) {
            Order buyOrder = orderBook.peekBestBuy(companyId);
            if (buyOrder.getPrice() >= sellOrder.getPrice()) {
                executeTrade(buyOrder, sellOrder);
                if (buyOrder.getQuantity() == 0) {
                    orderBook.pollBestBuy(companyId);
                }
            } else {
                break;
            }
        }
    }

    // Trade execution
    private void executeTrade(Order buy, Order sell) {
        int qty = Math.min(buy.getQuantity(), sell.getQuantity());
        double tradePrice = sell.getPrice();

        // validate
        double validPrice;
        try {
            CompanyDTO company = companyClient.getCompanyById(buy.getCompanyId());
            var maybePrice = validatePrice.validatePrice(
                    company.getCurrentPrice(),
                    tradePrice,
                    company.getOpeningPrice());

            if (maybePrice <= 0) {
                log.warn("Trade skipped for company={}: tradePrice={} failed validation",
                        buy.getCompanyId(), tradePrice);
                return;
            }
            validPrice = maybePrice;
        } catch (Exception e) {
            log.error("Trade skipped for company={}: could not fetch company data — {}",
                    buy.getCompanyId(), e.getMessage());
            return;
        }

        buy.setQuantity(buy.getQuantity() - qty);
        sell.setQuantity(sell.getQuantity() - qty);

        // Trade record
        Trade trade = new Trade();
        trade.setBuyerId(buy.getUserId());
        trade.setSellerId(sell.getUserId());
        trade.setCompanyId(buy.getCompanyId());
        trade.setQuantity(qty);
        trade.setPrice(validPrice);
        trade.setExecutedAt(LocalDateTime.now());

        // 4. All I/O dispatched off the lock thread.
        persistAndNotify(trade, buy.getCompanyId(), validPrice);
    }

    @Async
    public void persistAndNotify(Trade trade, String companyId, double validPrice) {
        try {
            tradeRepository.save(trade);
        } catch (Exception e) {
            log.error("Failed to persist trade for company={}: {}", companyId, e.getMessage(), e);
        }
        try {
            companyClient.updatePrice(companyId, validPrice);
        } catch (Exception e) {
            log.error("Failed to update price for company={}: {}", companyId, e.getMessage(), e);
        }
    }
}