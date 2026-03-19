package com.example.company_service.service.implementation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseEmitterService {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String companyId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.computeIfAbsent(companyId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> emitters.getOrDefault(companyId, List.of()).remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        return emitter;
    }

    public void broadcast(String companyId, double newPrice) {
        List<SseEmitter> targets = emitters.getOrDefault(companyId, new CopyOnWriteArrayList<>());
        List<SseEmitter> dead = new ArrayList<>();

        for (SseEmitter emitter : targets) {
            try {
                emitter.send(SseEmitter.event()
                        .name("price-update")
                        .data(Map.of("companyId", companyId, "price", newPrice)));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        targets.removeAll(dead);
    }
}
