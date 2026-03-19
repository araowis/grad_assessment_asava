package com.example.company_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.company_service.service.implementation.SseEmitterService;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockStreamController {

    @Autowired
    private SseEmitterService sseEmitterService;

    @GetMapping(value = "/stream/{companyId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPrice(@PathVariable String companyId) {
        return sseEmitterService.subscribe(companyId);
    }
}
