package ufcg.software.engineering.trip.share.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class FirebaseTokenServiceTest {
    @Mock private FirebaseAuth firebaseAuth;
    @Mock private FirebaseToken token;
    @Mock private FirebaseAuthException exception;
    @InjectMocks private FirebaseTokenService service;

    @Test
    void validate_validToken_shouldReturnOnlyVerifiedIdentity() throws Exception {
        when(firebaseAuth.verifyIdToken("id-token")).thenReturn(token);
        when(token.getUid()).thenReturn("opaque-firebase-uid");
        when(token.getEmail()).thenReturn("user@example.invalid");
        assertThat(service.validate("id-token"))
                .isEqualTo(new FirebaseIdentity("opaque-firebase-uid", "user@example.invalid"));
    }

    @ParameterizedTest
    @EnumSource(value = AuthErrorCode.class, names = {"INVALID_ID_TOKEN", "EXPIRED_ID_TOKEN", "REVOKED_ID_TOKEN", "USER_DISABLED", "USER_NOT_FOUND", "TENANT_ID_MISMATCH"})
    void validate_invalidCredentials_shouldThrowBadCredentials(AuthErrorCode code) throws Exception {
        when(firebaseAuth.verifyIdToken("id-token")).thenThrow(exception);
        when(exception.getAuthErrorCode()).thenReturn(code);
        assertThatThrownBy(() -> service.validate("id-token")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void validate_certificateFetchFailure_shouldRemainInfrastructureFailure() throws Exception {
        when(firebaseAuth.verifyIdToken("id-token")).thenThrow(exception);
        when(exception.getAuthErrorCode()).thenReturn(AuthErrorCode.CERTIFICATE_FETCH_FAILED);
        assertThatThrownBy(() -> service.validate("id-token")).isInstanceOf(AuthenticationServiceException.class);
    }

    @Test
    void validate_unknownFirebaseFailure_shouldRemainInfrastructureFailure() throws Exception {
        when(firebaseAuth.verifyIdToken("id-token")).thenThrow(exception);
        assertThatThrownBy(() -> service.validate("id-token")).isInstanceOf(AuthenticationServiceException.class);
    }
}
