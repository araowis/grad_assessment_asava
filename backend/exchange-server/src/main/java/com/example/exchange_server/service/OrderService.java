package com.example.exchange_server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.exchange_server.dto.OrderRequestDTO;
import com.example.exchange_server.engine.MatchingEngine;
import com.example.exchange_server.model.Order;
import com.example.exchange_server.model.OrderStatus;
import com.example.exchange_server.repository.OrderRepository;

@Service
public class OrderService {

    @Autowired
    private MatchingEngine matchingEngine;

    @Autowired
    private OrderRepository orderRepository;

    public void placeOrder(OrderRequestDTO dto) {

        Order order = new Order();
        order.setUserId(dto.getUserId());
        order.setCompanyId(dto.getCompanyId());
        order.setQuantity(dto.getQuantity());
        order.setPrice(dto.getPrice());
        order.setType(dto.getType());
        order.setStatus(OrderStatus.OPEN);

        orderRepository.save(order);

        matchingEngine.processOrder(order);
    }
}
