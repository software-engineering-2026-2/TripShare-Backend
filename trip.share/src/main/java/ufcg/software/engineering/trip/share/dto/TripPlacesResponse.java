package ufcg.software.engineering.trip.share.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record TripPlacesResponse(
        List<PlaceCardResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {

    public static TripPlacesResponse from(Page<PlaceCardResponse> places) {
        return new TripPlacesResponse(
                places.getContent(),
                places.getNumber(),
                places.getSize(),
                places.getTotalElements(),
                places.getTotalPages(),
                places.isLast());
    }
}
