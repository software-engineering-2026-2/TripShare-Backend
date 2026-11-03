package ufcg.software.engineering.trip.share.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import ufcg.software.engineering.trip.share.models.Destination;
import ufcg.software.engineering.trip.share.models.Trip;

public record TripResponse(
        UUID id,
        UUID userId,
        UUID destinationId,
        String destinationName,
        String destinationRegion,
        String destinationCountryCode,
        String title,
        LocalDate startsOn,
        LocalDate endsOn,
        int durationDays,
        Instant createdAt,
        Instant updatedAt) {

    public static TripResponse from(Trip trip) {
        Destination destination = trip.getDestination();
        return new TripResponse(
                trip.getId(),
                trip.getUser().getId(),
                destination.getId(),
                destination.getName(),
                destination.getRegion(),
                destination.getCountryCode(),
                trip.getTitle(),
                trip.getStartsOn(),
                trip.getEndsOn(),
                trip.getDurationDays(),
                trip.getCreatedAt(),
                trip.getUpdatedAt());
    }
}
