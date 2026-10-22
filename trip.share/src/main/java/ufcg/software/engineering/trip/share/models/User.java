package ufcg.software.engineering.trip.share.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "firebase_uid", length = 128, unique = true)
    private String firebaseUid;

    @Email
    @Column(length = 254, nullable = false)
    private String email;

    @Column(length = 50, nullable = false)
    private String username;

    @Column(length = 120, nullable = false)
    private String name;

    @Column(length = 32)
    private String phone;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(columnDefinition = "text")
    private String hometown;

    @Column(nullable = false)
    private String role = "USER";

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
