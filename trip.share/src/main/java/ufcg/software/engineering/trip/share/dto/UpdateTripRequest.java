package ufcg.software.engineering.trip.share.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.Pattern;

public record UpdateTripRequest(

        @Size(max = 120) @Pattern(regexp = ".*\\S.*") String title,
        UUID destinationId,
        LocalDate startsOn,
        LocalDate endsOn,
        @Positive Integer durationDays) {

}
