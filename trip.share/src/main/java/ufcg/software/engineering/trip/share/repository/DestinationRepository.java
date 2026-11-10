package ufcg.software.engineering.trip.share.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ufcg.software.engineering.trip.share.models.Destination;

public interface DestinationRepository extends JpaRepository<Destination, UUID> {
}
