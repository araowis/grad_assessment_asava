package com.example.portfolio_service.service;

import com.example.portfolio_service.dto.PortfolioRequestDto;
import com.example.portfolio_service.dto.PortfolioResponseDto;
import java.util.List;

public interface IPortfolioService {

    PortfolioResponseDto buy(Long userId, PortfolioRequestDto request);

    PortfolioResponseDto sell(Long userId, PortfolioRequestDto request);

    List<PortfolioResponseDto> getPortfolio(Long userId, Long portfolioId, String companyId);

    void checkAndExecuteStopLoss();
}
