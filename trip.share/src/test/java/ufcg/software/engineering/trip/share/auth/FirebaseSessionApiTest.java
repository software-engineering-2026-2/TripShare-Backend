package ufcg.software.engineering.trip.share.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ufcg.software.engineering.trip.share.service.AuthenticatedUser;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FirebaseSessionApiTest {
    private static final String ORIGIN = "http://localhost:3000";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean FirebaseTokenService tokenService;
    @MockitoBean FirebaseAuth firebaseAuth;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM auth");
        when(tokenService.validate("id-token"))
                .thenReturn(new AuthenticatedUser("firebase-uid", "traveler@example.invalid"));
        when(firebaseAuth.createSessionCookie(eq("id-token"), any())).thenReturn("firebase-session-cookie");
    }

    @Test
    void registerAndLogin_issueHttpOnlySessionCookieWithoutChangingBody() throws Exception {
        mvc.perform(post("/auth/register")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer id-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"uuid":"firebase-uid","username":"traveler","name":"Traveler"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("tripshare_session=firebase-session-cookie"),
                                org.hamcrest.Matchers.containsString("Path=/"),
                                org.hamcrest.Matchers.containsString("Max-Age=432000"),
                                org.hamcrest.Matchers.containsString("HttpOnly"),
                                org.hamcrest.Matchers.containsString("SameSite=Lax"))))
                .andExpect(jsonPath("$.uuid").value("firebase-uid"))
                .andExpect(jsonPath("$.token").doesNotExist());

        String userId = jdbc.queryForObject("SELECT CAST(id AS varchar) FROM users", String.class);
        mvc.perform(post("/auth/login")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer id-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uuid\":\"firebase-uid\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("HttpOnly")))
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.token").doesNotExist());
        verify(firebaseAuth, times(2)).createSessionCookie(eq("id-token"), any());
    }

    @Test
    void protectedEndpoint_acceptsSessionCookie() throws Exception {
        mvc.perform(post("/auth/register")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer id-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"uuid":"firebase-uid","username":"traveler","name":"Traveler"}
                                """))
                .andExpect(status().isCreated());
        String userId = jdbc.queryForObject("SELECT CAST(id AS varchar) FROM users", String.class);
        when(tokenService.validateSessionCookie("firebase-session-cookie", true))
                .thenReturn(new AuthenticatedUser("firebase-uid", "traveler@example.invalid"));

        mvc.perform(get("/users/" + userId).cookie(
                        new jakarta.servlet.http.Cookie("tripshare_session", "firebase-session-cookie")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("traveler"));
        verify(tokenService).validateSessionCookie("firebase-session-cookie", true);
    }

    @Test
    void invalidSessionCookie_returns401AndDoesNotFallBackToBearer() throws Exception {
        when(tokenService.validateSessionCookie("invalid-cookie", true))
                .thenThrow(new BadCredentialsException("Invalid Firebase session cookie"));

        mvc.perform(get("/users/" + java.util.UUID.randomUUID())
                        .cookie(new jakarta.servlet.http.Cookie("tripshare_session", "invalid-cookie"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer id-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verify(tokenService, never()).validate("id-token");
    }

    @Test
    void bearerFallback_worksWhenCookieIsAbsent() throws Exception {
        when(tokenService.validate("legacy-token"))
                .thenReturn(new AuthenticatedUser("firebase-uid", "traveler@example.invalid"));

        mvc.perform(get("/users/" + java.util.UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer legacy-token"))
                .andExpect(status().isNotFound());
        verify(tokenService).validate("legacy-token");
    }

    @Test
    void suspendedAccountStillReturns403OnLogin() throws Exception {
        mvc.perform(post("/auth/register")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer id-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"uuid":"firebase-uid","username":"traveler","name":"Traveler"}
                                """))
                .andExpect(status().isCreated());
        jdbc.update("UPDATE users SET status='SUSPENDED'");

        mvc.perform(post("/auth/login")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer id-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uuid\":\"firebase-uid\"}"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void logoutClearsCookieAndRevokesRefreshTokensWhenCookieIsValid() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("firebase-uid");
        when(firebaseAuth.verifySessionCookie("firebase-session-cookie", true)).thenReturn(token);

        mvc.perform(post("/auth/logout")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .cookie(new jakarta.servlet.http.Cookie("tripshare_session", "firebase-session-cookie")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("tripshare_session="),
                                org.hamcrest.Matchers.containsString("Path=/"),
                                org.hamcrest.Matchers.containsString("Max-Age=0"),
                                org.hamcrest.Matchers.containsString("HttpOnly"),
                                org.hamcrest.Matchers.containsString("SameSite=Lax"))));
        verify(firebaseAuth).revokeRefreshTokens("firebase-uid");
    }

    @Test
    void logoutIsIdempotentWithoutCookie() throws Exception {
        mvc.perform(post("/auth/logout").header(HttpHeaders.ORIGIN, ORIGIN))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("Max-Age=0")));
        verifyNoInteractions(firebaseAuth);
    }

    @Test
    void logoutIsIdempotentWithInvalidCookie() throws Exception {
        when(firebaseAuth.verifySessionCookie("invalid-cookie", true))
                .thenThrow(new FirebaseAuthException(
                        com.google.firebase.ErrorCode.INVALID_ARGUMENT,
                        "invalid",
                        null,
                        null,
                        com.google.firebase.auth.AuthErrorCode.INVALID_SESSION_COOKIE));

        mvc.perform(post("/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("tripshare_session", "invalid-cookie")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("Max-Age=0")));
        verify(firebaseAuth, never()).revokeRefreshTokens(anyString());
    }

}
