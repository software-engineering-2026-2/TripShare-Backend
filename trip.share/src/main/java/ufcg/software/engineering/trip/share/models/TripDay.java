package ufcg.software.engineering.trip.share.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trip_days", uniqueConstraints = @UniqueConstraint(
        name = "uq_trip_days_number", columnNames = {"trip_id", "day_number"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripDay {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_trip_days_trip"))
    private Trip trip;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    public static TripDay create(Trip trip, int dayNumber) {
        if (dayNumber <= 0) {
            throw new IllegalArgumentException("Invalid trip day number");
        }
        TripDay day = new TripDay();
        day.trip = Objects.requireNonNull(trip);
        day.dayNumber = dayNumber;
        return day;
    }

    public LocalDate getDate() {
        return trip.getStartsOn() == null ? null : trip.getStartsOn().plusDays(dayNumber - 1L);
    }
}
