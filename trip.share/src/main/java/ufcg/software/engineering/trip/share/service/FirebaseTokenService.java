package ufcg.software.engineering.trip.share.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import static com.google.firebase.auth.AuthErrorCode.*;

@Service
public class FirebaseTokenService {
    private final FirebaseAuth firebaseAuth;

    public FirebaseTokenService(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    public FirebaseIdentity validate(String idToken) {
        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken);
            return new FirebaseIdentity(token.getUid(), token.getEmail());
        } catch (FirebaseAuthException exception) {
            var code = exception.getAuthErrorCode();
            if (code != null && Set.of(INVALID_ID_TOKEN, EXPIRED_ID_TOKEN, REVOKED_ID_TOKEN,
                    USER_DISABLED, USER_NOT_FOUND, TENANT_ID_MISMATCH).contains(code)) {
                throw new BadCredentialsException("Invalid Firebase token");
            }
            throw new AuthenticationServiceException("Firebase authentication unavailable", exception);
        }
    }
}
