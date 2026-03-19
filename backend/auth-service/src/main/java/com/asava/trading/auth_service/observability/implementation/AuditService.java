package com.asava.trading.auth_service.observability.implementation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.asava.trading.auth_service.observability.IAuditService;

@Slf4j
@Service
public class AuditService implements IAuditService {

    @Override
    public void loginSuccess(Long userId, String username) {
        log.info("event=login_success userId={} username={}", userId, username);
    }

    @Override
    public void loginFailure(String username) {
        log.warn("event=login_failure username={}", username);
    }

    @Override
    public void tokenIssued(Long userId, String username) {
        log.info("event=token_issued userId={} username={}", userId, username);
    }

    @Override
    public void tokenValidationFailed(String reason) {
        log.warn("event=token_invalid reason={}", reason);
    }

    @Override
    public void tokenRefreshed(Long userId, String username) {
        log.info("event=token_refreshed userId={} username={}", userId, username);
    }
}
