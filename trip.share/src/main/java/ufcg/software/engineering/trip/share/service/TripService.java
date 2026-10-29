package ufcg.software.engineering.trip.share.service;

import static ufcg.software.engineering.trip.share.exceptions.AuthException.Reason.USER_NOT_FOUND;
import static ufcg.software.engineering.trip.share.exceptions.AuthException.Reason.USER_SUSPENDED;
import static ufcg.software.engineering.trip.share.exceptions.TripException.Reason.DESTINATION_NOT_FOUND;
import static ufcg.software.engineering.trip.share.exceptions.TripException.Reason.INVALID_DURATION;
import static ufcg.software.engineering.trip.share.exceptions.TripException.Reason.INVALID_PERIOD;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ufcg.software.engineering.trip.share.dto.CreateTripRequest;
import ufcg.software.engineering.trip.share.dto.TripResponse;
import ufcg.software.engineering.trip.share.exceptions.AuthException;
import ufcg.software.engineering.trip.share.exceptions.TripException;
import ufcg.software.engineering.trip.share.models.Destination;
import ufcg.software.engineering.trip.share.models.Trip;
import ufcg.software.engineering.trip.share.models.User;
import ufcg.software.engineering.trip.share.models.UserStatus;
import ufcg.software.engineering.trip.share.repository.DestinationRepository;
import ufcg.software.engineering.trip.share.repository.TripRepository;
import ufcg.software.engineering.trip.share.repository.UserRepository;

@Service
public class TripService {
    private final TripRepository tripRepository;
    private final DestinationRepository destinationRepository;
    private final UserRepository userRepository;

    public TripService(TripRepository tripRepository,
                       DestinationRepository destinationRepository,
                       UserRepository userRepository) {
        this.tripRepository = tripRepository;
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
        return TripResponse.from(tripRepository.saveAndFlush(trip));
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
}
