package com.example.exchange_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.exchange_server.model.Trade;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
}
