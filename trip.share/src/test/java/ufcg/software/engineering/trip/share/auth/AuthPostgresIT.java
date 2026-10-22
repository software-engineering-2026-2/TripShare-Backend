package ufcg.software.engineering.trip.share.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import ufcg.software.engineering.trip.share.dto.LoginRequest;
import ufcg.software.engineering.trip.share.dto.LoginResponse;
import ufcg.software.engineering.trip.share.service.AuthService;
import ufcg.software.engineering.trip.share.service.SessionService;
import ufcg.software.engineering.trip.share.service.SessionTokenService;

@SpringBootTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthIntegrationTest.ProbeConfig.class)
class AuthPostgresIT {
    private static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:16-alpine");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired SessionService sessions;
    @Autowired SessionTokenService tokens;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper mapper;
    @MockitoBean FirebaseApp firebaseApp;
    @MockitoBean FirebaseAuth firebaseAuth;
    @MockitoBean Clock clock;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        DATABASE.start(); // Docker obrigatório; não ignorar ausência do daemon.
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
    }

    @AfterAll
    static void stopDatabase() { DATABASE.stop(); }

    @BeforeEach
    void setUp() throws Exception {
        when(clock.instant()).thenReturn(NOW);
        jdbc.update("DELETE FROM user_sessions");
        jdbc.update("DELETE FROM users");
        jdbc.update("INSERT INTO users(id, firebase_uid, username, name, email) VALUES (?, 'firebase-uid', 'alex', 'Alex', 'alex@example.invalid')", USER_ID);
        var identity = mock(FirebaseToken.class);
        when(identity.getUid()).thenReturn("firebase-uid");
        when(firebaseAuth.verifyIdToken("firebase-token", true)).thenReturn(identity);
    }

    private LoginResponse login() {
        return auth.login(new LoginRequest(LoginRequest.Method.FIREBASE, null, null, "firebase-token"));
    }

    @Test
    void login_shouldUseMigratedSchemaAndPersistOnlyHash() {
        var response = login();
        assertThat(response.user().id()).isEqualTo(USER_ID);
        assertThat(response.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(8)));
        assertThat(jdbc.queryForObject("SELECT token_hash FROM user_sessions", String.class)).isEqualTo(tokens.hash(response.accessToken()));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success AND version = '7'", Long.class)).isEqualTo(1);
    }

    @Test
    void concurrentLogins_shouldKeepIndependentSessionsAndNotChangeUser() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<LoginResponse>> calls = List.of(this::login, this::login);
            var results = executor.invokeAll(calls);
            var first = results.get(0).get();
            var second = results.get(1).get();
            assertThat(first.accessToken()).isNotEqualTo(second.accessToken());
            assertThat(sessions.authenticate(first.accessToken()).userId()).isEqualTo(USER_ID);
            assertThat(sessions.authenticate(second.accessToken()).userId()).isEqualTo(USER_ID);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM user_sessions", Long.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT version FROM users WHERE id = ?", Long.class, USER_ID)).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void logout_shouldCommitRevocationAndPreserveOtherSession() throws Exception {
        String first = login().accessToken();
        String second = login().accessToken();
        mvc.perform(post("/auth/logout").header("Authorization", "Bearer " + first)).andExpect(status().isNoContent());
        mvc.perform(get("/test/whoami").header("Authorization", "Bearer " + first)).andExpect(status().isUnauthorized());
        mvc.perform(get("/test/whoami").header("Authorization", "Bearer " + second)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_sessions WHERE revoked_at IS NOT NULL", Long.class)).isEqualTo(1);
    }

    @Test
    void expiredSession_shouldRequireNewLoginAtExactBoundary() throws Exception {
        String token = login().accessToken();
        when(clock.instant()).thenReturn(NOW.plus(Duration.ofHours(8)));
        mvc.perform(get("/test/whoami").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("INVALID_SESSION"));
    }

    @Test
    void suspension_shouldDenyBothExistingSessionAndNewLogin() throws Exception {
        String token = login().accessToken();
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE id = ?", USER_ID);
        mvc.perform(get("/test/whoami").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"FIREBASE\",\"idToken\":\"firebase-token\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Credenciais inválidas."));
    }

    @Test
    void userDeletion_shouldCascadeSessions() {
        login();
        jdbc.update("DELETE FROM users WHERE id = ?", USER_ID);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_sessions", Long.class)).isZero();
    }

    @Test
    void firebaseUid_shouldBeUniqueAndNullableForExistingAccounts() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO users(username, name, email, firebase_uid) "
                + "VALUES ('other', 'Other', 'other@example.invalid', 'firebase-uid')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("INSERT INTO users(username, name, email) VALUES ('unlinked', 'Unlinked', 'unlinked@example.invalid')");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE firebase_uid IS NULL", Long.class)).isEqualTo(1);
    }
}
