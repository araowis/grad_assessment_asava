package com.example.exchange_server.engine;

import java.time.LocalDateTime;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
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

    // One lock per company: orders for different companies never block each other
    private final ConcurrentHashMap<String, ReentrantLock> companyLocks = new ConcurrentHashMap<>();

    public MatchingEngine(OrderBook orderBook,
                          TradeRepository tradeRepository,
                          CompanyClient companyClient,
                          ValidatePrice validatePrice) {
        this.orderBook = orderBook;
        this.tradeRepository = tradeRepository;
        this.companyClient = companyClient;
        this.validatePrice = validatePrice;
    }

    //  Entry point. Acquires a per-company lock so orders for different companies
    //  run concurrently, while orders for the same company are still serialised
    public void processOrder(Order order) {
        ReentrantLock lock = companyLocks.computeIfAbsent(
                order.getCompanyId(), k -> new ReentrantLock());

        lock.lock();
        try {
            PriorityQueue<Order> buyQueue  = orderBook.getBuyOrders(order.getCompanyId());
            PriorityQueue<Order> sellQueue = orderBook.getSellOrders(order.getCompanyId());

            if (order.getType() == OrderType.BUY) {
                matchBuyOrder(order, sellQueue);
                if (order.getQuantity() > 0) {
                    buyQueue.add(order);
                }
            } else {
                matchSellOrder(order, buyQueue);
                if (order.getQuantity() > 0) {
                    sellQueue.add(order);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    // Matching logic — pure in-memory
    private void matchBuyOrder(Order buyOrder, PriorityQueue<Order> sellQueue) {
        while (!sellQueue.isEmpty() && buyOrder.getQuantity() > 0) {
            Order sellOrder = sellQueue.peek();
            if (sellOrder.getPrice() <= buyOrder.getPrice()) {
                executeTrade(buyOrder, sellOrder);
                if (sellOrder.getQuantity() == 0) {
                    sellQueue.poll();
                }
            } else {
                break;
            }
        }
    }

    private void matchSellOrder(Order sellOrder, PriorityQueue<Order> buyQueue) {
        while (!buyQueue.isEmpty() && sellOrder.getQuantity() > 0) {
            Order buyOrder = buyQueue.peek();
            if (buyOrder.getPrice() >= sellOrder.getPrice()) {
                executeTrade(buyOrder, sellOrder);
                if (buyOrder.getQuantity() == 0) {
                    buyQueue.poll();
                }
            } else {
                break;
            }
        }
    }

    /**
     * Validate price BEFORE mutating quantities.
     *
     * If validation fails we skip this trade entirely — the order book is
     * untouched and no corrupt state is produced. The exception is caught here
     * so a bad price never propagates up and kills the whole match loop.
     *
     * After a successful match the quantities are updated in-memory (still
     * inside the lock), then the expensive I/O work (Feign + DB write) is
     * dispatched to an async thread so it never blocks order processing.
     */
    private void executeTrade(Order buy, Order sell) {
        int qty        = Math.min(buy.getQuantity(), sell.getQuantity());
        double tradePrice = sell.getPrice();

        //Validate BEFORE any mutation
        double validPrice;
        try {
            CompanyDTO company = companyClient.getCompanyById(buy.getCompanyId());
            var maybePrice = validatePrice.validatePrice(
                    company.getCurrentPrice(),
                    tradePrice,
                    company.getOpeningPrice());

            if (maybePrice <= 0) {
                log.warn("Trade skipped for company={}: tradePrice={} is outside the daily band",
                        buy.getCompanyId(), tradePrice);
                return;
            }
            validPrice = maybePrice;
        } catch (Exception e) {
            // Company service unavailable, skip rather than corrupt the order book.
            log.error("Trade skipped for company={}: could not fetch company data — {}",
                    buy.getCompanyId(), e.getMessage());
            return;
        }

        // Mutate quantities only after validation passes
        buy.setQuantity(buy.getQuantity() - qty);
        sell.setQuantity(sell.getQuantity() - qty);

        // Build the trade record
        Trade trade = new Trade();
        trade.setBuyerId(buy.getUserId());
        trade.setSellerId(sell.getUserId());
        trade.setCompanyId(buy.getCompanyId());
        trade.setQuantity(qty);
        trade.setPrice(validPrice);
        trade.setExecutedAt(LocalDateTime.now());

        //Dispatch all I/O off the lock thread
        persistAndNotify(trade, buy.getCompanyId(), validPrice);
    }

    /**
     * Runs on the async executor configured in AsyncConfig.
     * DB write + Feign price update happen here, off the matching-engine thread.
     */
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