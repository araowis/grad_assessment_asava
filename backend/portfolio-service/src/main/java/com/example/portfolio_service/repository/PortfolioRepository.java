package com.example.portfolio_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.portfolio_service.entity.Portfolio;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByUserId(Long userId);

    Optional<Portfolio> findByUserIdAndCompanyId(Long userId, String companyId);
}
