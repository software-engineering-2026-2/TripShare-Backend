package ufcg.software.engineering.trip.share.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ufcg.software.engineering.trip.share.models.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    @EntityGraph(attributePaths = "auth")
    Optional<User> findByAuthUuid(String uuid);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);
}
