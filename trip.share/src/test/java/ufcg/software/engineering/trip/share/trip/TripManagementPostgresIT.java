package ufcg.software.engineering.trip.share.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ufcg.software.engineering.trip.share.service.FirebaseIdentity;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripManagementPostgresIT {
    private static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:16-alpine");
    private static final UUID TRIP = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID PUBLIC = UUID.fromString("60000000-0000-0000-0000-000000000001");
    private static final UUID PRIVATE = UUID.fromString("60000000-0000-0000-0000-000000000003");
    private static final UUID SECOND = UUID.fromString("60000000-0000-0000-0000-000000000004");
    private static final String PATH = "/trips/" + TRIP;
    private static final String PREVIEW = PATH + "/delete-preview";

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

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @MockitoBean FirebaseTokenService tokens;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE users, auth, destinations CASCADE");
        new ResourceDatabasePopulator(new FileSystemResource("scripts/seeds/demo.sql")).execute(dataSource);
        jdbc.update("INSERT INTO itineraries(id,trip_id,title) VALUES (?,?,'A'), (?,?,'A')", SECOND, TRIP, PRIVATE, TRIP);
        jdbc.update("INSERT INTO itinerary_favorites(itinerary_id,user_id) SELECT ?,id FROM users WHERE username='demo_admin'", PUBLIC);
        when(tokens.validate("owner")).thenReturn(new FirebaseIdentity("demo-only-alex", "alex@example.invalid"));
        when(tokens.validate("other")).thenReturn(new FirebaseIdentity("demo-only-sam", "sam@example.invalid"));
    }

    private ResultActions preview(String query) throws Exception {
        return mvc.perform(get(PREVIEW + query).header("Authorization", "Bearer owner"));
    }

    private ResultActions remove(String query) throws Exception {
        return mvc.perform(delete(PATH + query).header("Authorization", "Bearer owner"));
    }

    private ResultActions edit(String body) throws Exception {
        return mvc.perform(patch(PATH).header("Authorization", "Bearer owner")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @Test
    void previewPaginatesItinerariesAndCountsEachFavoriteOnce() throws Exception {
        preview("").andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true)).andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].id").value(PRIVATE.toString()))
                .andExpect(jsonPath("$.content[0].published").value(false))
                .andExpect(jsonPath("$.content[0].inRanking").value(false))
                .andExpect(jsonPath("$.content[0].favoriteCount").value(0))
                .andExpect(jsonPath("$.content[1].id").value(SECOND.toString()));
        preview("?page=1&size=2").andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.content[0].id").value(PUBLIC.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Demo public museum route"))
                .andExpect(jsonPath("$.content[0].published").value(true))
                .andExpect(jsonPath("$.content[0].favoriteCount").value(2))
                .andExpect(jsonPath("$.content[0].inRanking").value(true));
        jdbc.update("UPDATE itineraries SET hidden_by_moderation=true WHERE id=?", PUBLIC);
        preview("?page=1&size=2").andExpect(jsonPath("$.content[0].published").value(true))
                .andExpect(jsonPath("$.content[0].inRanking").value(false));
    }

    @Test
    void previewValidatesBoundsAndIncludesTotalsOnEmptyPages() throws Exception {
        for (String query : new String[]{"?page=-1", "?size=0"}) {
            preview(query).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid pagination parameters"));
        }
        preview("?size=101&page=2147483647").andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100)).andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1)).andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.content").isEmpty());
        jdbc.update("DELETE FROM itineraries WHERE trip_id=?", TRIP);
        preview("").andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0)).andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void confirmationAfterOnlyFirstPageDeletesEntireTripAndPreservesOtherTrip() throws Exception {
        preview("?size=1").andExpect(status().isOk()).andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.content[0].id").value(PRIVATE.toString()));
        for (String query : new String[]{"", "?confirm=false", "?confirm=yes", "?confirm=1"}) {
            remove(query).andExpect(status().isBadRequest());
        }
        assertTripIntact();
        remove("?confirm=true").andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(count("SELECT count(*) FROM trips WHERE id=?", TRIP)).isZero();
        assertThat(count("SELECT count(*) FROM trip_days WHERE trip_id=?", TRIP)).isZero();
        assertThat(count("SELECT count(*) FROM itineraries WHERE trip_id=?", TRIP)).isZero();
        assertThat(count("SELECT count(*) FROM itinerary_activities WHERE itinerary_id=?", PUBLIC)).isZero();
        assertThat(count("SELECT count(*) FROM itinerary_favorites WHERE itinerary_id=?", PUBLIC)).isZero();
        assertThat(count("SELECT count(*) FROM public_itineraries WHERE id=?", PUBLIC)).isZero();
        assertThat(count("SELECT count(*) FROM itinerary_ranking() WHERE itinerary_id=?", PUBLIC)).isZero();
        assertThat(count("SELECT count(*) FROM trips")).isOne();
        assertThat(count("SELECT count(*) FROM itineraries WHERE origin='COPIED' AND source_itinerary_id IS NULL")).isOne();
        preview("").andExpect(status().isNotFound());
        remove("?confirm=true").andExpect(status().isNotFound());
    }

    @Test
    void failedCommitRestoresAllCascadedRows() throws Exception {
        jdbc.execute("CREATE TABLE deletion_guard (trip_id uuid REFERENCES trips(id) DEFERRABLE INITIALLY DEFERRED)");
        try {
            jdbc.update("INSERT INTO deletion_guard VALUES (?)", TRIP);
            remove("?confirm=true").andExpect(status().isInternalServerError());
            assertTripIntact();
        } finally {
            jdbc.execute("DROP TABLE deletion_guard");
        }
    }

    private void assertTripIntact() {
        assertThat(count("SELECT count(*) FROM trips WHERE id=?", TRIP)).isOne();
        assertThat(count("SELECT count(*) FROM trip_days WHERE trip_id=?", TRIP)).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM itineraries WHERE trip_id=?", TRIP)).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM itinerary_favorites WHERE itinerary_id=?", PUBLIC)).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM itinerary_ranking() WHERE itinerary_id=?", PUBLIC)).isOne();
    }

    @Test
    void previewAndDeletionRequireAnActiveOwner() throws Exception {
        mvc.perform(get(PREVIEW)).andExpect(status().isUnauthorized());
        mvc.perform(delete(PATH + "?confirm=true")).andExpect(status().isUnauthorized());
        mvc.perform(get(PREVIEW).header("Authorization", "Bearer other")).andExpect(status().isForbidden());
        mvc.perform(delete(PATH + "?confirm=true").header("Authorization", "Bearer other")).andExpect(status().isForbidden());
        jdbc.update("UPDATE users SET status='SUSPENDED' WHERE username='demo_alex'");
        preview("").andExpect(status().isForbidden());
        remove("?confirm=true").andExpect(status().isForbidden());
    }

    @Test
    void shorteningMovesAndRemovesActivitiesInOneTransaction() throws Exception {
        jdbc.update("""
                INSERT INTO itinerary_activities(itinerary_id,trip_day_id,name,position)
                SELECT ?,id,'Move me',1 FROM trip_days WHERE trip_id=? AND day_number=3
                """, PUBLIC, TRIP);
        jdbc.update("""
                INSERT INTO itinerary_activities(itinerary_id,trip_day_id,name,position)
                SELECT ?,id,'Remove me',1 FROM trip_days WHERE trip_id=? AND day_number=2
                """, PRIVATE, TRIP);
        edit("""
                {"durationDays":1,"resolutions":[null]}
                """).andExpect(status().isBadRequest());
        edit("""
                {"durationDays":1,"resolutions":[{"dayNumber":3,"action":"MOVE","targetDay":1}]}
                """).andExpect(status().isBadRequest());
        edit("""
                {"durationDays":1,"resolutions":[{"dayNumber":3,"action":"MOVE","targetDay":1},{"dayNumber":2,"action":"REMOVE"}]}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.durationDays").value(1));
        assertThat(count("SELECT count(*) FROM itinerary_activities WHERE itinerary_id=? AND name='Remove me'", PRIVATE)).isZero();
        assertThat(count("""
                SELECT count(*) FROM itinerary_activities a JOIN trip_days d ON d.id=a.trip_day_id
                WHERE a.itinerary_id=? AND a.name='Move me' AND d.day_number=1 AND a.position=3
                """, PUBLIC)).isOne();
    }

    @Test
    void removingLastLocatedPublicActivityReturnsConflictAndRestoresPeriod() throws Exception {
        jdbc.update("""
                UPDATE itinerary_activities SET trip_day_id=(SELECT id FROM trip_days WHERE trip_id=? AND day_number=3)
                WHERE itinerary_id=?
                """, TRIP, PUBLIC);
        edit("""
                {"durationDays":1,"resolutions":[{"dayNumber":3,"action":"REMOVE"}]}
                """).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A public itinerary needs an activity with an identified location"));
        assertTripIntact();
        assertThat(count("SELECT duration_days FROM trips WHERE id=?", TRIP)).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM itinerary_activities WHERE itinerary_id=?", PUBLIC)).isEqualTo(2);
    }
}
