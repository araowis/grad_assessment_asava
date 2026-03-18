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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingEngineTest {

    @Mock private OrderBook orderBook;
    @Mock private TradeRepository tradeRepository;
    @Mock private CompanyClient companyClient;
    @Mock private ValidatePrice validatePrice;

    @InjectMocks
    private MatchingEngine matchingEngine;

    // BUY queue: highest price first (max-heap)
    private PriorityQueue<Order> buyQueue;
    // SELL queue: lowest price first (min-heap)
    private PriorityQueue<Order> sellQueue;

    private CompanyDTO sampleCompany;

    @BeforeEach
    void setUp() {
        // BUY orders: highest bid first
        buyQueue  = new PriorityQueue<>((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
        // SELL orders: lowest ask first
        sellQueue = new PriorityQueue<>((a, b) -> Double.compare(a.getPrice(), b.getPrice()));

        sampleCompany = new CompanyDTO("TCS", "Tata Consultancy Services", 1000, 100.0, 105.0);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Order makeBuyOrder(Long userId, double price, int qty) {
        Order o = new Order();
        o.setId(userId); // reuse userId as id for simplicity in tests
        o.setUserId(userId);
        o.setCompanyId("TCS");
        o.setPrice(price);
        o.setQuantity(qty);
        o.setType(OrderType.BUY);
        o.setStatus(OrderStatus.OPEN);
        return o;
    }

    private Order makeSellOrder(Long userId, double price, int qty) {
        Order o = new Order();
        o.setId(userId);
        o.setUserId(userId);
        o.setCompanyId("TCS");
        o.setPrice(price);
        o.setQuantity(qty);
        o.setType(OrderType.SELL);
        o.setStatus(OrderStatus.OPEN);
        return o;
    }

    private void stubCompanyAndValidatePrice(double validPrice) {
        when(companyClient.getCompanyById("TCS")).thenReturn(sampleCompany);
        when(validatePrice.validatePrice(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(validPrice);
        doNothing().when(companyClient).updatePrice(anyString(), anyDouble());
    }

    // ─── BUY order processing ─────────────────────────────────────────────────

    @Test
    void processOrder_BuyOrder_ShouldAddToQueue_WhenNoMatchingSellOrder() {
        when(orderBook.getBuyOrders("TCS")).thenReturn(buyQueue);
        when(orderBook.getSellOrders("TCS")).thenReturn(sellQueue); // empty

        Order buyOrder = makeBuyOrder(1L, 100.0, 10);
        matchingEngine.processOrder(buyOrder);

        // No match → full quantity remains → order added to buy queue
        assertEquals(1, buyQueue.size());
        verify(tradeRepository, never()).save(any());
    }

    @Test
    void processOrder_BuyOrder_ShouldMatch_WhenSellPriceIsLowerOrEqual() {
        Order existingSell = makeSellOrder(2L, 95.0, 10);
        sellQueue.add(existingSell);

        when(orderBook.getBuyOrders("TCS")).thenReturn(buyQueue);
        when(orderBook.getSellOrders("TCS")).thenReturn(sellQueue);
        stubCompanyAndValidatePrice(95.0);

        Order buyOrder = makeBuyOrder(1L, 100.0, 10); // buy@100 >= sell@95 → match
        matchingEngine.processOrder(buyOrder);

        // Full match → trade saved, both quantities consumed
        verify(tradeRepository, times(1)).save(any(Trade.class));
        assertEquals(0, buyOrder.getQuantity());
        assertEquals(0, existingSell.getQuantity());
    }

    @Test
    void processOrder_BuyOrder_ShouldNotMatch_WhenSellPriceIsTooHigh() {
        Order existingSell = makeSellOrder(2L, 110.0, 10);
        sellQueue.add(existingSell);

        when(orderBook.getBuyOrders("TCS")).thenReturn(buyQueue);
        when(orderBook.getSellOrders("TCS")).thenReturn(sellQueue);

        Order buyOrder = makeBuyOrder(1L, 100.0, 10); // buy@100 < sell@110 → no match
        matchingEngine.processOrder(buyOrder);

        verify(tradeRepository, never()).save(any());
        // Unmatched buy order goes into queue
        assertEquals(1, buyQueue.size());
    }

    @Test
    void processOrder_BuyOrder_ShouldPartiallyMatch_WhenSellQuantityIsSmaller() {
        Order existingSell = makeSellOrder(2L, 95.0, 4); // sell only 4
        sellQueue.add(existingSell);

        when(orderBook.getBuyOrders("TCS")).thenReturn(buyQueue);
        when(orderBook.getSellOrders("TCS")).thenReturn(sellQueue);
        stubCompanyAndValidatePrice(95.0);

        Order buyOrder = makeBuyOrder(1L, 100.0, 10); // buy 10
        matchingEngine.processOrder(buyOrder);

        // 4 traded, 6 remain → buy order stays in queue with qty=6
        verify(tradeRepository, times(1)).save(any(Trade.class));
        assertEquals(6, buyOrder.getQuantity());
        assertEquals(0, existingSell.getQuantity());
        assertEquals(1, buyQueue.size()); // remaining buy stays in queue
    }

    // ─── SELL order processing ────────────────────────────────────────────────

    @Test
    void processOrder_SellOrder_ShouldAddToQueue_WhenNoMatchingBuyOrder() {
        when(orderBook.getBuyOrders("TCS")).thenReturn(buyQueue); // empty
        when(orderBook.getSellOrders("TCS")).thenReturn(sellQueue);

        Order sellOrder = makeSellOrder(1L, 100.0, 10);
        matchingEngine.processOrder(sellOrder);

        assertEquals(1, sellQueue.size());
        verify(tradeRepository, never()).save(any());
    }

    @Test
    void processOrder_SellOrder_ShouldMatch_WhenBuyPriceIsHigherOrEqual() {
        Order existingBuy = makeBuyOrder(1L, 105.0, 10);
        buyQueue.add(existingBuy);

        when(orderBook.getBuyOrders("TCS")).thenReturn(buyQueue);
        when(orderBook.getSellOrders("TCS")).thenReturn(sellQueue);
        stubCompanyAndValidatePrice(100.0);

        Order sellOrder = makeSellOrder(2L, 100.0, 10); // sell@100 <= buy@105 → match
        matchingEngine.processOrder(sellOrder);

        verify(tradeRepository, times(1)).save(any(Trade.class));
        assertEquals(0, sellOrder.getQuantity());
        assertEquals(0, existingBuy.getQuantity());
    }

    @Test
    void processOrder_SellOrder_ShouldNotMatch_WhenBuyPriceIsTooLow() {
        Order existingBuy = makeBuyOrder(1L, 90.0, 10);
        buyQueue.add(existingBuy);

        when(orderBook.getBuyOrders("TCS")).thenReturn(buyQueue);
        when(orderBook.getSellOrders("TCS")).thenReturn(sellQueue);

        Order sellOrder = makeSellOrder(2L, 100.0, 10); // sell@100 > buy@90 → no match
        matchingEngine.processOrder(sellOrder);

        verify(tradeRepository, never()).save(any());
        assertEquals(1, sellQueue.size());
    }

    // ─── Trade creation correctness ───────────────────────────────────────────

    @Test
    void executeTrade_ShouldSaveTrade_WithCorrectBuyerSellerAndQuantity() {
        Order existingSell = makeSellOrder(99L, 95.0, 10);
        sellQueue.add(existingSell);

        when(orderBook.getBuyOrders("TCS")).thenReturn(buyQueue);
        when(orderBook.getSellOrders("TCS")).thenReturn(sellQueue);
        stubCompanyAndValidatePrice(95.0);

        Order buyOrder = makeBuyOrder(42L, 100.0, 10);
        matchingEngine.processOrder(buyOrder);

        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(tradeCaptor.capture());

        Trade savedTrade = tradeCaptor.getValue();
        assertEquals(42L, savedTrade.getBuyerId());
        assertEquals(99L, savedTrade.getSellerId());
        assertEquals("TCS", savedTrade.getCompanyId());
        assertEquals(10, savedTrade.getQuantity());
        assertEquals(95.0, savedTrade.getPrice()); // trade at validated sell price
        assertNotNull(savedTrade.getExecutedAt());
    }

    @Test
    void executeTrade_ShouldUseValidatedPrice_NotRawTradePrice() {
        Order existingSell = makeSellOrder(2L, 95.0, 10);
        sellQueue.add(existingSell);

        when(orderBook.getBuyOrders("TCS")).thenReturn(buyQueue);
        when(orderBook.getSellOrders("TCS")).thenReturn(sellQueue);
        // ValidatePrice returns a different (adjusted) price
        when(companyClient.getCompanyById("TCS")).thenReturn(sampleCompany);
        when(validatePrice.validatePrice(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(96.5); // adjusted price
        doNothing().when(companyClient).updatePrice(anyString(), anyDouble());

        Order buyOrder = makeBuyOrder(1L, 100.0, 10);
        matchingEngine.processOrder(buyOrder);

        ArgumentCaptor<Trade> captor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(captor.capture());

        // Trade must use the validated price, not the raw sell price
        assertEquals(96.5, captor.getValue().getPrice());
    }

    @Test
    void executeTrade_ShouldUpdateCompanyPrice_AfterEachTrade() {
        Order existingSell = makeSellOrder(2L, 95.0, 10);
        sellQueue.add(existingSell);

        when(orderBook.getBuyOrders("TCS")).thenReturn(buyQueue);
        when(orderBook.getSellOrders("TCS")).thenReturn(sellQueue);
        stubCompanyAndValidatePrice(95.0);

        Order buyOrder = makeBuyOrder(1L, 100.0, 10);
        matchingEngine.processOrder(buyOrder);

        // Company price must be updated after every trade
        verify(companyClient).updatePrice("TCS", 95.0);
    }

    @Test
    void executeTrade_ShouldRemoveFullyMatchedSellOrder_FromQueue() {
        Order existingSell = makeSellOrder(2L, 95.0, 10);
        sellQueue.add(existingSell);

        when(orderBook.getBuyOrders("TCS")).thenReturn(buyQueue);
        when(orderBook.getSellOrders("TCS")).thenReturn(sellQueue);
        stubCompanyAndValidatePrice(95.0);

        Order buyOrder = makeBuyOrder(1L, 100.0, 10);
        matchingEngine.processOrder(buyOrder);

        // Fully matched sell order must be polled out of the queue
        assertTrue(sellQueue.isEmpty(),
                "Fully matched sell order must be removed from sell queue");
    }

    @Test
    void executeTrade_ShouldRemoveFullyMatchedBuyOrder_FromQueue() {
        Order existingBuy = makeBuyOrder(1L, 105.0, 10);
        buyQueue.add(existingBuy);

        when(orderBook.getBuyOrders("TCS")).thenReturn(buyQueue);
        when(orderBook.getSellOrders("TCS")).thenReturn(sellQueue);
        stubCompanyAndValidatePrice(100.0);

        Order sellOrder = makeSellOrder(2L, 100.0, 10);
        matchingEngine.processOrder(sellOrder);

        assertTrue(buyQueue.isEmpty(),
                "Fully matched buy order must be removed from buy queue");
    }
}