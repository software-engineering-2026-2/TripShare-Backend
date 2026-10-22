package ufcg.software.engineering.trip.share.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ufcg.software.engineering.trip.share.controller.AuthController;
import ufcg.software.engineering.trip.share.dto.LoginResponse;
import ufcg.software.engineering.trip.share.exceptions.AuthException;
import ufcg.software.engineering.trip.share.handlers.ApiExceptionHandler;
import ufcg.software.engineering.trip.share.handlers.AuthProblemResponses;
import ufcg.software.engineering.trip.share.handlers.SessionAuthenticationFilter;
import ufcg.software.engineering.trip.share.security.SessionPrincipal;
import ufcg.software.engineering.trip.share.service.AuthService;
import ufcg.software.engineering.trip.share.service.SessionService;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthProblemResponses.class, ApiExceptionHandler.class})
class AuthSecurityMockMvcTest {
    @Autowired MockMvc mvc;
    @Autowired ApplicationContext context;
    @MockitoBean AuthService auth;
    @MockitoBean SessionService sessions;

    @Test
    void login_password_shouldBePublicAndReturnNoStore() throws Exception {
        when(auth.login(any())).thenReturn(new LoginResponse("opaque-session", "Bearer",
                Instant.parse("2026-10-22T20:00:00Z"), null));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"PASSWORD\",\"email\":\"alex@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.accessToken").value("opaque-session"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.idToken").doesNotExist());
        verifyNoInteractions(sessions);
    }

    @Test
    void login_firebase_shouldIgnoreOldAuthorizationHeader() throws Exception {
        mvc.perform(post("/auth/login").header("Authorization", "Bearer expired-session")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"FIREBASE\",\"idToken\":\"firebase-token\"}"))
                .andExpect(status().isOk());
        verifyNoInteractions(sessions);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{", "{\"method\":\"UNKNOWN\"}",
            "{\"method\":\"PASSWORD\",\"email\":\"invalid\",\"password\":\"password\"}",
            "{\"method\":\"PASSWORD\",\"email\":\"alex@example.com\",\"password\":\" \"}",
            "{\"method\":\"PASSWORD\",\"email\":\"alex@example.com\",\"password\":\"pw\",\"idToken\":\"token\"}",
            "{\"method\":\"FIREBASE\",\"idToken\":\" \"}",
            "{\"method\":\"FIREBASE\",\"idToken\":\"token\",\"email\":\"alex@example.com\"}"})
    void login_invalidBody_shouldReturn400WithoutCallingFirebase(String body) throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
        verifyNoInteractions(auth);
    }

    @Test
    void login_invalidCredentials_shouldReturnGenericProblem() throws Exception {
        when(auth.login(any())).thenThrow(AuthException.invalidCredentials());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"FIREBASE\",\"idToken\":\"invalid\"}"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("Credenciais inválidas."));
    }

    @Test
    void logout_withoutToken_shouldReturn401() throws Exception {
        mvc.perform(post("/auth/logout")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_SESSION"));
        verifyNoInteractions(auth);
    }

    @Test
    void logout_withValidSession_shouldReturn204AndNoHttpSession() throws Exception {
        when(sessions.authenticate("session-token"))
                .thenReturn(new SessionPrincipal(UUID.randomUUID(), UUID.randomUUID(), "USER"));
        mvc.perform(post("/auth/logout").header("Authorization", "bearer session-token"))
                .andExpect(status().isNoContent()).andExpect(content().string(""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(result -> assertThat(result.getRequest().getSession(false)).isNull());
    }

    @Test
    void logout_invalidOrExpiredSession_shouldReturn401() throws Exception {
        when(sessions.authenticate("invalid")).thenThrow(AuthException.invalidSession());
        mvc.perform(post("/auth/logout").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("INVALID_SESSION"));
        verifyNoInteractions(auth);
    }

    @Test
    void protectedWrite_missingToken_shouldReturn401() throws Exception {
        mvc.perform(post("/trips")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Basic dXNlcjpwYXNz", "Bearer", "Bearer "})
    void protectedRequest_malformedAuthorization_shouldReturn401(String authorization) throws Exception {
        when(sessions.authenticate("")).thenThrow(AuthException.invalidSession());
        mvc.perform(post("/auth/logout").header("Authorization", authorization))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("INVALID_SESSION"));
    }

    @Test
    void protectedRequest_databaseFailure_shouldNotReturnBadCredentials() throws Exception {
        when(sessions.authenticate("token")).thenThrow(new DataAccessResourceFailureException("private database details"));
        mvc.perform(post("/auth/logout").header("Authorization", "Bearer token"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.errorCode").value("DATABASE_UNAVAILABLE"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private"))));
    }

    @Test
    void login_unexpectedFailure_shouldReturnGeneric500() throws Exception {
        when(auth.login(any())).thenThrow(new IllegalStateException("private failure"));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"FIREBASE\",\"idToken\":\"token\"}"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private"))));
    }

    @Test
    void logout_deniedByApplication_shouldReturn403() throws Exception {
        when(sessions.authenticate("token")).thenReturn(new SessionPrincipal(UUID.randomUUID(), UUID.randomUUID(), "USER"));
        doThrow(new org.springframework.security.access.AccessDeniedException("private")).when(auth).logout(any());
        mvc.perform(post("/auth/logout").header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void securityFilter_shouldOnlyBeRegisteredInsideSecurityChain() {
        assertThat(context.getBeansOfType(SessionAuthenticationFilter.class)).isEmpty();
    }

    @Test
    void wrongMethod_shouldPreserve405ForAuthenticatedRequest() throws Exception {
        when(sessions.authenticate("token")).thenReturn(new SessionPrincipal(UUID.randomUUID(), UUID.randomUUID(), "USER"));
        mvc.perform(get("/auth/logout").header("Authorization", "Bearer token"))
                .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.errorCode").value("HTTP_405"));
    }
}
