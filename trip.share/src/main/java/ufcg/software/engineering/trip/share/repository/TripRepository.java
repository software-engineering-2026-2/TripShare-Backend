package ufcg.software.engineering.trip.share.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import ufcg.software.engineering.trip.share.dto.TripDeletePreview.ItineraryImpact;
import org.springframework.data.jpa.repository.JpaRepository;
import ufcg.software.engineering.trip.share.models.Trip;

public interface TripRepository extends JpaRepository<Trip, UUID> {
    @Query(value = "SELECT count(*) FROM itineraries WHERE trip_id = :tripId", nativeQuery = true)
    long countItineraries(UUID tripId);

    @Query(value = """
            SELECT i.id, i.title, i.visibility = 'PUBLIC' AS published,
                   (SELECT count(*) FROM itinerary_favorites f WHERE f.itinerary_id = i.id) AS favorite_count,
                   EXISTS (SELECT 1 FROM public_itineraries p WHERE p.id = i.id) AS in_ranking
            FROM itineraries i
            WHERE i.trip_id = :tripId
            ORDER BY i.title, i.id
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<ItineraryImpact> findDeletionImpact(UUID tripId, int size, long offset);
}
