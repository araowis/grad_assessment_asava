package com.example.exchange_server.engine;

import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.example.exchange_server.model.Order;

@Component
public class OrderBook {

    private final Map<String, PriorityQueue<Order>> buyOrders = new ConcurrentHashMap<>();
    private final Map<String, PriorityQueue<Order>> sellOrders = new ConcurrentHashMap<>();

    public PriorityQueue<Order> getBuyOrders(String companyId) {
        return buyOrders.computeIfAbsent(companyId,
                k -> new PriorityQueue<>((a, b) -> Double.compare(b.getPrice(), a.getPrice()))); // Max Heap
    }

    public PriorityQueue<Order> getSellOrders(String companyId) {
        return sellOrders.computeIfAbsent(companyId,
                k -> new PriorityQueue<>(Comparator.comparingDouble(Order::getPrice))); // Min Heap
    }
}
