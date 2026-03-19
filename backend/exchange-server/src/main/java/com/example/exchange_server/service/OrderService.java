package com.example.exchange_server.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.exchange_server.dto.OrderRequestDTO;
import com.example.exchange_server.dto.OrderResponseDTO;
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

    public OrderResponseDTO placeOrder(OrderRequestDTO dto) {
        Order order = new Order();
        order.setUserId(dto.getUserId());
        order.setCompanyId(dto.getCompanyId());
        order.setQuantity(dto.getQuantity());
        order.setPrice(dto.getPrice());

        // 🟢 Ensure you map the string/enum correctly
        order.setType(dto.getType());
        order.setStatus(OrderStatus.OPEN);

        // 🟢 CRITICAL: Capture the result of .save()
        Order savedOrder = orderRepository.save(order);

        // Process matching
        matchingEngine.processOrder(savedOrder);

        // 🟢 Return the savedOrder values, which now contain the Generated ID and Timestamp
        return OrderResponseDTO.builder()
                .id(savedOrder.getId())
                .status(savedOrder.getStatus())
                .price(savedOrder.getPrice())
                .type(savedOrder.getType())
                .createdAt(savedOrder.getCreatedAt())
                .build();
    }

    public List<OrderResponseDTO> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

// Helper method to map Entity to DTO
    private OrderResponseDTO mapToResponseDTO(Order order) {
        return OrderResponseDTO.builder()
                .userId(order.getUserId())
                .companyId(order.getCompanyId())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .type(order.getType())
                .status(order.getStatus())
                // Add date if your DTO supports it
                .build();
    }
}
