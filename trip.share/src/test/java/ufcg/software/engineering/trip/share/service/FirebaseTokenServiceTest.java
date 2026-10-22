package ufcg.software.engineering.trip.share.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FirebaseTokenServiceTest {

    @Mock
    private FirebaseAuth firebaseAuth;

    @InjectMocks
    private FirebaseTokenService service;

    @Test
    void validate_shouldReturnFirebaseToken_fromVerifyIdToken() throws FirebaseAuthException {
        FirebaseToken token = mock(FirebaseToken.class);
        when(firebaseAuth.verifyIdToken("id-token")).thenReturn(token);

        assertThat(service.validate("id-token")).isSameAs(token);
        verify(firebaseAuth).verifyIdToken("id-token");
        verifyNoMoreInteractions(firebaseAuth);
    }

    @Test
    void validate_shouldThrowFirebaseAuthException_whenTokenInvalid() throws FirebaseAuthException {
        when(firebaseAuth.verifyIdToken("tampered"))
                .thenThrow(new FirebaseAuthException(ErrorCode.INVALID_ARGUMENT, "INVALID_ID_TOKEN", null, null, null));

        assertThatThrownBy(() -> service.validate("tampered"))
                .isInstanceOf(FirebaseAuthException.class);
        verify(firebaseAuth).verifyIdToken("tampered");
    }
}
