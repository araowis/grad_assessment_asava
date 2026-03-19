package com.example.exchange_server.engine;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

import com.example.exchange_server.model.Order;

@Component
public class OrderBook {

    // One lock per company — shared with MatchingEngine via lockFor().
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, PriorityQueue<Order>> buyQueues  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PriorityQueue<Order>> sellQueues = new ConcurrentHashMap<>();

    public ReentrantLock lockFor(String companyId) {
        return locks.computeIfAbsent(companyId, k -> new ReentrantLock());
    }

    public void addBuy(Order order) {
        buyQueues
            .computeIfAbsent(order.getCompanyId(),
                k -> new PriorityQueue<>((a, b) -> Double.compare(b.getPrice(), a.getPrice())))
            .add(order);
    }
 
    /** Returns the highest-priced buy order without removing it, or null if empty. */
    public Order peekBestBuy(String companyId) {
        PriorityQueue<Order> q = buyQueues.get(companyId);
        return q == null ? null : q.peek();
    }
 
    /** Removes and returns the highest-priced buy order, or null if empty. */
    public Order pollBestBuy(String companyId) {
        PriorityQueue<Order> q = buyQueues.get(companyId);
        return q == null ? null : q.poll();
    }
 
    public boolean hasBuys(String companyId) {
        PriorityQueue<Order> q = buyQueues.get(companyId);
        return q != null && !q.isEmpty();
    }

    public void addSell(Order order) {
        sellQueues
            .computeIfAbsent(order.getCompanyId(),
                k -> new PriorityQueue<>(Comparator.comparingDouble(Order::getPrice)))
            .add(order);
    }

    public Order peekBestSell(String companyId) {
        PriorityQueue<Order> q = sellQueues.get(companyId);
        return q == null ? null : q.peek();
    }
 
    /** Removes and returns the lowest-priced sell order, or null if empty. */
    public Order pollBestSell(String companyId) {
        PriorityQueue<Order> q = sellQueues.get(companyId);
        return q == null ? null : q.poll();
    }
 
    public boolean hasSells(String companyId) {
        PriorityQueue<Order> q = sellQueues.get(companyId);
        return q != null && !q.isEmpty();
    }

    // public PriorityQueue<Order> getBuyOrders(String companyId) {
    //     return buyOrders.computeIfAbsent(companyId,
    //             k -> new PriorityQueue<>((a, b) -> Double.compare(b.getPrice(), a.getPrice()))); // Max Heap
    // }

    // public PriorityQueue<Order> getSellOrders(String companyId) {
    //     return sellOrders.computeIfAbsent(companyId,
    //             k -> new PriorityQueue<>(Comparator.comparingDouble(Order::getPrice))); // Min Heap
    // }
}
