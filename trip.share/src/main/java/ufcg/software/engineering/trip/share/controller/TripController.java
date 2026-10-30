package ufcg.software.engineering.trip.share.controller;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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

    @PatchMapping("/{id}")
    public TripResponse update(@PathVariable UUID id,
                               @AuthenticationPrincipal FirebaseIdentity identity,
                               @Valid @RequestBody UpdateTripRequest request) {
        return tripService.update(id, request, identity);
    }

}
