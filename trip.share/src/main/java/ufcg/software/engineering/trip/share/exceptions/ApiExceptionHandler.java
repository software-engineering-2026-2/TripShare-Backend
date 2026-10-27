package ufcg.software.engineering.trip.share.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ufcg.software.engineering.trip.share.dto.ErrorResponse;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> authentication(AuthException exception) {
        return switch (exception.reason()) {
            case UID_MISMATCH -> error(HttpStatus.UNAUTHORIZED, "UID does not match the Firebase token");
            case INVALID_EMAIL -> error(HttpStatus.BAD_REQUEST, "Firebase token must contain a valid email");
            case USER_NOT_FOUND -> error(HttpStatus.NOT_FOUND, "User profile not found");
            case USER_SUSPENDED -> error(HttpStatus.FORBIDDEN, "User is suspended");
            case DUPLICATE_USER -> error(HttpStatus.CONFLICT, "UID, email or username already registered");
        };
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> invalidRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request body");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException exception) {
        // Only expected uniqueness races are client conflicts. Other database failures stay server errors.
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && java.util.Set.of("uq_auth_uuid", "uq_users_auth", "uq_users_email", "uq_users_username")
                    .contains(violation.getConstraintName() == null ? "" : violation.getConstraintName())) {
                return error(HttpStatus.CONFLICT, "UID, email or username already registered");
            }
            cause = cause.getCause();
        }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Database operation failed");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        var response = ResponseEntity.status(status);
        if (status == HttpStatus.UNAUTHORIZED) response.header("WWW-Authenticate", "Bearer");
        return response.body(new ErrorResponse(status.value(), message));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> databaseUnavailable(DataAccessException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Database operation unavailable");
    }
}
