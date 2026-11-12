package ufcg.software.engineering.trip.share.repository;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ufcg.software.engineering.trip.share.models.Place;

public interface PlaceRepository extends JpaRepository<Place, UUID> {

    /**
     * Card de local para listagens: projeção escalar que evita carregar a entidade completa.
     */
    interface PlaceCard {
        UUID getId();

        String getName();

        String getAddress();

        String getNeighborhood();

        String getDescription();

        BigDecimal getLatitude();

        BigDecimal getLongitude();

        Integer getEstimatedVisitMinutes();

        BigDecimal getEstimatedCost();

        String getCurrency();
    }

    Page<PlaceCard> findCardsByDestinationIdAndRemovedAtIsNullOrderByNameAscIdAsc(UUID destinationId, Pageable pageable);
}
