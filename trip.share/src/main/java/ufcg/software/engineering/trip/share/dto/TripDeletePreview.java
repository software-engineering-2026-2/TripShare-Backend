package ufcg.software.engineering.trip.share.dto;

import java.util.List;
import java.util.UUID;

public record TripDeletePreview(List<ItineraryImpact> content, int page, int size,
                                long totalElements, long totalPages, boolean last) {
    public record ItineraryImpact(UUID id, String title, boolean published, long favoriteCount, boolean inRanking) {}
}
