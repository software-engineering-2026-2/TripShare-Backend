package ufcg.software.engineering.trip.share.models;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auth {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "uuid", nullable = false, unique = true, length = 128)
    private String uuid;

    public static Auth create(String uuid) {
        if (uuid == null || uuid.isBlank() || uuid.length() > 128) {
            throw new IllegalArgumentException("Invalid Firebase UID");
        }
        Auth auth = new Auth();
        auth.uuid = uuid;
        return auth;
    }
}
