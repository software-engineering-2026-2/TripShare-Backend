package ufcg.software.engineering.trip.share.exceptions;

import java.util.Collections;
import java.util.List;
import ufcg.software.engineering.trip.share.dto.DayConflict;

public class TripPeriodConflictException extends RuntimeException {
    private final List<DayConflict> conflicts;

    public TripPeriodConflictException(List<DayConflict> conflicts) {
        super("Cannot shorten trip: activities exist on days outside the new period");
        this.conflicts = conflicts != null ? Collections.unmodifiableList(conflicts) : List.of();
    }

    public List<DayConflict> getConflicts() {
        return conflicts;
    }
}
