package ufcg.software.engineering.trip.share.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "destinations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Destination {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Column(columnDefinition = "text")
    private String region;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, columnDefinition = "text")
    private String timezone;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(nullable = false, columnDefinition = "text")
    private String source;

    @Column(name = "external_id", columnDefinition = "text")
    private String externalId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Destination create(String name, String region, String countryCode,
                                     String timezone, String source, String externalId) {
        Destination destination = new Destination();
        destination.name = Objects.requireNonNull(name);
        destination.region = region;
        destination.countryCode = Objects.requireNonNull(countryCode);
        destination.timezone = Objects.requireNonNull(timezone);
        destination.source = Objects.requireNonNull(source);
        destination.externalId = externalId;
        return destination;
    }
}
