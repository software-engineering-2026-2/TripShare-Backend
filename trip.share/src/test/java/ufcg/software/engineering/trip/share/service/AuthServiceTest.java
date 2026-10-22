package ufcg.software.engineering.trip.share.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.google.firebase.ErrorCode;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import ufcg.software.engineering.trip.share.dto.LoginRequest;
import ufcg.software.engineering.trip.share.dto.LoginResponse;
import ufcg.software.engineering.trip.share.exceptions.AuthException;
import ufcg.software.engineering.trip.share.security.SessionPrincipal;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock FirebasePasswordClient passwords;
    @Mock FirebaseTokenService firebaseTokens;
    @Mock SessionService sessions;
    @Mock FirebaseToken identity;
    @Mock LoginResponse response;
    @InjectMocks AuthService auth;

    @Test
    void login_password_shouldVerifyFirebaseTokenAndCreateSessionByUid() throws Exception {
        when(passwords.signIn("alex@example.com", " exact password ")).thenReturn("firebase-token");
        when(firebaseTokens.validate("firebase-token")).thenReturn(identity);
        when(identity.getUid()).thenReturn("firebase-uid");
        when(sessions.create("firebase-uid")).thenReturn(response);
        assertThat(auth.login(new LoginRequest(LoginRequest.Method.PASSWORD, "alex@example.com", " exact password ", null)))
                .isSameAs(response);
    }

    @Test
    void login_idToken_shouldNotUsePasswordClient() throws Exception {
        when(firebaseTokens.validate("firebase-token")).thenReturn(identity);
        when(identity.getUid()).thenReturn("firebase-uid");
        when(sessions.create("firebase-uid")).thenReturn(response);
        assertThat(auth.login(new LoginRequest(LoginRequest.Method.FIREBASE, null, null, "firebase-token"))).isSameAs(response);
        verifyNoInteractions(passwords);
    }

    @ParameterizedTest
    @EnumSource(value = AuthErrorCode.class, names = {"REVOKED_ID_TOKEN", "EXPIRED_ID_TOKEN", "USER_DISABLED", "INVALID_ID_TOKEN"})
    void login_revokedOrDisabledIdentity_shouldReturnSameGenericError(AuthErrorCode code) throws Exception {
            var failure = org.mockito.Mockito.mock(FirebaseAuthException.class);
            when(failure.getAuthErrorCode()).thenReturn(code);
            when(firebaseTokens.validate("invalid")).thenThrow(failure);
            assertThatThrownBy(() -> auth.login(new LoginRequest(LoginRequest.Method.FIREBASE, null, null, "invalid")))
                    .isInstanceOfSatisfying(AuthException.class, ex -> {
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                        assertThat(ex.getMessage()).isEqualTo("Credenciais inválidas.");
                    });
        verifyNoInteractions(sessions);
    }

    @Test
    void login_providerFailure_shouldReturn503WithoutLeakingMessage() throws Exception {
        when(firebaseTokens.validate("token")).thenThrow(new FirebaseAuthException(
                ErrorCode.UNAVAILABLE, "private provider message", null, null, null));
        assertThatThrownBy(() -> auth.login(new LoginRequest(LoginRequest.Method.FIREBASE, null, null, "token")))
                .isInstanceOfSatisfying(AuthException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(ex.getMessage()).doesNotContain("private provider");
                });
        verifyNoInteractions(sessions);
    }

    @Test
    void logout_shouldRevokeOnlyCurrentSession() {
        var principal = new SessionPrincipal(UUID.randomUUID(), UUID.randomUUID(), "USER");
        auth.logout(principal);
        verify(sessions).logout(principal);
    }
}
