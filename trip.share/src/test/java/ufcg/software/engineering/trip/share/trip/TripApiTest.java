package ufcg.software.engineering.trip.share.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ufcg.software.engineering.trip.share.service.FirebaseIdentity;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripApiTest {
    private static final String ALICE_TOKEN = "alice-token";
    private static final String BOB_TOKEN = "bob-token";
    private static final String ALICE_UID = "uid-alice";
    private static final String BOB_UID = "uid-bob";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private FirebaseTokenService tokenService;

    private UUID aliceTripId;
    private UUID bobTripId;

    @BeforeEach
    void prepare() throws Exception {
        jdbc.update("DELETE FROM trips");
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM auth");
        when(tokenService.validate(ALICE_TOKEN))
                .thenReturn(new FirebaseIdentity(ALICE_UID, "alice@example.invalid"));
        when(tokenService.validate(BOB_TOKEN))
                .thenReturn(new FirebaseIdentity(BOB_UID, "bob@example.invalid"));
        register(ALICE_TOKEN, ALICE_UID, "alice", "alice@example.invalid");
        register(BOB_TOKEN, BOB_UID, "bob", "bob@example.invalid");

        UUID aliceId = userIdOf(ALICE_UID);
        UUID bobId = userIdOf(BOB_UID);
        aliceTripId = insertTrip(aliceId, "Alice trip", "2030-01-10", "2030-01-12");
        bobTripId = insertTrip(bobId, "Bob trip", "2030-02-10", "2030-02-14");
    }

    private void register(String token, String uid, String username, String email) throws Exception {
        mvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"uuid":"%s","username":"%s","name":"%s","phone":"+5583999999999"}
                                """.formatted(uid, username, email)))
                .andExpect(status().isCreated());
    }

    private UUID userIdOf(String authUuid) {
        return UUID.fromString(jdbc.queryForObject(
                "SELECT CAST(u.id AS varchar) FROM users u JOIN auth a ON a.id = u.auth_id WHERE a.uuid = ?",
                String.class, authUuid));
    }

    private UUID insertTrip(UUID userId, String title, String startsOn, String endsOn) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO trips (id, user_id, destination_id, title, starts_on, ends_on,
                    pace_code, transport_mode_code, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                """, id, userId, UUID.fromString("20000000-0000-0000-0000-000000000001"),
                title, startsOn, endsOn, null, null);
        return id;
    }

    private record TripRow(String title, String startsOn, String endsOn) {
    }

    private TripRow tripRow(UUID id) {
        return jdbc.queryForObject("""
                SELECT title, CAST(starts_on AS varchar), CAST(ends_on AS varchar)
                FROM trips WHERE id = ?
                """, (rs, rowNum) -> new TripRow(rs.getString(1), rs.getString(2),
                rs.getString(3)), id);
    }

    private ResultActions patchTrip(String token, UUID tripId, String body) throws Exception {
        return mvc.perform(patch("/trips/" + tripId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void patchTrip_partialTitle_updatesOnlyTitle() throws Exception {
        patchTrip(ALICE_TOKEN, aliceTripId, "{\"title\":\"Alice renamed trip\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceTripId.toString()))
                .andExpect(jsonPath("$.title").value("Alice renamed trip"))
                .andExpect(jsonPath("$.startsOn").value("2030-01-10"))
                .andExpect(jsonPath("$.endsOn").value("2030-01-12"))
                .andExpect(jsonPath("$.durationDays").value(3));

        var before = tripRow(aliceTripId);
        assertThat(before.startsOn()).isEqualTo("2030-01-10");
        assertThat(before.endsOn()).isEqualTo("2030-01-12");
    }

    @Test
    void patchTrip_onlyNewStart_shiftsEndPreservingDuration() throws Exception {
        patchTrip(ALICE_TOKEN, aliceTripId, "{\"startsOn\":\"2030-03-01\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startsOn").value("2030-03-01"))
                .andExpect(jsonPath("$.endsOn").value("2030-03-03"))
                .andExpect(jsonPath("$.durationDays").value(3));

        assertThat(tripRow(aliceTripId).endsOn()).isEqualTo("2030-03-03");
    }

    @Test
    void patchTrip_onlyNewEnd_keepsStartAndDurationResultsFromDifference() throws Exception {
        patchTrip(ALICE_TOKEN, aliceTripId, "{\"endsOn\":\"2030-01-20\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startsOn").value("2030-01-10"))
                .andExpect(jsonPath("$.endsOn").value("2030-01-20"))
                .andExpect(jsonPath("$.durationDays").value(11));

        assertThat(tripRow(aliceTripId).startsOn()).isEqualTo("2030-01-10");
        assertThat(tripRow(aliceTripId).endsOn()).isEqualTo("2030-01-20");
    }

    @Test
    void patchTrip_newDuration_setsEndFromNewStart() throws Exception {
        patchTrip(ALICE_TOKEN, aliceTripId,
                "{\"startsOn\":\"2030-03-01\",\"durationDays\":5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startsOn").value("2030-03-01"))
                .andExpect(jsonPath("$.endsOn").value("2030-03-05"))
                .andExpect(jsonPath("$.durationDays").value(5));

        assertThat(tripRow(aliceTripId).endsOn()).isEqualTo("2030-03-05");
    }

    @Test
    void patchTrip_durationOnly_setsEndFromCurrentStart() throws Exception {
        patchTrip(ALICE_TOKEN, aliceTripId, "{\"durationDays\":7}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startsOn").value("2030-01-10"))
                .andExpect(jsonPath("$.endsOn").value("2030-01-16"))
                .andExpect(jsonPath("$.durationDays").value(7));
    }

    @Test
    void patchTrip_datesWithConflictingDuration_returns400() throws Exception {
        patchTrip(ALICE_TOKEN, aliceTripId,
                "{\"startsOn\":\"2030-03-01\",\"endsOn\":\"2030-03-05\",\"durationDays\":3}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Trip period is invalid"));

        assertThat(tripRow(aliceTripId).startsOn()).isEqualTo("2030-01-10");
        assertThat(tripRow(aliceTripId).endsOn()).isEqualTo("2030-01-12");
    }

    @Test
    void patchTrip_datesWithCompatibleDuration_isAccepted() throws Exception {
        patchTrip(ALICE_TOKEN, aliceTripId,
                "{\"startsOn\":\"2030-03-01\",\"endsOn\":\"2030-03-05\",\"durationDays\":5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endsOn").value("2030-03-05"))
                .andExpect(jsonPath("$.durationDays").value(5));
    }

    @Test
    void patchTrip_endBeforeStart_returns400() throws Exception {
        patchTrip(ALICE_TOKEN, aliceTripId, "{\"endsOn\":\"2030-01-01\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Trip period is invalid"));
    }

    @Test
    void patchTrip_unknownTripId_returns404() throws Exception {
        patchTrip(ALICE_TOKEN, UUID.randomUUID(), "{\"title\":\"Ghost trip\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Trip not found"));
    }

    @Test
    void patchTrip_otherUsersTrip_returns403AndKeepsData() throws Exception {
        patchTrip(ALICE_TOKEN, bobTripId, "{\"title\":\"Hacked trip\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not own this trip"));

        assertThat(tripRow(bobTripId).title()).isEqualTo("Bob trip");
    }

    @Test
    void patchTrip_blankTitle_returns400AndKeepsData() throws Exception {
        patchTrip(ALICE_TOKEN, aliceTripId, "{\"title\":\"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request body"));

        assertThat(tripRow(aliceTripId).title()).isEqualTo("Alice trip");
    }

    @Test
    void patchTrip_nonPositiveDuration_returns400() throws Exception {
        patchTrip(ALICE_TOKEN, aliceTripId, "{\"durationDays\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request body"));
    }

    @Test
    void patchTrip_withoutToken_returns401() throws Exception {
        mvc.perform(patch("/trips/" + aliceTripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"No token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Firebase Bearer token is required"));
    }

    @Test
    void patchTrip_updatesDestinationId() throws Exception {
        UUID newDestinationId = UUID.randomUUID();
        patchTrip(ALICE_TOKEN, aliceTripId,
                "{\"destinationId\":\"%s\"}".formatted(newDestinationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinationId").value(newDestinationId.toString()));

        String stored = jdbc.queryForObject(
                "SELECT CAST(destination_id AS varchar) FROM trips WHERE id = ?",
                String.class, aliceTripId);
        assertThat(stored).isEqualTo(newDestinationId.toString());
    }
}
