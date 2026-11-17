package ufcg.software.engineering.trip.share.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DayResolution(
        @NotNull @Min(1) Integer dayNumber,
        @NotNull Action action,
        @Min(1) Integer targetDay
) {
    public enum Action { MOVE, REMOVE }
}