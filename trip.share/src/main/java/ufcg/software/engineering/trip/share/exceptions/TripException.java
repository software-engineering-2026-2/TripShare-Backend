package ufcg.software.engineering.trip.share.exceptions;

public class TripException extends RuntimeException {
    public enum Reason {
        INVALID_PERIOD,
        INVALID_DURATION,
        INVALID_PAGINATION,
        DESTINATION_NOT_FOUND,
        TRIP_NOT_FOUND,
        TRIP_FORBIDDEN
    }

    private final Reason reason;

    public TripException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
