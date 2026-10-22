package ufcg.software.engineering.trip.share.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ufcg.software.engineering.trip.share.models.UserSession;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    @EntityGraph(attributePaths = "user")
    Optional<UserSession> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query("update UserSession s set s.revokedAt = :now "
            + "where s.id = :id and s.revokedAt is null and s.expiresAt > :now")
    int revoke(@Param("id") UUID id, @Param("now") Instant now);
}
