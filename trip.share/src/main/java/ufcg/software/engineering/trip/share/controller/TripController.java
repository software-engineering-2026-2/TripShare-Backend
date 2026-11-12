package ufcg.software.engineering.trip.share.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ufcg.software.engineering.trip.share.dto.CreateTripRequest;
import ufcg.software.engineering.trip.share.dto.TripResponse;
import ufcg.software.engineering.trip.share.dto.TripPlacesResponse;
import ufcg.software.engineering.trip.share.dto.UpdateTripRequest;
import ufcg.software.engineering.trip.share.service.FirebaseIdentity;
import ufcg.software.engineering.trip.share.service.TripService;

@RestController
@RequestMapping("/trips")
public class TripController {
    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse createTrip(@AuthenticationPrincipal FirebaseIdentity identity,
                                   @Valid @RequestBody CreateTripRequest request) {
        return tripService.createTrip(request, identity);
    }

    @PatchMapping("/{id}")
    public TripResponse updateTrip(@PathVariable UUID id,
                                   @AuthenticationPrincipal FirebaseIdentity identity,
                                   @Valid @RequestBody UpdateTripRequest request) {
        return tripService.updateTrip(id, request, identity);
    }

    @GetMapping("{id}/places")
    @ResponseStatus(HttpStatus.OK)
    public TripPlacesResponse getTripPlaces(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal FirebaseIdentity identity
    ) {
        return tripService.getPlacesTrip(id, identity, page, size);
    }
}
