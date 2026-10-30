package ufcg.software.engineering.trip.share.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import ufcg.software.engineering.trip.share.models.Trip;

public record TripResponse(UUID id, UUID destinationId, String title, LocalDate startsOn,
        LocalDate endsOn, Integer durationDays, Instant createdAt, Instant updatedAt) {

    public static TripResponse from(Trip trip) {
        Integer duration = (int) ChronoUnit.DAYS.between(trip.getStartsOn(), trip.getEndsOn()) + 1;
        return new TripResponse(trip.getId(), trip.getDestinationId(), trip.getTitle(),
                trip.getStartsOn(), trip.getEndsOn(), duration,
                trip.getCreatedAt(), trip.getUpdatedAt());
    }
}
