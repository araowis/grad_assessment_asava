package com.example.exchange_server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.exchange_server.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

}
