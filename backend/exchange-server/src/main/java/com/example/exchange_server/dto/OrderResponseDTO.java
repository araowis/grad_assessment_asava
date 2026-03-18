package com.example.exchange_server.dto;

import com.example.exchange_server.model.OrderStatus;
import com.example.exchange_server.model.OrderType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderResponseDTO {

    private Long id;
    private Long userId;
    private String companyId;

    private int quantity;
    private double price;

    private OrderType type;
    private OrderStatus status;

    private LocalDateTime createdAt;
}
