package ufcg.software.engineering.trip.share.trip;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ufcg.software.engineering.trip.share.dto.DayResolution;
import ufcg.software.engineering.trip.share.dto.UpdateTripRequest;
import ufcg.software.engineering.trip.share.exceptions.TripException;
import ufcg.software.engineering.trip.share.models.Trip;
import ufcg.software.engineering.trip.share.models.User;
import ufcg.software.engineering.trip.share.repository.*;
import ufcg.software.engineering.trip.share.service.FirebaseIdentity;
import ufcg.software.engineering.trip.share.service.TripService;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {
    @Mock TripRepository trips;
    @Mock TripDayRepository days;
    @Mock UserRepository users;
    @Mock DestinationRepository destinations;
    @InjectMocks TripService service;

    private final UUID tripId = UUID.randomUUID();
    private final FirebaseIdentity identity = new FirebaseIdentity("owner", "owner@example.invalid");

    private Trip loadTrip(boolean owned) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(users.findByAuthUuid(identity.uid())).thenReturn(Optional.of(user));
        Trip trip = mock(Trip.class);
        when(trips.findById(tripId)).thenReturn(Optional.of(trip));
        User owner = user;
        if (!owned) {
            owner = mock(User.class);
            when(owner.getId()).thenReturn(UUID.randomUUID());
        }
        when(trip.getUser()).thenReturn(owner);
        return trip;
    }

    @Test
    void otherOwnerCannotReadImpactOrDeleteTrip() {
        loadTrip(false);
        assertThatThrownBy(() -> service.previewDeletion(tripId, identity, 0, 20))
                .isInstanceOf(TripException.class).hasMessage("TRIP_FORBIDDEN");
        assertThatThrownBy(() -> service.deleteTrip(tripId, identity, true))
                .isInstanceOf(TripException.class).hasMessage("TRIP_FORBIDDEN");
        verify(trips, never()).countItineraries(any());
        verify(trips, never()).delete(any(Trip.class));
    }

    @Test
    void onlyExplicitConfirmationDeletesTrip() {
        Trip trip = loadTrip(true);
        assertThatThrownBy(() -> service.deleteTrip(tripId, identity, false))
                .isInstanceOf(TripException.class).hasMessage("CONFIRMATION_REQUIRED");
        verify(trips, never()).delete(trip);
        service.deleteTrip(tripId, identity, true);
        verify(trips).delete(trip);
    }

    @Test
    void duplicateResolutionRejectsEntireBatchBeforeMovingActivities() {
        Trip trip = loadTrip(true);
        when(trip.getDurationDays()).thenReturn(3);
        when(trip.getId()).thenReturn(tripId);
        var conflict = mock(TripDayRepository.ActivityConflict.class);
        when(conflict.getDayNumber()).thenReturn(3);
        when(days.findActivityConflictsAfterDuration(tripId, 2)).thenReturn(List.of(conflict));
        var move = new DayResolution(3, DayResolution.Action.MOVE, 1);
        var request = new UpdateTripRequest(null, null, null, null, 2, List.of(move, move));
        assertThatThrownBy(() -> service.updateTrip(tripId, request, identity))
                .isInstanceOf(TripException.class).hasMessage("INVALID_RESOLUTIONS");
        verify(days, never()).moveActivities(any(), any());
        verify(trips, never()).saveAndFlush(any());
    }
}
