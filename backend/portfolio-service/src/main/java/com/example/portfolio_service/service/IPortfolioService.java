package com.example.portfolio_service.service;

import java.util.List;

import com.example.portfolio_service.dto.PortfolioRequestDto;
import com.example.portfolio_service.dto.PortfolioResponseDto;

public interface IPortfolioService {

    PortfolioResponseDto buy(Long userId, PortfolioRequestDto request);

    void sell(Long userId, String companyId, int quantity);

    List<PortfolioResponseDto> getPortfolio(Long userId);
}
