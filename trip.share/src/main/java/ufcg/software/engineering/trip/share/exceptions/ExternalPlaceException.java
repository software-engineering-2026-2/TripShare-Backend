package ufcg.software.engineering.trip.share.exceptions;

public class ExternalPlaceException extends RuntimeException {
    public enum Reason { SOURCE_UNAVAILABLE }

    private final Reason reason;

    public ExternalPlaceException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public ExternalPlaceException(Reason reason, Throwable cause) {
        super(reason.name(), cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
