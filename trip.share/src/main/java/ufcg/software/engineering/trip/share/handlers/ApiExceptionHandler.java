package ufcg.software.engineering.trip.share.handlers;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ufcg.software.engineering.trip.share.exceptions.AuthException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ProblemDetail> authentication(AuthException ex, HttpServletRequest request) {
        var headers = new HttpHeaders();
        headers.setCacheControl("no-store");
        if (ex.getStatus() == HttpStatus.UNAUTHORIZED) headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return new ResponseEntity<>(AuthProblemResponses.problem(ex.getStatus(), ex.getErrorCode(),
                ex.getMessage(), request.getRequestURI()), headers, ex.getStatus());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> forbidden(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(AuthProblemResponses.problem(
                HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Acesso não permitido.", request.getRequestURI()));
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            problem.setProperty("errorCode", status.value() == 400 ? "INVALID_REQUEST" : "HTTP_" + status.value());
            if (status.value() == 400) problem.setDetail("Corpo ou parâmetros da requisição inválidos.");
        }
        var responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setCacheControl("no-store");
        return super.createResponseEntity(body, responseHeaders, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unexpected(Exception ex, HttpServletRequest request) {
        // A mensagem de uma exceção de cliente HTTP pode incluir credenciais: registrar só o tipo.
        log.error("Falha interna em {} ({})", request.getRequestURI(), ex.getClass().getSimpleName());
        return ResponseEntity.internalServerError().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(AuthProblemResponses.problem(HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_ERROR", "Erro interno do servidor.", request.getRequestURI()));
    }
}
