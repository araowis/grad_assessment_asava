package com.example.company_service.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.company_service.dto.StockHistoryDTO;
import com.example.company_service.models.StockPriceHistory;
import com.example.company_service.repository.HistoryRepository;
import com.example.company_service.service.implementation.SseEmitterService;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockStreamController {

    @Autowired
    private SseEmitterService sseEmitterService;
    @Autowired
    private HistoryRepository historyRepo;

    @GetMapping(value = "/stream/{companyId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPrice(@PathVariable String companyId) {
        return sseEmitterService.subscribe(companyId);
    }

    @GetMapping("/history/{companyId}")
    public List<StockHistoryDTO> getHistory(
            @PathVariable String companyId,
            @RequestParam(defaultValue = "500") int limit) {
        return historyRepo
                .findByCompanyShortIdOrderByRecordedAtAsc(companyId, PageRequest.of(0, limit))
                .getContent() // Page → List
                .stream()
                .map(h -> new StockHistoryDTO(h.getPrice(), h.getRecordedAt()))
                .collect(Collectors.toList());
    }
}
