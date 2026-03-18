package com.example.exchange_server;

import com.example.exchange_server.dto.OrderRequestDTO;
import com.example.exchange_server.engine.MatchingEngine;
import com.example.exchange_server.model.Order;
import com.example.exchange_server.model.OrderStatus;
import com.example.exchange_server.model.OrderType;
import com.example.exchange_server.repository.OrderRepository;
import com.example.exchange_server.service.OrderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private MatchingEngine matchingEngine;

    @InjectMocks
    private OrderService orderService;

    private OrderRequestDTO buyDTO;
    private OrderRequestDTO sellDTO;

    @BeforeEach
    void setUp() {
        buyDTO = new OrderRequestDTO();
        buyDTO.setUserId(1L);
        buyDTO.setCompanyId("TCS");
        buyDTO.setQuantity(10);
        buyDTO.setPrice(100.0);
        buyDTO.setType(OrderType.BUY);

        sellDTO = new OrderRequestDTO();
        sellDTO.setUserId(2L);
        sellDTO.setCompanyId("TCS");
        sellDTO.setQuantity(5);
        sellDTO.setPrice(98.0);
        sellDTO.setType(OrderType.SELL);
    }

    // ─── placeOrder ───────────────────────────────────────────────────────────

    @Test
    void placeOrder_ShouldSaveOrder_WithStatusOpen() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.placeOrder(buyDTO);

        // Capture the order that was actually saved
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());

        Order saved = captor.getValue();
        assertEquals(OrderStatus.OPEN, saved.getStatus(),
                "Newly placed order must always start as OPEN");
    }

    @Test
    void placeOrder_ShouldMapAllFieldsFromDTO_ToOrder() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.placeOrder(buyDTO);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());

        Order saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals("TCS", saved.getCompanyId());
        assertEquals(10, saved.getQuantity());
        assertEquals(100.0, saved.getPrice());
        assertEquals(OrderType.BUY, saved.getType());
    }

    @Test
    void placeOrder_ShouldCallMatchingEngine_AfterSavingOrder() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.placeOrder(buyDTO);

        // Save must happen BEFORE matching — verify order of calls
        InOrder inOrder = inOrder(orderRepository, matchingEngine);
        inOrder.verify(orderRepository).save(any(Order.class));
        inOrder.verify(matchingEngine).processOrder(any(Order.class));
    }

    @Test
    void placeOrder_ShouldWork_ForSellOrder() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.placeOrder(sellDTO);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());

        Order saved = captor.getValue();
        assertEquals(OrderType.SELL, saved.getType());
        assertEquals(OrderStatus.OPEN, saved.getStatus());
        assertEquals(2L, saved.getUserId());
    }

    @Test
    void placeOrder_ShouldNeverSetStatus_ToMatchedOrCancelled_OnCreation() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.placeOrder(buyDTO);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());

        Order saved = captor.getValue();
        assertNotEquals(OrderStatus.MATCHED, saved.getStatus());
        assertNotEquals(OrderStatus.CANCELLED, saved.getStatus());
    }
}