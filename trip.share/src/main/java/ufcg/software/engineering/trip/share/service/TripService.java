package ufcg.software.engineering.trip.share.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ufcg.software.engineering.trip.share.dto.TripResponse;
import ufcg.software.engineering.trip.share.dto.UpdateTripRequest;
import ufcg.software.engineering.trip.share.models.Trip;
import ufcg.software.engineering.trip.share.models.User;
import ufcg.software.engineering.trip.share.models.UserStatus;
import ufcg.software.engineering.trip.share.repository.TripRepository;
import ufcg.software.engineering.trip.share.repository.UserRepository;

import ufcg.software.engineering.trip.share.exceptions.AuthException;
import ufcg.software.engineering.trip.share.exceptions.TripException;
import static ufcg.software.engineering.trip.share.exceptions.AuthException.Reason.*;
import static ufcg.software.engineering.trip.share.exceptions.TripException.Reason.*;

@Service
public class TripService {
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    public TripService(TripRepository tripRepository, UserRepository userRepository) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
    }

    /*
     * Partial update of title, destination, dates or duration. Dates have priority
     * as input: the start is the anchor of the period, and the stored duration is a
     * generated column that is never written by the application.
     */
    @Transactional
    public TripResponse update(UUID tripId, UpdateTripRequest request, FirebaseIdentity identity) {
        User user = userRepository.findByAuthUuid(identity.uid())
                .orElseThrow(() -> new AuthException(USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthException(USER_SUSPENDED);
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripException(TRIP_NOT_FOUND));

        if (!trip.getUser().getId().equals(user.getId())) {
            throw new TripException(TRIP_FORBIDDEN);
        }

        LocalDate start = request.startsOn() != null ? request.startsOn() : trip.getStartsOn();
        LocalDate end = resolveEnd(trip, request, start);

        if (request.title() != null) {
            trip.setTitle(request.title().trim());
        }
        if (request.destinationId() != null) {
            trip.setDestinationId(request.destinationId());
        }
        trip.setStartsOn(start);
        trip.setEndsOn(end);

        tripRepository.saveAndFlush(trip);
        return TripResponse.from(trip);
    }

    private LocalDate resolveEnd(Trip trip, UpdateTripRequest request, LocalDate start) {
        Integer duration = request.durationDays();
        LocalDate end;
        if (request.endsOn() != null) {
            end = request.endsOn();
        } else if (duration != null) {
            end = start.plusDays(duration - 1);
        } else if (request.startsOn() != null) {
            end = start.plusDays(currentDuration(trip) - 1);
        } else {
            end = trip.getEndsOn();
        }

        // duration was sent together with end dates: dates win and must be compatible
        if (duration != null && request.endsOn() != null
                && duration != ChronoUnit.DAYS.between(start, end) + 1) {
            throw new TripException(INVALID_PERIOD);
        }
        if (end.isBefore(start)) {
            throw new TripException(INVALID_PERIOD);
        }
        return end;
    }

    private int currentDuration(Trip trip) {
        return (int) ChronoUnit.DAYS.between(trip.getStartsOn(), trip.getEndsOn()) + 1;
    }
}
