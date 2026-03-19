package com.example.exchange_server.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.exchange_server.dto.OrderRequestDTO;
import com.example.exchange_server.dto.OrderResponseDTO;
import com.example.exchange_server.service.OrderService;

@RestController
@RequestMapping("/api/v1/exchange")
public class ExchangeController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/order")
    public ResponseEntity<OrderResponseDTO> placeOrder(@RequestBody OrderRequestDTO dto) {
        OrderResponseDTO response = orderService.placeOrder(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponseDTO>> getUserOrders(@PathVariable Long userId) {
        List<OrderResponseDTO> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }
}
