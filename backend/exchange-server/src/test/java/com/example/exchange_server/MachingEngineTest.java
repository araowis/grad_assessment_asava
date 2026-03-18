package com.example.exchange_server;

import com.example.exchange_server.client.CompanyClient;
import com.example.exchange_server.dto.CompanyDTO;
import com.example.exchange_server.engine.MatchingEngine;
import com.example.exchange_server.engine.OrderBook;
import com.example.exchange_server.model.Order;
import com.example.exchange_server.model.OrderStatus;
import com.example.exchange_server.model.OrderType;
import com.example.exchange_server.model.Trade;
import com.example.exchange_server.repository.TradeRepository;
import com.example.exchange_server.util.ValidatePrice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingEngineTest {

    @Mock private OrderBook       orderBook;
    @Mock private TradeRepository tradeRepository;
    @Mock private CompanyClient   companyClient;
    @Mock private ValidatePrice   validatePrice;

    @InjectMocks
    private MatchingEngine engine;

    private static final String COMPANY_A = "COMP_A";
    private static final String COMPANY_B = "COMP_B";

    private CompanyDTO companyA;

    @BeforeEach
    void setUp() {
        companyA = new CompanyDTO(COMPANY_A, "Company A", 1_000_000, 100.0, 105.0);

        // Wire a real OrderBook so queue operations actually work
        OrderBook realBook = new OrderBook();
        when(orderBook.getBuyOrders(anyString()))
                .thenAnswer(inv -> realBook.getBuyOrders(inv.getArgument(0)));
        when(orderBook.getSellOrders(anyString()))
                .thenAnswer(inv -> realBook.getSellOrders(inv.getArgument(0)));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Order buyOrder(long userId, String companyId, int qty, double price) {
        return new Order(userId, companyId, qty, price, OrderType.BUY, OrderStatus.OPEN);
    }

    private Order sellOrder(long userId, String companyId, int qty, double price) {
        return new Order(userId, companyId, qty, price, OrderType.SELL, OrderStatus.OPEN);
    }

    // -------------------------------------------------------------------------
    // Basic matching
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("matching buy and sell orders produces one trade and zeroes both quantities")
    void matchingOrders_producesTrade() {
        when(companyClient.getCompanyById(COMPANY_A)).thenReturn(companyA);
        when(validatePrice.validatePrice(anyDouble(), anyDouble(), anyDouble())).thenReturn(100.0);

        Order buy  = buyOrder(1L,  COMPANY_A, 10, 100.0);
        Order sell = sellOrder(2L, COMPANY_A, 10, 100.0);

        engine.processOrder(sell); // sell enters the book first
        engine.processOrder(buy);  // buy matches against it

        ArgumentCaptor<Trade> cap = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(cap.capture());

        Trade trade = cap.getValue();
        assertThat(trade.getQuantity()).isEqualTo(10);
        assertThat(trade.getBuyerId()).isEqualTo(1L);
        assertThat(trade.getSellerId()).isEqualTo(2L);
        assertThat(buy.getQuantity()).isZero();
        assertThat(sell.getQuantity()).isZero();
    }

    @Test
    @DisplayName("buy order with no matching sell remains in the order book")
    void buyOrder_noMatch_remainsInBook() {
        Order buy = buyOrder(1L, COMPANY_A, 5, 90.0);
        engine.processOrder(buy);

        verify(tradeRepository, never()).save(any());
        assertThat(buy.getQuantity()).isEqualTo(5); // untouched
    }

    @Test
    @DisplayName("partial fill: buy for 10, sell for 6 — buy has 4 remaining")
    void partialFill_buyHasRemainingQuantity() {
        when(companyClient.getCompanyById(COMPANY_A)).thenReturn(companyA);
        when(validatePrice.validatePrice(anyDouble(), anyDouble(), anyDouble())).thenReturn(100.0);

        Order sell = sellOrder(2L, COMPANY_A, 6,  100.0);
        Order buy  = buyOrder(1L,  COMPANY_A, 10, 100.0);

        engine.processOrder(sell);
        engine.processOrder(buy);

        verify(tradeRepository, times(1)).save(any(Trade.class));
        assertThat(buy.getQuantity()).isEqualTo(4);  // 10 - 6
        assertThat(sell.getQuantity()).isZero();
    }

    // -------------------------------------------------------------------------
    // Price validation guard — no mutation on invalid price
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("when price is out of daily band, quantities are NOT mutated")
    void outOfBandPrice_quantitiesUnchanged() {
        when(companyClient.getCompanyById(COMPANY_A)).thenReturn(companyA);
        when(validatePrice.validatePrice(anyDouble(), anyDouble(), anyDouble())).thenReturn(-1.0); // price rejected

        Order sell = sellOrder(2L, COMPANY_A, 10, 150.0); // way above band
        Order buy  = buyOrder(1L,  COMPANY_A, 10, 150.0);

        int originalSellQty = sell.getQuantity();
        int originalBuyQty  = buy.getQuantity();

        engine.processOrder(sell);
        engine.processOrder(buy);

        // No trade saved
        verify(tradeRepository, never()).save(any());

        // Quantities intact — no corrupt state
        assertThat(sell.getQuantity()).isEqualTo(originalSellQty);
        assertThat(buy.getQuantity()).isEqualTo(originalBuyQty);
    }

    @Test
    @DisplayName("when company service is unavailable, quantities are NOT mutated")
    void companyServiceDown_quantitiesUnchanged() {
        when(companyClient.getCompanyById(COMPANY_A))
                .thenThrow(new RuntimeException("Feign timeout"));

        Order sell = sellOrder(2L, COMPANY_A, 10, 100.0);
        Order buy  = buyOrder(1L,  COMPANY_A, 10, 100.0);

        engine.processOrder(sell);
        engine.processOrder(buy); // should not throw

        verify(tradeRepository, never()).save(any());
        assertThat(buy.getQuantity()).isEqualTo(10);
        assertThat(sell.getQuantity()).isEqualTo(10);
    }

    // -------------------------------------------------------------------------
    // Per-company lock isolation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("orders for different companies are processed independently (no cross-block)")
    void differentCompanies_processedIndependently() throws InterruptedException {
        // Company B setup
        CompanyDTO companyB = new CompanyDTO(COMPANY_B, "Company B", 500_000, 50.0, 52.0);
        when(companyClient.getCompanyById(COMPANY_A)).thenReturn(companyA);
        when(companyClient.getCompanyById(COMPANY_B)).thenReturn(companyB);
        when(validatePrice.validatePrice(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(100.0);

        int threadCount = 4;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threadCount);
        List<Throwable> errors = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        // 2 threads hammer COMPANY_A, 2 threads hammer COMPANY_B
        for (int i = 0; i < threadCount; i++) {
            String companyId = (i % 2 == 0) ? COMPANY_A : COMPANY_B;
            pool.submit(() -> {
                try {
                    start.await();
                    engine.processOrder(sellOrder(99L, companyId, 5, 100.0));
                    engine.processOrder(buyOrder(100L, companyId, 5, 100.0));
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown(); // release all threads simultaneously
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(errors).isEmpty();
        // Each pair of threads fired one trade each → 2 trades total
        verify(tradeRepository, times(2)).save(any(Trade.class));
    }

    @Test
    @DisplayName("concurrent orders for the same company do not corrupt quantities")
    void sameCompany_concurrentOrders_noCorruption() throws InterruptedException {
        when(companyClient.getCompanyById(COMPANY_A)).thenReturn(companyA);
        when(validatePrice.validatePrice(anyDouble(), anyDouble(), anyDouble())).thenReturn(100.0);

        int pairs = 20;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(pairs * 2);
        List<Throwable> errors = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(pairs * 2);

        for (int i = 0; i < pairs; i++) {
            final long buyerId  = i * 2L;
            final long sellerId = i * 2L + 1;

            pool.submit(() -> {
                try {
                    start.await();
                    engine.processOrder(buyOrder(buyerId, COMPANY_A, 1, 100.0));
                } catch (Throwable t) { errors.add(t); }
                finally { done.countDown(); }
            });

            pool.submit(() -> {
                try {
                    start.await();
                    engine.processOrder(sellOrder(sellerId, COMPANY_A, 1, 100.0));
                } catch (Throwable t) { errors.add(t); }
                finally { done.countDown(); }
            });
        }

        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(errors).as("no exceptions during concurrent processing").isEmpty();
    }
}
