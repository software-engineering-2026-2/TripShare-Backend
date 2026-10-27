package ufcg.software.engineering.trip.share.user;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private FirebaseTokenService tokenService;

    @BeforeEach
    void prepare() throws Exception {
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM auth");
        when(tokenService.validate("valid-token"))
                .thenReturn(new FirebaseIdentity("firebase-uid", "user@example.invalid"));
        register().andExpect(status().isCreated());
    }

    private ResultActions request(String endpoint, String body) throws Exception {
        return mvc.perform(post("/auth/" + endpoint).header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions patchMe(String body) throws Exception {
        return patchMe("valid-token", body);
    }

    private ResultActions patchMe(String token, String body) throws Exception {
        return mvc.perform(patch("/users/me").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions register() throws Exception {
        return request("register", """
                {"uuid":"firebase-uid","username":"traveler","name":"Test User",
                 "phone":"+5583999999999","bio":"Traveler","hometown":"Campina Grande"}
                """);
    }

    private String jsonOf(String field, String value) {
        return "{\"" + field + "\": \"" + value + "\"}";
    }

    private ResultActions registerOther() throws Exception {
        return mvc.perform(post("/auth/register").header("Authorization", "Bearer other-token")
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"uuid":"other-uid","username":"other","name":"Other User"}
                    """));
    }

    @Test
    void updateProfile_onlyName_shouldKeepOtherFields() throws Exception {
        patchMe("{\"name\": \"Novo Nome\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Novo Nome"))
                .andExpect(jsonPath("$.hometown").value("Campina Grande"));
    }

    @Test
    void updateProfile_forbiddenFields_shouldBeIgnored() throws Exception {
        patchMe("{\"name\": \"Outro\", \"role\": \"ADMIN\", \"status\": \"SUSPENDED\", \"email\": \"zin@x.com\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Outro"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.email").value("user@example.invalid"));
        assertThat(jdbc.queryForObject("SELECT role FROM users", String.class)).isEqualTo("USER");
        assertThat(jdbc.queryForObject("SELECT status FROM users", String.class)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT email FROM users", String.class)).isEqualTo("user@example.invalid");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  "})
    void updateProfile_blankName_shouldReturn400(String name) throws Exception {
        patchMe("{\"name\": \"" + name + "\"}")
                .andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("SELECT name FROM users", String.class)).isEqualTo("Test User");
    }

    @ParameterizedTest
    @CsvSource({"name,121", "bio,501", "hometown,121"})
    void updateProfile_exceedLimit_shouldReturn400(String field, int size) throws Exception {
        patchMe(jsonOf(field, "a".repeat(size)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_otherUser_shouldNotAffectFirstUser() throws Exception {
        when(tokenService.validate("other-token"))
                .thenReturn(new FirebaseIdentity("other-uid", "other@example.invalid"));
        registerOther().andExpect(status().isCreated());

        patchMe("other-token", "{\"name\": \"Changed\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("other"))
                .andExpect(jsonPath("$.name").value("Changed"));

        assertThat(jdbc.queryForObject(
                "SELECT name FROM users WHERE username = 'traveler'", String.class)).isEqualTo("Test User");
        assertThat(jdbc.queryForObject(
                "SELECT name FROM users WHERE username = 'other'", String.class)).isEqualTo("Changed");
    }
}
