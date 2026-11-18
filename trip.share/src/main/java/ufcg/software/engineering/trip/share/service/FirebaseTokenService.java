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

    public AuthenticatedUser validate(String idToken) {
        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken);
            return new AuthenticatedUser(token.getUid(), token.getEmail());
        } catch (FirebaseAuthException exception) {
            if (isInvalidCredential(exception)) {
                throw new BadCredentialsException("Invalid Firebase token");
            }
            throw new AuthenticationServiceException("Firebase authentication unavailable", exception);
        }
    }

    public AuthenticatedUser validateSessionCookie(String sessionCookie, boolean checkRevoked) {
        try {
            FirebaseToken token = firebaseAuth.verifySessionCookie(sessionCookie, checkRevoked);
            return new AuthenticatedUser(token.getUid(), token.getEmail());
        } catch (FirebaseAuthException exception) {
            if (isInvalidCredential(exception)) {
                throw new BadCredentialsException("Invalid Firebase session cookie");
            }
            throw new AuthenticationServiceException("Firebase authentication unavailable", exception);
        }
    }

    private boolean isInvalidCredential(FirebaseAuthException exception) {
        var code = exception.getAuthErrorCode();
        return code != null && Set.of(INVALID_ID_TOKEN, EXPIRED_ID_TOKEN, REVOKED_ID_TOKEN,
                INVALID_SESSION_COOKIE, EXPIRED_SESSION_COOKIE, REVOKED_SESSION_COOKIE,
                USER_DISABLED, USER_NOT_FOUND, TENANT_ID_MISMATCH).contains(code);
    }
}
