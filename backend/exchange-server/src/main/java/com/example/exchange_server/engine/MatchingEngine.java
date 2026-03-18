package com.example.exchange_server.engine;

import java.util.PriorityQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.exchange_server.model.Order;
import com.example.exchange_server.repository.TradeRepository;

@Service
public class MatchingEngine {

    @Autowired
    private OrderBook orderBook;

    @Autowired
    private TradeRepository tradeRepository;

    public synchronized void processOrder(Order order) {

        PriorityQueue<Order> buyQueue = orderBook.getBuyOrders(order.getCompanyId());
        PriorityQueue<Order> sellQueue = orderBook.getSellOrders(order.getCompanyId());

        if (order.getType() == OrderType.BUY) {
            matchBuyOrder(order, sellQueue);
            if (order.getQuantity() > 0) {
                buyQueue.add(order);
            }
        } else {
            matchSellOrder(order, buyQueue);
            if (order.getQuantity() > 0) {
                sellQueue.add(order);
            }
        }
    }

    private void matchBuyOrder(Order buyOrder, PriorityQueue<Order> sellQueue) {
        while (!sellQueue.isEmpty() && buyOrder.getQuantity() > 0) {
            Order sellOrder = sellQueue.peek();

            if (sellOrder.getPrice() <= buyOrder.getPrice()) {
                executeTrade(buyOrder, sellOrder);
                if (sellOrder.getQuantity() == 0) {
                    sellQueue.poll();
                }
            } else {
                break;
            }
        }
    }

    private void matchSellOrder(Order sellOrder, PriorityQueue<Order> buyQueue) {
        while (!buyQueue.isEmpty() && sellOrder.getQuantity() > 0) {
            Order buyOrder = buyQueue.peek();

            if (buyOrder.getPrice() >= sellOrder.getPrice()) {
                executeTrade(buyOrder, sellOrder);
                if (buyOrder.getQuantity() == 0) {
                    buyQueue.poll();
                }
            } else {
                break;
            }
        }
    }

    private void executeTrade(Order buy, Order sell) {
        int qty = Math.min(buy.getQuantity(), sell.getQuantity());

        buy.setQuantity(buy.getQuantity() - qty);
        sell.setQuantity(sell.getQuantity() - qty);

        Trade trade = new Trade();
        trade.setBuyerId(buy.getUserId());
        trade.setSellerId(sell.getUserId());
        trade.setCompanyId(buy.getCompanyId());
        trade.setQuantity(qty);
        trade.setPrice(sell.getPrice());
        trade.setExecutedAt(LocalDateTime.now());

        tradeRepository.save(trade);
    }
}
