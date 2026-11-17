package ufcg.software.engineering.trip.share.exceptions;

public class TripException extends RuntimeException {
    public enum Reason {
        INVALID_PERIOD,
        INVALID_PAGINATION,
        INVALID_RESOLUTIONS,
        CONFIRMATION_REQUIRED,
        PUBLIC_ACTIVITY_REQUIRED,
        INVALID_DURATION,
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
