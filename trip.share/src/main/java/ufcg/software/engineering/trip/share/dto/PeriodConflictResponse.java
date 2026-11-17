package ufcg.software.engineering.trip.share.dto;

import java.util.List;

public record PeriodConflictResponse(
        int status,
        String message,
        List<DayConflict> conflicts
) {}
