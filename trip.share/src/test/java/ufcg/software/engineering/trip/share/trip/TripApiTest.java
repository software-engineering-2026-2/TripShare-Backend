package ufcg.software.engineering.trip.share.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ufcg.software.engineering.trip.share.models.Destination;
import ufcg.software.engineering.trip.share.models.TripDay;
import ufcg.software.engineering.trip.share.repository.DestinationRepository;
import ufcg.software.engineering.trip.share.repository.TripDayRepository;
import ufcg.software.engineering.trip.share.service.FirebaseIdentity;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DestinationRepository destinationRepository;
    @MockitoBean private FirebaseTokenService tokenService;
    @MockitoSpyBean private TripDayRepository tripDayRepository;

    private UUID destinationId;

    @BeforeEach
    void prepare() throws Exception {
        jdbc.update("DELETE FROM trip_days");
        jdbc.update("DELETE FROM trips");
        jdbc.update("DELETE FROM destinations");
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM auth");

        when(tokenService.validate("valid-token"))
                .thenReturn(new FirebaseIdentity("firebase-uid", "user@example.invalid"));
        mvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"uuid":"firebase-uid","username":"traveler","name":"Test User"}
                                """))
                .andExpect(status().isCreated());

        Destination savedDestination = destinationRepository.saveAndFlush(
                Destination.create("Rio de Janeiro", "Rio de Janeiro", "BR", "America/Sao_Paulo", "DEMO", "rio-br"));
        destinationId = savedDestination.getId();
    }

    private ResultActions postTrip(String token, String body) throws Exception {
        return mvc.perform(post("/trips")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions postTrip(String body) throws Exception {
        return postTrip("valid-token", body);
    }

    @Test
    void createTrip_validMultiDay_shouldReturn201WithDurationAndTrimmedTitle() throws Exception {
        UUID expectedUserId = jdbc.queryForObject(
                "SELECT id FROM users WHERE username = 'traveler'", UUID.class);

        postTrip("""
                {
                  "title": "  Férias no Rio  ",
                  "destinationId": "%s",
                  "startsOn": "2027-01-10",
                  "endsOn": "2027-01-14"
                }
                """.formatted(destinationId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(expectedUserId.toString()))
                .andExpect(jsonPath("$.destinationId").value(destinationId.toString()))
                .andExpect(jsonPath("$.destinationName").value("Rio de Janeiro"))
                .andExpect(jsonPath("$.destinationRegion").value("Rio de Janeiro"))
                .andExpect(jsonPath("$.destinationCountryCode").value("BR"))
                .andExpect(jsonPath("$.title").value("Férias no Rio"))
                .andExpect(jsonPath("$.startsOn").value("2027-01-10"))
                .andExpect(jsonPath("$.endsOn").value("2027-01-14"))
                .andExpect(jsonPath("$.durationDays").value(5))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM trips", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT title FROM trips", String.class)).isEqualTo("Férias no Rio");
        assertThat(jdbc.queryForObject("SELECT user_id FROM trips", UUID.class)).isEqualTo(expectedUserId);
    }

    @Test
    void createTrip_sameStartAndEndDate_shouldReturnDurationOfOneDay() throws Exception {
        postTrip("""
                {
                  "title": "Bate e volta",
                  "destinationId": "%s",
                  "startsOn": "2027-05-01",
                  "endsOn": "2027-05-01"
                }
                """.formatted(destinationId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.durationDays").value(1));
    }

    @Test
    void createTrip_onlyDurationDaysWithoutDates_shouldReturn201WithNullDates() throws Exception {
        postTrip("""
                {
                  "title": "Roteiro de 5 dias no Rio",
                  "destinationId": "%s",
                  "durationDays": 5
                }
                """.formatted(destinationId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Roteiro de 5 dias no Rio"))
                .andExpect(jsonPath("$.startsOn").isEmpty())
                .andExpect(jsonPath("$.endsOn").isEmpty())
                .andExpect(jsonPath("$.durationDays").value(5));

        assertThat(jdbc.queryForObject("SELECT duration_days FROM trips", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT starts_on FROM trips", Object.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT ends_on FROM trips", Object.class)).isNull();
    }

    @Test
    void createTrip_endDateBeforeStartDate_shouldReturn400WithoutSaving() throws Exception {
        postTrip("""
                {
                  "title": "Viagem inválida",
                  "destinationId": "%s",
                  "startsOn": "2027-01-15",
                  "endsOn": "2027-01-10"
                }
                """.formatted(destinationId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("End date cannot be before start date"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM trips", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {"title":"Viagem","destinationId":"%s"}
            """,
            """
            {"title":"Viagem","destinationId":"%s","durationDays":0}
            """,
            """
            {"title":"Viagem","destinationId":"%s","durationDays":-3}
            """,
            """
            {"title":"Viagem","destinationId":"%s","startsOn":"2027-01-10"}
            """,
            """
            {"title":"Viagem","destinationId":"%s","endsOn":"2027-01-12"}
            """,
            """
            {"title":"Viagem","destinationId":"%s","startsOn":"2027-01-10","endsOn":"2027-01-12","durationDays":5}
            """
    })
    void createTrip_invalidDurationOrIncompleteDates_shouldReturn400WithoutSaving(String template) throws Exception {
        postTrip(template.formatted(destinationId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Trip must specify a valid date range or a positive duration in days"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM trips", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void createTrip_blankTitle_shouldReturn400(String blankTitle) throws Exception {
        postTrip("""
                {
                  "title": "%s",
                  "destinationId": "%s",
                  "startsOn": "2027-01-10",
                  "endsOn": "2027-01-12"
                }
                """.formatted(blankTitle, destinationId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM trips", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {"destinationId":"00000000-0000-0000-0000-000000000001","startsOn":"2027-01-10","endsOn":"2027-01-12"}
            """,
            """
            {"title":"Viagem","startsOn":"2027-01-10","endsOn":"2027-01-12"}
            """,
            """
            {"title":"Viagem","destinationId":"00000000-0000-0000-0000-000000000001","startsOn":"invalid-date","endsOn":"2027-01-12"}
            """
    })
    void createTrip_missingOrMalformedRequiredFields_shouldReturn400(String invalidBody) throws Exception {
        postTrip(invalidBody)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM trips", Integer.class)).isZero();
    }

    @Test
    void createTrip_unknownDestination_shouldReturn404WithoutSaving() throws Exception {
        UUID unknownDestinationId = UUID.randomUUID();

        postTrip("""
                {
                  "title": "Viagem",
                  "destinationId": "%s",
                  "startsOn": "2027-01-10",
                  "endsOn": "2027-01-12"
                }
                """.formatted(unknownDestinationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Destination not found"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM trips", Integer.class)).isZero();
    }

    @Test
    void createTrip_withoutToken_shouldReturn401() throws Exception {
        mvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Viagem",
                                  "destinationId": "%s",
                                  "startsOn": "2027-01-10",
                                  "endsOn": "2027-01-12"
                                }
                                """.formatted(destinationId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createTrip_unregisteredUser_shouldReturn404() throws Exception {
        when(tokenService.validate("unregistered-token"))
                .thenReturn(new FirebaseIdentity("unknown-uid", "unknown@example.invalid"));

        postTrip("unregistered-token", """
                {
                  "title": "Viagem",
                  "destinationId": "%s",
                  "startsOn": "2027-01-10",
                  "endsOn": "2027-01-12"
                }
                """.formatted(destinationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User profile not found"));
    }

    @Test
    void createTrip_suspendedUser_shouldReturn403() throws Exception {
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE username = 'traveler'");

        postTrip("""
                {
                  "title": "Viagem",
                  "destinationId": "%s",
                  "startsOn": "2027-01-10",
                  "endsOn": "2027-01-12"
                }
                """.formatted(destinationId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is suspended"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM trips", Integer.class)).isZero();
    }

    @Test
    void createTrip_dateRange_shouldCreateOneEmptyDayForEachDayOfPeriod() throws Exception {
        postTrip("""
                {
                  "title": "Quatro dias",
                  "destinationId": "%s",
                  "startsOn": "2026-11-10",
                  "endsOn": "2026-11-13"
                }
                """.formatted(destinationId))
                .andExpect(status().isCreated());

        UUID tripId = jdbc.queryForObject("SELECT id FROM trips", UUID.class);
        assertThat(jdbc.queryForList(
                "SELECT day_number FROM trip_days WHERE trip_id = ? ORDER BY day_number",
                Integer.class, tripId)).containsExactly(1, 2, 3, 4);
        assertThat(jdbc.queryForObject(
                "SELECT count(DISTINCT day_number) FROM trip_days WHERE trip_id = ?",
                Integer.class, tripId)).isEqualTo(4);
        assertThat(tripDayRepository.findByTripIdOrderByDayNumberAsc(tripId))
                .extracting(TripDay::getDate)
                .containsExactly(
                        LocalDate.of(2026, 11, 10),
                        LocalDate.of(2026, 11, 11),
                        LocalDate.of(2026, 11, 12),
                        LocalDate.of(2026, 11, 13));
    }

    @Test
    void createTrip_onlyDurationDays_shouldCreateOneEmptyDayForEachDayOfDuration() throws Exception {
        postTrip("""
                {
                  "title": "Três dias",
                  "destinationId": "%s",
                  "durationDays": 3
                }
                """.formatted(destinationId))
                .andExpect(status().isCreated());

        UUID tripId = jdbc.queryForObject("SELECT id FROM trips", UUID.class);
        assertThat(jdbc.queryForList(
                "SELECT day_number FROM trip_days WHERE trip_id = ? ORDER BY day_number",
                Integer.class, tripId)).containsExactly(1, 2, 3);
    }

    @Test
    void createTrip_sameDay_shouldCreateSingleDay() throws Exception {
        postTrip("""
                {
                  "title": "Bate e volta",
                  "destinationId": "%s",
                  "startsOn": "2026-11-10",
                  "endsOn": "2026-11-10"
                }
                """.formatted(destinationId))
                .andExpect(status().isCreated());

        UUID tripId = jdbc.queryForObject("SELECT id FROM trips", UUID.class);
        assertThat(jdbc.queryForList(
                "SELECT day_number FROM trip_days WHERE trip_id = ?",
                Integer.class, tripId)).containsExactly(1);
    }

    @Test
    void createTrip_duplicateDayForSameTrip_shouldBeRejected() throws Exception {
        postTrip("""
                {
                  "title": "Dois dias",
                  "destinationId": "%s",
                  "durationDays": 2
                }
                """.formatted(destinationId))
                .andExpect(status().isCreated());

        UUID tripId = jdbc.queryForObject("SELECT id FROM trips", UUID.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO trip_days (id, trip_id, day_number) VALUES (?, ?, ?)",
                UUID.randomUUID(), tripId, 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void createTrip_dayCreationFailure_shouldRollBackTrip() throws Exception {
        doThrow(new DataIntegrityViolationException("forced day failure"))
                .when(tripDayRepository).saveAll(any());

        postTrip("""
                {
                  "title": "Viagem revertida",
                  "destinationId": "%s",
                  "durationDays": 2
                }
                """.formatted(destinationId))
                .andExpect(status().isInternalServerError());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM trips", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM trip_days", Integer.class)).isZero();
    }
}
