package ufcg.software.engineering.trip.share.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ufcg.software.engineering.trip.share.models.TripDay;

public interface TripDayRepository extends JpaRepository<TripDay, UUID> {
    interface ActivityConflict {
        int getDayNumber();
        long getActivityCount();
    }

    @EntityGraph(attributePaths = "trip")
    List<TripDay> findByTripIdOrderByDayNumberAsc(UUID tripId);

    @Query(value = """
            SELECT td.day_number AS "dayNumber", count(ia.id) AS "activityCount"
            FROM trip_days td
            JOIN itinerary_activities ia ON ia.trip_day_id = td.id
            WHERE td.trip_id = :tripId AND td.day_number > :newDuration
            GROUP BY td.day_number
            ORDER BY td.day_number ASC
            """, nativeQuery = true)
    List<ActivityConflict> findActivityConflictsAfterDuration(
            @Param("tripId") UUID tripId, @Param("newDuration") int newDuration);

    @Modifying
    @Query("DELETE FROM TripDay td WHERE td.trip.id = :tripId AND td.dayNumber > :dayNumber")
    void deleteByTripIdAndDayNumberGreaterThan(@Param("tripId") UUID tripId, @Param("dayNumber") int dayNumber);
}
