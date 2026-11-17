package ufcg.software.engineering.trip.share.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ufcg.software.engineering.trip.share.models.Place;

public interface PlaceRepository extends JpaRepository<Place, UUID> {
    Page<Place> findByDestinationIdAndRemovedAtIsNullOrderByNameAscIdAsc(UUID destinationId, Pageable pageable);
}
