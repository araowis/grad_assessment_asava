package com.example.exchange_server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.exchange_server.model.Trade;
import com.example.exchange_server.repository.TradeRepository;

@Service
public class TradeService {

    @Autowired
    private TradeRepository tradeRepository;

    public void saveTrade(Trade trade) {
        tradeRepository.save(trade);
    }
}
