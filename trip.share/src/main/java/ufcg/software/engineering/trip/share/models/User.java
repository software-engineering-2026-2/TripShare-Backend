package ufcg.software.engineering.trip.share.models;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auth_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_users_auth"))
    private Auth auth;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(columnDefinition = "text")
    private String hometown;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private UserStatus status = UserStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public static User create(Auth auth, String username, String name, String email,
                              String phone, String bio, String hometown) {
        User user = new User();
        user.auth = Objects.requireNonNull(auth);
        user.username = Objects.requireNonNull(username);
        user.name = Objects.requireNonNull(name);
        user.email = Objects.requireNonNull(email);
        user.phone = phone;
        user.bio = bio;
        user.hometown = hometown;
        return user;
    }
}
