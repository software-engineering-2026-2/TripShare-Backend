package ufcg.software.engineering.trip.share.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ufcg.software.engineering.trip.share.models.TripDay;

public interface TripDayRepository extends JpaRepository<TripDay, UUID> {
    @EntityGraph(attributePaths = "trip")
    List<TripDay> findByTripIdOrderByDayNumberAsc(UUID tripId);
}
