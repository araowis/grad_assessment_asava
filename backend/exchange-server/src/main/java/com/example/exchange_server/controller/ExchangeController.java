package com.example.exchange_server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exchange")
public class ExchangeController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/order")
    public ResponseEntity<String> placeOrder(@RequestBody OrderRequestDTO dto) {
        orderService.placeOrder(dto);
        return ResponseEntity.ok("Order placed successfully");
    }
}
