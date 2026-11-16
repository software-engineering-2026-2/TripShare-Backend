package ufcg.software.engineering.trip.share.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ufcg.software.engineering.trip.share.models.Destination;
import ufcg.software.engineering.trip.share.models.Place;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlaceRepositoryPostgresIT {
    private static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        DATABASE.start();
        properties.add("spring.datasource.url", DATABASE::getJdbcUrl);
        properties.add("spring.datasource.username", DATABASE::getUsername);
        properties.add("spring.datasource.password", DATABASE::getPassword);
        properties.add("spring.flyway.enabled", () -> true);
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @AfterAll
    static void stop() {
        DATABASE.stop();
    }

    @Autowired private PlaceRepository places;
    @Autowired private DestinationRepository destinations;
    @Autowired private EntityManager entityManager;

    @Test
    void findByDestination_shouldFilterRemovedAndOrderStablyWithinPage() {
        Destination destination = destinations.save(Destination.create("Test", null, "BR", "UTC", "test", null));
        Destination other = destinations.save(Destination.create("Other", null, "US", "UTC", "test", null));
        Place beta = places.save(Place.create(destination, "Beta", "test"));
        places.save(Place.create(destination, "Alpha", "test"));
        places.save(Place.create(destination, "Alpha", "test"));
        places.save(Place.create(other, "Aardvark", "test"));
        places.flush();
        entityManager.createNativeQuery("update places set removed_at = CURRENT_TIMESTAMP where id = :id")
                .setParameter("id", beta.getId()).executeUpdate();
        entityManager.clear();

        var firstPage = places.findByDestinationIdAndRemovedAtIsNullOrderByNameAscIdAsc(
                destination.getId(), PageRequest.of(0, 2));
        var secondPage = places.findByDestinationIdAndRemovedAtIsNullOrderByNameAscIdAsc(
                destination.getId(), PageRequest.of(1, 2));

        assertThat(firstPage.getContent()).extracting(Place::getName).containsExactly("Alpha", "Alpha");
        // Postgres orders uuid lexicographically by bytes; compare the hex string instead of UUID.compareTo
        assertThat(firstPage.getContent()).extracting(place -> place.getId().toString()).isSorted();
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(secondPage).isEmpty();
    }

    @Test
    void findByDestination_shouldPreserveUnknownValues() {
        Destination destination = destinations.save(Destination.create("Test", null, "BR", "UTC", "test", null));
        Place rich = places.save(Place.create(destination, "Rich", "test"));
        places.save(Place.create(destination, "Sparse", "test"));
        places.flush();
        entityManager.createNativeQuery("""
                update places set address = :address, neighborhood = :neighborhood, description = :description,
                    latitude = :latitude, longitude = :longitude, estimated_visit_minutes = :minutes,
                    estimated_cost = :cost, currency = :currency
                where id = :id
                """)
                .setParameter("address", "Rua Exemplo, 100")
                .setParameter("neighborhood", "Centro")
                .setParameter("description", "Ponto turístico")
                .setParameter("latitude", new BigDecimal("12.345678"))
                .setParameter("longitude", new BigDecimal("-54.123456"))
                .setParameter("minutes", 90)
                .setParameter("cost", new BigDecimal("25.50"))
                .setParameter("currency", "BRL")
                .setParameter("id", rich.getId())
                .executeUpdate();
        entityManager.clear();

        var found = places.findByDestinationIdAndRemovedAtIsNullOrderByNameAscIdAsc(
                destination.getId(), PageRequest.of(0, 20)).getContent();

        Place richPlace = found.get(0);
        assertThat(richPlace.getId()).isEqualTo(rich.getId());
        assertThat(richPlace.getName()).isEqualTo("Rich");
        assertThat(richPlace.getAddress()).isEqualTo("Rua Exemplo, 100");
        assertThat(richPlace.getNeighborhood()).isEqualTo("Centro");
        assertThat(richPlace.getDescription()).isEqualTo("Ponto turístico");
        assertThat(richPlace.getLatitude()).isEqualByComparingTo("12.345678");
        assertThat(richPlace.getLongitude()).isEqualByComparingTo("-54.123456");
        assertThat(richPlace.getEstimatedVisitMinutes()).isEqualTo(90);
        assertThat(richPlace.getEstimatedCost()).isEqualByComparingTo("25.50");
        assertThat(richPlace.getCurrency()).isEqualTo("BRL");

        Place sparsePlace = found.get(1);
        assertThat(sparsePlace.getAddress()).isNull();
        assertThat(sparsePlace.getNeighborhood()).isNull();
        assertThat(sparsePlace.getDescription()).isNull();
        assertThat(sparsePlace.getLatitude()).isNull();
        assertThat(sparsePlace.getLongitude()).isNull();
        assertThat(sparsePlace.getEstimatedVisitMinutes()).isNull();
        assertThat(sparsePlace.getEstimatedCost()).isNull();
        assertThat(sparsePlace.getCurrency()).isNull();
    }
}
