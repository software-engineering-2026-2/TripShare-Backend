package ufcg.software.engineering.trip.share.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ufcg.software.engineering.trip.share.models.Auth;

public interface AuthRepository extends JpaRepository<Auth, UUID> {
    boolean existsByUuid(String uuid);
}
