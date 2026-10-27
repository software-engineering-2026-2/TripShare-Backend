package ufcg.software.engineering.trip.share.exceptions;

public class AuthException extends RuntimeException {
    public enum Reason { UID_MISMATCH, INVALID_EMAIL, USER_NOT_FOUND, USER_SUSPENDED, DUPLICATE_USER }

    private final Reason reason;

    public AuthException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
