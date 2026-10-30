package ufcg.software.engineering.trip.share.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "trips")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_trips_user"))
    private User user;

    @Column(name = "destination_id", nullable = false)
    @Setter
    private UUID destinationId;

    @Column(nullable = false, columnDefinition = "text")
    @Setter
    private String title;

    @Column(name = "starts_on", nullable = false)
    @Setter
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    @Setter
    private LocalDate endsOn;

    @Column(name = "duration_days", insertable = false, updatable = false)
    private Integer durationDays;

    @Column(name = "pace_code", columnDefinition = "text")
    private String paceCode;

    @Column(name = "transport_mode_code", columnDefinition = "text")
    private String transportModeCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public static Trip create(User user, UUID destinationId, String title,
                              LocalDate startsOn, LocalDate endsOn) {
        Trip trip = new Trip();
        trip.user = Objects.requireNonNull(user);
        trip.destinationId = Objects.requireNonNull(destinationId);
        trip.title = Objects.requireNonNull(title);
        trip.startsOn = Objects.requireNonNull(startsOn);
        trip.endsOn = Objects.requireNonNull(endsOn);
        return trip;
    }
}
