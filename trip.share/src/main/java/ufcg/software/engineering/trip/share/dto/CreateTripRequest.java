package ufcg.software.engineering.trip.share.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTripRequest(
        @NotBlank String title,
        @NotNull UUID destinationId,
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn) {
}
