package ufcg.software.engineering.trip.share.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ufcg.software.engineering.trip.share.service.FirebaseIdentity;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private FirebaseTokenService tokenService;

    @BeforeEach
    void prepare() {
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM auth");
        when(tokenService.validate("valid-token"))
                .thenReturn(new FirebaseIdentity("firebase-uid", "user@example.invalid"));
    }

    private ResultActions request(String endpoint, String body) throws Exception {
        return mvc.perform(post("/auth/" + endpoint).header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions register() throws Exception {
        return request("register", """
                {"uuid":"firebase-uid","username":"traveler","name":"Test User",
                 "phone":"+5583999999999","bio":"Traveler","hometown":"Campina Grande"}
                """);
    }

    @Test
    void registerAndLogin_validToken_shouldReturnPersistedProfile() throws Exception {
        register().andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty()).andExpect(jsonPath("$.authId").isNotEmpty())
                .andExpect(jsonPath("$.uuid").value("firebase-uid"))
                .andExpect(jsonPath("$.email").value("user@example.invalid"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty()).andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.token").doesNotExist());
        String id = jdbc.queryForObject("SELECT CAST(id AS varchar) FROM users", String.class);
        request("login", "{\"uuid\":\"firebase-uid\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.username").value("traveler"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.phone").value("+5583999999999"))
                .andExpect(jsonPath("$.bio").value("Traveler"))
                .andExpect(jsonPath("$.hometown").value("Campina Grande"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth", Integer.class)).isEqualTo(1);
    }

    @Test
    void login_unknownUser_shouldReturn404WithoutCreatingProfile() throws Exception {
        request("login", "{\"uuid\":\"firebase-uid\"}").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth", Integer.class)).isZero();
    }

    @Test
    void login_suspendedUser_shouldReturn403() throws Exception {
        register().andExpect(status().isCreated());
        jdbc.update("UPDATE users SET status = 'SUSPENDED'");
        request("login", "{\"uuid\":\"firebase-uid\"}").andExpect(status().isForbidden());
    }

    @Test
    void loginAndRegister_mismatchedUid_shouldReturn401WithoutDatabaseWrites() throws Exception {
        request("login", "{\"uuid\":\"another-uid\"}").andExpect(status().isUnauthorized());
        request("register", "{\"uuid\":\"another-uid\",\"username\":\"test\",\"name\":\"Test\"}")
                .andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth", Integer.class)).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Basic abc", "Bearer ", "broken"})
    void login_missingOrMalformedHeader_shouldReturn401(String header) throws Exception {
        var request = post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"uuid\":\"firebase-uid\"}");
        if (header != null) request.header("Authorization", header);
        mvc.perform(request).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(tokenService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "expired"})
    void login_invalidOrExpiredToken_shouldReturn401(String reason) throws Exception {
        when(tokenService.validate("valid-token")).thenThrow(new BadCredentialsException(reason));
        request("login", "{\"uuid\":\"firebase-uid\"}").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid Firebase token"));
    }

    @Test
    void login_firebaseUnavailable_shouldReturn503() throws Exception {
        when(tokenService.validate("valid-token")).thenThrow(new AuthenticationServiceException("downstream details"));
        request("login", "{\"uuid\":\"firebase-uid\"}").andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Firebase authentication unavailable"));
    }

    @Test
    void login_lowercaseBearer_shouldAcceptHeader() throws Exception {
        register().andExpect(status().isCreated());
        mvc.perform(post("/auth/login").header("Authorization", "bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"uuid\":\"firebase-uid\"}"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-an-email", "   ", " user@example.invalid"})
    void register_invalidFirebaseEmail_shouldReturn400WithoutWrites(String email) throws Exception {
        when(tokenService.validate("valid-token")).thenReturn(new FirebaseIdentity("firebase-uid", email));
        register().andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"uuid\":\"\"}", "{", "{\"uuid\":\"   \"}"})
    void login_invalidBody_shouldReturn400(String body) throws Exception {
        request("login", body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"uuid\":\"firebase-uid\",\"username\":\"\",\"name\":\"Test\"}",
            "{\"uuid\":\"firebase-uid\",\"username\":\"test\",\"name\":\"   \"}",
            "{\"uuid\":\"firebase-uid\",\"username\":\"test\",\"name\":\"Test\",\"phone\":\"123\"}"})
    void register_invalidProfile_shouldReturn400WithoutWrites(String body) throws Exception {
        request("register", body).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth", Integer.class)).isZero();
    }

    @Test
    void register_duplicateUid_shouldReturn409() throws Exception {
        register().andExpect(status().isCreated());
        register().andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth", Integer.class)).isEqualTo(1);
    }

    @Test
    void register_duplicateUsernameIgnoringCase_shouldReturn409() throws Exception {
        register().andExpect(status().isCreated());
        when(tokenService.validate("valid-token")).thenReturn(new FirebaseIdentity("second-uid", "second@example.invalid"));
        request("register", "{\"uuid\":\"second-uid\",\"username\":\"TRAVELER\",\"name\":\"Second\"}")
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth", Integer.class)).isEqualTo(1);
    }

    @Test
    void register_duplicateEmailIgnoringCase_shouldReturn409() throws Exception {
        register().andExpect(status().isCreated());
        when(tokenService.validate("valid-token")).thenReturn(new FirebaseIdentity("second-uid", "USER@EXAMPLE.INVALID"));
        request("register", "{\"uuid\":\"second-uid\",\"username\":\"second\",\"name\":\"Second\"}")
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth", Integer.class)).isEqualTo(1);
    }

    @Test
    void register_clientRoleAndEmail_shouldUseTrustedDefaultsAndTokenEmail() throws Exception {
        request("register", """
                {"uuid":"firebase-uid","username":"test","name":"Test",
                 "email":"attacker@example.invalid","role":"ADMIN","status":"SUSPENDED"}
                """).andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@example.invalid"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
