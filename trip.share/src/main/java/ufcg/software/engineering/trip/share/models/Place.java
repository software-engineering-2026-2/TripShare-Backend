package ufcg.software.engineering.trip.share.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id", nullable = false, foreignKey = @ForeignKey(name = "fk_places_destination"))
    private Destination destination;

    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String address;

    @Column(columnDefinition = "text")
    private String neighborhood;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "estimated_visit_minutes")
    private Integer estimatedVisitMinutes;

    @Column(name = "estimated_cost", precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 3)
    private String currency;

    @Column(nullable = false, columnDefinition = "text")
    private String source;

    @Column(name = "external_id", columnDefinition = "text")
    private String externalId;

    @Column(name = "source_url", columnDefinition = "text")
    private String sourceUrl;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Place create(Destination destination, String name, String source) {
        Place place = new Place();
        place.destination = Objects.requireNonNull(destination);
        place.name = Objects.requireNonNull(name);
        place.source = Objects.requireNonNull(source);
        return place;
    }
}
