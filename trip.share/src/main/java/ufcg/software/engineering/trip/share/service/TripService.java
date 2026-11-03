package ufcg.software.engineering.trip.share.service;

import static ufcg.software.engineering.trip.share.exceptions.AuthException.Reason.USER_NOT_FOUND;
import static ufcg.software.engineering.trip.share.exceptions.AuthException.Reason.USER_SUSPENDED;
import static ufcg.software.engineering.trip.share.exceptions.TripException.Reason.DESTINATION_NOT_FOUND;
import static ufcg.software.engineering.trip.share.exceptions.TripException.Reason.INVALID_DURATION;
import static ufcg.software.engineering.trip.share.exceptions.TripException.Reason.INVALID_PERIOD;
import static ufcg.software.engineering.trip.share.exceptions.TripException.Reason.TRIP_FORBIDDEN;
import static ufcg.software.engineering.trip.share.exceptions.TripException.Reason.TRIP_NOT_FOUND;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ufcg.software.engineering.trip.share.dto.CreateTripRequest;
import ufcg.software.engineering.trip.share.dto.TripResponse;
import ufcg.software.engineering.trip.share.dto.UpdateTripRequest;
import ufcg.software.engineering.trip.share.exceptions.AuthException;
import ufcg.software.engineering.trip.share.exceptions.TripException;
import ufcg.software.engineering.trip.share.models.Destination;
import ufcg.software.engineering.trip.share.models.Trip;
import ufcg.software.engineering.trip.share.models.TripDay;
import ufcg.software.engineering.trip.share.models.User;
import ufcg.software.engineering.trip.share.models.UserStatus;
import ufcg.software.engineering.trip.share.repository.DestinationRepository;
import ufcg.software.engineering.trip.share.repository.TripDayRepository;
import ufcg.software.engineering.trip.share.repository.TripRepository;
import ufcg.software.engineering.trip.share.repository.UserRepository;

@Service
public class TripService {
    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final DestinationRepository destinationRepository;
    private final UserRepository userRepository;

    public TripService(TripRepository tripRepository,
                       TripDayRepository tripDayRepository,
                       DestinationRepository destinationRepository,
                       UserRepository userRepository) {
        this.tripRepository = tripRepository;
        this.tripDayRepository = tripDayRepository;
        this.destinationRepository = destinationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TripResponse createTrip(CreateTripRequest request, FirebaseIdentity identity) {
        User user = userRepository.findByAuthUuid(identity.uid())
                .orElseThrow(() -> new AuthException(USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthException(USER_SUSPENDED);
        }
        validatePeriodOrDuration(request);
        Destination destination = destinationRepository.findById(request.destinationId())
                .orElseThrow(() -> new TripException(DESTINATION_NOT_FOUND));

        Trip trip = request.startsOn() != null
                ? Trip.create(user, destination, request.title(), request.startsOn(), request.endsOn())
                : Trip.create(user, destination, request.title(), request.durationDays());
        tripRepository.save(trip);
        createEmptyDays(trip);
        return TripResponse.from(tripRepository.saveAndFlush(trip));
    }

    private void createEmptyDays(Trip trip) {
        List<TripDay> days = new ArrayList<>(trip.getDurationDays());
        for (int dayNumber = 1; dayNumber <= trip.getDurationDays(); dayNumber++) {
            days.add(TripDay.create(trip, dayNumber));
        }
        tripDayRepository.saveAll(days);
    }

    private void validatePeriodOrDuration(CreateTripRequest request) {
        boolean hasStartsOn = request.startsOn() != null;
        boolean hasEndsOn = request.endsOn() != null;
        if (hasStartsOn != hasEndsOn) {
            throw new TripException(INVALID_DURATION);
        }
        if (hasStartsOn) {
            if (request.endsOn().isBefore(request.startsOn())) {
                throw new TripException(INVALID_PERIOD);
            }
            int calculatedDays = Trip.calculateDurationDays(request.startsOn(), request.endsOn());
            if (request.durationDays() != null && request.durationDays() != calculatedDays) {
                throw new TripException(INVALID_DURATION);
            }
        } else if (request.durationDays() == null || request.durationDays() <= 0) {
            throw new TripException(INVALID_DURATION);
        }
    }

    @Transactional
    public TripResponse updateTrip(UUID tripId, UpdateTripRequest request, FirebaseIdentity identity) {
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

        Destination destination = null;
        if (request.destinationId() != null) {
            destination = destinationRepository.findById(request.destinationId())
                    .orElseThrow(() -> new TripException(DESTINATION_NOT_FOUND));
        }

        ResolvedPeriod period = resolvePeriod(trip, request);
        trip.update(request.title(), destination, period.startsOn(), period.endsOn(), period.durationDays());
        return TripResponse.from(tripRepository.saveAndFlush(trip));
    }

    private ResolvedPeriod resolvePeriod(Trip trip, UpdateTripRequest request) {
        boolean hasRequestStartsOn = request.startsOn() != null;
        boolean hasRequestEndsOn = request.endsOn() != null;
        boolean hasTripStartsOn = trip.getStartsOn() != null;
        boolean hasTripEndsOn = trip.getEndsOn() != null;

        LocalDate newStart;
        LocalDate newEnd;
        Integer newDuration = request.durationDays();

        if (hasRequestStartsOn || hasRequestEndsOn) {
            newStart = hasRequestStartsOn ? request.startsOn() : trip.getStartsOn();
            newEnd = hasRequestEndsOn ? request.endsOn() : trip.getEndsOn();

            // If existing trip had no dates, both startsOn and endsOn must be supplied
            if (newStart == null || newEnd == null) {
                throw new TripException(INVALID_DURATION);
            }

            if (newEnd.isBefore(newStart)) {
                throw new TripException(INVALID_PERIOD);
            }

            int calculated = Trip.calculateDurationDays(newStart, newEnd);
            if (newDuration != null && !newDuration.equals(calculated)) {
                throw new TripException(INVALID_PERIOD);
            }
            return new ResolvedPeriod(newStart, newEnd, calculated);
        }

        // Neither startsOn nor endsOn provided in request
        if (newDuration != null) {
            if (newDuration <= 0) {
                throw new TripException(INVALID_DURATION);
            }
            if (hasTripStartsOn && hasTripEndsOn) {
                newStart = trip.getStartsOn();
                newEnd = newStart.plusDays(newDuration - 1);
                return new ResolvedPeriod(newStart, newEnd, newDuration);
            }
            // Trip has no fixed dates, update durationDays directly
            return new ResolvedPeriod(null, null, newDuration);
        }

        // Dates and duration untouched
        return new ResolvedPeriod(trip.getStartsOn(), trip.getEndsOn(), trip.getDurationDays());
    }

    private record ResolvedPeriod(LocalDate startsOn, LocalDate endsOn, int durationDays) {}
}
