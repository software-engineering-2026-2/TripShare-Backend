package ufcg.software.engineering.trip.share.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:auth-flow;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthIntegrationTest.ProbeConfig.class)
class AuthIntegrationTest {
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper mapper;
    @MockitoBean FirebaseApp firebaseApp;
    @MockitoBean FirebaseAuth firebaseAuth;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("DELETE FROM user_sessions");
        jdbc.update("DELETE FROM users");
        jdbc.update("INSERT INTO users(id, firebase_uid, username, name, email, role, status, created_at, updated_at, version) "
                + "VALUES (?, 'firebase-uid', 'alex', 'Alex', 'local@example.com', 'USER', 'ACTIVE', ?, ?, 0)", USER_ID,
                Instant.now(), Instant.now());
        var identity = mock(FirebaseToken.class);
        when(identity.getUid()).thenReturn("firebase-uid");
        when(identity.getEmail()).thenReturn("local@example.com");
        when(firebaseAuth.verifyIdToken("firebase-token", true)).thenReturn(identity);
    }

    private JsonNode login() throws Exception {
        var result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"FIREBASE\",\"idToken\":\"firebase-token\"}"))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void login_shouldReturnLocalUuidAndStoreOnlyHash() throws Exception {
        var response = login();
        String raw = response.get("accessToken").asText();
        assertThat(response.at("/user/id").asText()).isEqualTo(USER_ID.toString());
        assertThat(response.toString()).doesNotContain("firebase-uid", "firebase-token", "refreshToken");
        assertThat(jdbc.queryForObject("SELECT token_hash FROM user_sessions", String.class)).hasSize(64).isNotEqualTo(raw);
        mvc.perform(get("/test/whoami").header("Authorization", "Bearer " + raw))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(USER_ID.toString()));
    }

    @Test
    void logout_shouldRevokeCurrentSessionAndPreserveOtherSession() throws Exception {
        String first = login().get("accessToken").asText();
        String second = login().get("accessToken").asText();
        mvc.perform(post("/auth/logout").header("Authorization", "Bearer " + first)).andExpect(status().isNoContent());
        mvc.perform(get("/test/whoami").header("Authorization", "Bearer " + first)).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/logout").header("Authorization", "Bearer " + first)).andExpect(status().isUnauthorized());
        mvc.perform(get("/test/whoami").header("Authorization", "Bearer " + second)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_sessions WHERE revoked_at IS NOT NULL", Long.class)).isEqualTo(1);
    }

    @Test
    void session_expiredOrAccountSuspended_shouldRequireAuthentication() throws Exception {
        String token = login().get("accessToken").asText();
        jdbc.update("UPDATE user_sessions SET expires_at = ?", Instant.now().minusSeconds(1));
        mvc.perform(get("/test/whoami").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        token = login().get("accessToken").asText();
        jdbc.update("UPDATE users SET status = 'SUSPENDED'");
        mvc.perform(get("/test/whoami").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void login_uidMissingEvenWithMatchingEmail_shouldNotLinkAutomatically() throws Exception {
        jdbc.update("UPDATE users SET firebase_uid = NULL");
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"FIREBASE\",\"idToken\":\"firebase-token\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Credenciais inválidas."));
        assertThat(jdbc.queryForObject("SELECT firebase_uid FROM users", String.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_sessions", Long.class)).isZero();
    }

    @Test
    void login_suspendedAccount_shouldReturnSameGeneric401() throws Exception {
        jdbc.update("UPDATE users SET status = 'SUSPENDED'");
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"FIREBASE\",\"idToken\":\"firebase-token\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Credenciais inválidas."));
    }

    @Test
    void login_sameEmailWithDifferentUid_shouldNotChangeExistingIdentity() throws Exception {
        jdbc.update("UPDATE users SET firebase_uid = 'another-firebase-uid'");
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"FIREBASE\",\"idToken\":\"firebase-token\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT firebase_uid FROM users", String.class)).isEqualTo("another-firebase-uid");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_sessions", Long.class)).isZero();
    }

    @Test
    void protectedEndpoint_firebaseIdToken_shouldNotBypassSession() throws Exception {
        mvc.perform(get("/test/whoami").header("Authorization", "Bearer firebase-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openApi_shouldDocumentBothLoginMethodsAndOpaqueBearerLogout() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var spec = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(spec.at("/paths/~1auth~1login/post/responses/200").isMissingNode()).isFalse();
        assertThat(spec.at("/paths/~1auth~1logout/post/responses/204").isMissingNode()).isFalse();
        assertThat(spec.at("/paths/~1auth~1logout/post/security/0/sessionBearer").isArray()).isTrue();
        assertThat(spec.at("/components/securitySchemes/sessionBearer/scheme").asText()).isEqualTo("bearer");
        assertThat(spec.at("/components/schemas/LoginRequest/oneOf").size()).isEqualTo(2);
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @TestConfiguration
    static class ProbeConfig {
        @Bean ProbeController probeController() { return new ProbeController(); }
    }

    @RestController
    static class ProbeController {
        @GetMapping("/test/whoami")
        Map<String, String> whoami(Authentication authentication) { return Map.of("id", authentication.getName()); }
    }
}
