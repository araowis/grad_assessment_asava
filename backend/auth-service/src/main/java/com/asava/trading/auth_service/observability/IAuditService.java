package com.asava.trading.auth_service.observability;

public interface IAuditService {

    void loginSuccess(Long userId, String username);

    void loginFailure(String username);

    void tokenIssued(Long userId, String username);

    void tokenValidationFailed(String reason);

    void tokenRefreshed(Long userId, String username);
}
