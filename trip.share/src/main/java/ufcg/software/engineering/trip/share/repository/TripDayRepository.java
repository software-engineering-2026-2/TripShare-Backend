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
    @EntityGraph(attributePaths = "trip")
    List<TripDay> findByTripIdOrderByDayNumberAsc(UUID tripId);

    @Modifying
    @Query("DELETE FROM TripDay td WHERE td.trip.id = :tripId AND td.dayNumber > :dayNumber")
    void deleteByTripIdAndDayNumberGreaterThan(@Param("tripId") UUID tripId, @Param("dayNumber") int dayNumber);
}
