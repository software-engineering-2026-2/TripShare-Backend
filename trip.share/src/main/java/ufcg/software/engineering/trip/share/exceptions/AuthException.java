package ufcg.software.engineering.trip.share.exceptions;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    private AuthException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Credenciais inválidas.");
    }

    public static AuthException invalidSession() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_SESSION", "Sessão inválida ou expirada. Autentique-se novamente.");
    }

    public static AuthException unavailable() {
        return new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_PROVIDER_UNAVAILABLE", "Serviço de autenticação indisponível.");
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}
