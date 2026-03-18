package com.example.portfolio_service.controller;

import com.example.portfolio_service.dto.PortfolioRequestDto;
import com.example.portfolio_service.dto.PortfolioResponseDto;
import com.example.portfolio_service.service.IPortfolioService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final IPortfolioService service;

    // ✅ BUY
    @PostMapping("/{userId}/buy")
    public ResponseEntity<PortfolioResponseDto> buy(
            @PathVariable Long userId,
            @RequestBody PortfolioRequestDto request) {

        return ResponseEntity.ok(service.buy(userId, request));
    }

    // ✅ SELL
    @PostMapping("/{userId}/sell")
    public ResponseEntity<PortfolioResponseDto> sell(
            @PathVariable Long userId,
            @RequestBody PortfolioRequestDto request) {

        return ResponseEntity.ok(service.sell(userId, request));
    }

    // ✅ GET PORTFOLIO (Fixed to include companyId filter as per your Service)
    @GetMapping("/{userId}/{portfolioId}")
    public ResponseEntity<List<PortfolioResponseDto>> getPortfolio(
            @PathVariable Long userId,
            @PathVariable Long portfolioId,
            @RequestParam(required = false) String companyId) {

        return ResponseEntity.ok(service.getPortfolio(userId, portfolioId, companyId));
    }
}
