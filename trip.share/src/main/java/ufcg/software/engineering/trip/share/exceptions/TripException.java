package ufcg.software.engineering.trip.share.exceptions;

public class TripException extends RuntimeException {
    public enum Reason { INVALID_PERIOD, INVALID_DURATION, DESTINATION_NOT_FOUND }

    private final Reason reason;

    public TripException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
