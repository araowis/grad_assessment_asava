package com.example.exchange_server.dto;

import com.example.exchange_server.model.OrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class OrderRequestDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Company ID is required")
    private String companyId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @Min(value = 1, message = "Price must be greater than 0")
    private double price;

    @NotNull(message = "Order type is required")
    private OrderType type; // BUY / SELL
}
