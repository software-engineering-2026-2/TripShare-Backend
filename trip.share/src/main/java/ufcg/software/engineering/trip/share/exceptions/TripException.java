package ufcg.software.engineering.trip.share.exceptions;

public class TripException extends RuntimeException {
    public enum Reason { TRIP_NOT_FOUND, TRIP_FORBIDDEN, INVALID_PERIOD }

    private final Reason reason;

    public TripException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
