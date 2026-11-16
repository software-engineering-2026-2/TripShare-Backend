package ufcg.software.engineering.trip.share.trip;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import ufcg.software.engineering.trip.share.models.Destination;
import ufcg.software.engineering.trip.share.models.Place;
import ufcg.software.engineering.trip.share.repository.DestinationRepository;
import ufcg.software.engineering.trip.share.repository.PlaceRepository;
import ufcg.software.engineering.trip.share.service.FirebaseIdentity;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripPlacesApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DestinationRepository destinationRepository;
    @Autowired private PlaceRepository placeRepository;
    @MockitoBean private FirebaseTokenService tokenService;

    private UUID destinationId;

    @BeforeEach
    void prepare() throws Exception {
        jdbc.update("DELETE FROM places");
        jdbc.update("DELETE FROM trip_days");
        jdbc.update("DELETE FROM trips");
        jdbc.update("DELETE FROM destinations");
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM auth");

        when(tokenService.validate("valid-token"))
                .thenReturn(new FirebaseIdentity("firebase-uid", "user@example.invalid"));
        when(tokenService.validate("other-token"))
                .thenReturn(new FirebaseIdentity("other-uid", "other@example.invalid"));
        when(tokenService.validate("suspended-token"))
                .thenReturn(new FirebaseIdentity("suspended-uid", "suspended@example.invalid"));

        register("valid-token", "firebase-uid", "traveler");
        register("other-token", "other-uid", "other");
        register("suspended-token", "suspended-uid", "suspended");

        destinationId = destinationRepository.saveAndFlush(
                Destination.create("Rio de Janeiro", "Rio de Janeiro", "BR", "America/Sao_Paulo", "DEMO", "rio-br"))
                .getId();
    }

    private void register(String token, String uid, String username) throws Exception {
        mvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"uuid":"%s","username":"%s","name":"Test User"}
                                """.formatted(uid, username)))
                .andExpect(status().isCreated());
    }

    private UUID createTrip(String token) throws Exception {
        String body = mvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Rio","destinationId":"%s","durationDays":3}
                                """.formatted(destinationId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));
    }

    private Place savePlace(String name) {
        Destination destination = destinationRepository.findById(destinationId).orElseThrow();
        return placeRepository.saveAndFlush(Place.create(destination, name, "DEMO"));
    }

    private ResultActions getPlaces(String token, UUID tripId, String query) throws Exception {
        return mvc.perform(get("/trips/" + tripId + "/places" + query)
                .header("Authorization", "Bearer " + token));
    }

    @Test
    void getPlaces_defaultPagination_shouldReturnCardsOrderedByName() throws Exception {
        UUID tripId = createTrip("valid-token");
        savePlace("Beta");
        Place rich = savePlace("Alpha");
        jdbc.update("""
                UPDATE places SET address = ?, neighborhood = ?, description = ?, latitude = ?,
                    longitude = ?, estimated_visit_minutes = ?, estimated_cost = ?, currency = ?
                WHERE id = ?
                """, "Rua Exemplo, 100", "Centro", "Ponto turístico",
                new BigDecimal("12.345678"), new BigDecimal("-54.123456"), 90,
                new BigDecimal("25.50"), "BRL", rich.getId());

        getPlaces("valid-token", tripId, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(rich.getId().toString()))
                .andExpect(jsonPath("$.content[0].name").value("Alpha"))
                .andExpect(jsonPath("$.content[0].address").value("Rua Exemplo, 100"))
                .andExpect(jsonPath("$.content[0].neighborhood").value("Centro"))
                .andExpect(jsonPath("$.content[0].description").value("Ponto turístico"))
                .andExpect(jsonPath("$.content[0].latitude").value(12.345678))
                .andExpect(jsonPath("$.content[0].longitude").value(-54.123456))
                .andExpect(jsonPath("$.content[0].estimatedVisitMinutes").value(90))
                .andExpect(jsonPath("$.content[0].estimatedCost").value(25.5))
                .andExpect(jsonPath("$.content[0].currency").value("BRL"))
                .andExpect(jsonPath("$.content[1].name").value("Beta"))
                .andExpect(jsonPath("$.content[1].address").isEmpty())
                .andExpect(jsonPath("$.content[1].estimatedCost").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getPlaces_secondPage_shouldRespectPageAndSize() throws Exception {
        UUID tripId = createTrip("valid-token");
        savePlace("Alpha");
        savePlace("Beta");
        Place gamma = savePlace("Gamma");

        getPlaces("valid-token", tripId, "?page=1&size=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(gamma.getId().toString()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getPlaces_shouldExcludeRemovedPlacesAndOtherDestinations() throws Exception {
        UUID tripId = createTrip("valid-token");
        Place removed = savePlace("Alpha");
        savePlace("Beta");
        jdbc.update("UPDATE places SET removed_at = CURRENT_TIMESTAMP WHERE id = ?", removed.getId());

        Destination otherDestination = destinationRepository.saveAndFlush(
                Destination.create("Salvador", "Bahia", "BR", "America/Bahia", "DEMO", "ssa-br"));
        placeRepository.saveAndFlush(Place.create(otherDestination, "Aardvark", "DEMO"));

        getPlaces("valid-token", tripId, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Beta"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getPlaces_emptyPage_shouldReturn200WithEmptyContent() throws Exception {
        UUID tripId = createTrip("valid-token");

        getPlaces("valid-token", tripId, "?page=2&size=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getPlaces_sizeAboveLimit_shouldClampTo100() throws Exception {
        UUID tripId = createTrip("valid-token");
        savePlace("Alpha");

        getPlaces("valid-token", tripId, "?size=500")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getPlaces_unknownTrip_shouldReturn404() throws Exception {
        getPlaces("valid-token", UUID.randomUUID(), "")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Trip not found"));
    }

    @Test
    void getPlaces_otherOwner_shouldReturn403() throws Exception {
        UUID tripId = createTrip("valid-token");

        getPlaces("other-token", tripId, "")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not own this trip"));
    }

    @Test
    void getPlaces_suspendedUser_shouldReturn403() throws Exception {
        UUID tripId = createTrip("suspended-token");
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE username = 'suspended'");

        getPlaces("suspended-token", tripId, "")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is suspended"));
    }

    @Test
    void getPlaces_invalidPagination_shouldReturn400() throws Exception {
        UUID tripId = createTrip("valid-token");

        getPlaces("valid-token", tripId, "?page=-1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid pagination parameters"));
        getPlaces("valid-token", tripId, "?size=0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid pagination parameters"));
    }

    @Test
    void getPlaces_withoutToken_shouldReturn401() throws Exception {
        UUID tripId = createTrip("valid-token");

        mvc.perform(get("/trips/" + tripId + "/places"))
                .andExpect(status().isUnauthorized());
    }
}
