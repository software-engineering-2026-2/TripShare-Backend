package ufcg.software.engineering.trip.share.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ufcg.software.engineering.trip.share.config.AuthProperties;
import ufcg.software.engineering.trip.share.exceptions.AuthException;
import ufcg.software.engineering.trip.share.models.User;
import ufcg.software.engineering.trip.share.models.UserSession;
import ufcg.software.engineering.trip.share.repository.UserRepository;
import ufcg.software.engineering.trip.share.repository.UserSessionRepository;
import ufcg.software.engineering.trip.share.security.SessionPrincipal;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");
    @Mock UserRepository users;
    @Mock UserSessionRepository sessions;
    private final SessionTokenService tokens = new SessionTokenService();
    private SessionService service;

    @BeforeEach
    void setUp() {
        service = new SessionService(users, sessions, tokens, new AuthProperties(Duration.ofHours(8)), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void create_activeAccount_shouldStoreOnlyHashAndExpireIn8Hours() {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(users.findByFirebaseUid("uid")).thenReturn(Optional.of(user));
        var response = service.create("uid");
        var session = ArgumentCaptor.forClass(UserSession.class);
        verify(sessions).save(session.capture());
        assertThat(session.getValue().getTokenHash()).isEqualTo(tokens.hash(response.accessToken())).hasSize(64);
        assertThat(session.getValue().getTokenHash()).isNotEqualTo(response.accessToken());
        assertThat(response.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(8)));
        assertThat(response.toString()).doesNotContain(response.accessToken());
    }

    @Test
    void create_missingUid_shouldNotCreateUserOrSession() {
        when(users.findByFirebaseUid("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create("unknown")).isInstanceOf(AuthException.class).hasMessage("Credenciais inválidas.");
        verify(users, never()).save(any());
        verify(sessions, never()).save(any());
    }

    @Test
    void create_suspendedAccount_shouldUseSameGenericError() {
        when(users.findByFirebaseUid("uid")).thenReturn(Optional.of(mock(User.class)));
        assertThatThrownBy(() -> service.create("uid")).isInstanceOf(AuthException.class).hasMessage("Credenciais inválidas.");
        verify(sessions, never()).save(any());
    }

    @Test
    void authenticate_validSession_shouldUseLocalIdAndCurrentRole() {
        String raw = tokens.generate();
        User user = mock(User.class);
        UUID userId = UUID.randomUUID();
        when(user.isActive()).thenReturn(true);
        when(user.getId()).thenReturn(userId);
        when(user.getRole()).thenReturn("ADMIN");
        when(sessions.findByTokenHash(tokens.hash(raw)))
                .thenReturn(Optional.of(new UserSession(user, tokens.hash(raw), NOW, NOW.plusSeconds(60))));
        var principal = service.authenticate(raw);
        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.role()).isEqualTo("ADMIN");
    }

    @Test
    void authenticate_atExactExpiration_shouldRequireNewAuthentication() {
        String raw = tokens.generate();
        when(sessions.findByTokenHash(tokens.hash(raw)))
                .thenReturn(Optional.of(new UserSession(mock(User.class), tokens.hash(raw), NOW.minusSeconds(60), NOW)));
        assertThatThrownBy(() -> service.authenticate(raw)).isInstanceOf(AuthException.class);
    }

    @Test
    void authenticate_accountSuspendedAfterLogin_shouldRejectSession() {
        String raw = tokens.generate();
        when(sessions.findByTokenHash(tokens.hash(raw)))
                .thenReturn(Optional.of(new UserSession(mock(User.class), tokens.hash(raw), NOW, NOW.plusSeconds(60))));
        assertThatThrownBy(() -> service.authenticate(raw)).isInstanceOf(AuthException.class);
    }

    @Test
    void authenticate_firebaseJwtOrMalformedToken_shouldNotQueryDatabase() {
        assertThatThrownBy(() -> service.authenticate("header.payload.signature")).isInstanceOf(AuthException.class);
        verify(sessions, never()).findByTokenHash(anyString());
    }

    @Test
    void authenticate_unknownToken_shouldRejectSession() {
        String raw = tokens.generate();
        when(sessions.findByTokenHash(tokens.hash(raw))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.authenticate(raw)).isInstanceOf(AuthException.class);
    }

    @Test
    void logout_shouldUseConditionalRevocationOfOneSession() {
        var principal = new SessionPrincipal(UUID.randomUUID(), UUID.randomUUID(), "USER");
        when(sessions.revoke(principal.sessionId(), NOW)).thenReturn(1);
        service.logout(principal);
        verify(sessions).revoke(principal.sessionId(), NOW);
    }

    @Test
    void logout_alreadyRevokedOrExpired_shouldReject() {
        var principal = new SessionPrincipal(UUID.randomUUID(), UUID.randomUUID(), "USER");
        assertThatThrownBy(() -> service.logout(principal)).isInstanceOf(AuthException.class);
    }

    @Test
    void sessionConfiguration_shouldRejectNonPositiveLifetime() {
        assertThatThrownBy(() -> new AuthProperties(Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthProperties(Duration.ofSeconds(-1))).isInstanceOf(IllegalArgumentException.class);
    }
}
