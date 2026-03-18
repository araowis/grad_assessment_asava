package com.example.portfolio_service.controller;

import com.example.portfolio_service.dto.PortfolioRequestDto;
import com.example.portfolio_service.dto.PortfolioResponseDto;
import com.example.portfolio_service.service.IPortfolioService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final IPortfolioService service;

    @PostMapping("/{userId}/buy")
    public ResponseEntity<PortfolioResponseDto> buy(
            @PathVariable Long userId,
            @RequestBody PortfolioRequestDto request) {

        return ResponseEntity.ok(service.buy(userId, request));
    }

    @PostMapping("/{userId}/sell")
    public ResponseEntity<Void> sell(
            @PathVariable Long userId,
            @RequestParam String companyId,
            @RequestParam int quantity) {

        service.sell(userId, companyId, quantity);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<PortfolioResponseDto>> getPortfolio(
            @PathVariable Long userId) {

        return ResponseEntity.ok(service.getPortfolio(userId));
    }
}
