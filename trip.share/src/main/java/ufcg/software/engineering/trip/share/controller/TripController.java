package ufcg.software.engineering.trip.share.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ufcg.software.engineering.trip.share.dto.CreateTripRequest;
import ufcg.software.engineering.trip.share.dto.TripDeletePreview;
import ufcg.software.engineering.trip.share.dto.TripResponse;
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

    @GetMapping("/{id}/delete-preview")
    public TripDeletePreview previewDeletion(@PathVariable UUID id,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @AuthenticationPrincipal FirebaseIdentity identity) {
        return tripService.previewDeletion(id, identity, page, size);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrip(@PathVariable UUID id, @RequestParam(required = false) String confirm,
                           @AuthenticationPrincipal FirebaseIdentity identity) {
        tripService.deleteTrip(id, identity, "true".equals(confirm));
    }
}
