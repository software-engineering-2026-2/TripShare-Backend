package ufcg.software.engineering.trip.share.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.SessionCookieOptions;
import java.util.Set;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import ufcg.software.engineering.trip.share.config.AuthProperties;
import static com.google.firebase.auth.AuthErrorCode.*;

@Service
public class FirebaseSessionService {
    private final FirebaseAuth firebaseAuth;
    private final AuthProperties.Cookie cookieProperties;

    public FirebaseSessionService(FirebaseAuth firebaseAuth, AuthProperties properties) {
        this.firebaseAuth = firebaseAuth;
        this.cookieProperties = properties.cookie();
    }

    public String createSessionCookie(String idToken) {
        try {
            return firebaseAuth.createSessionCookie(idToken,
                    SessionCookieOptions.builder().setExpiresIn(cookieProperties.maxAge().toMillis()).build());
        } catch (FirebaseAuthException exception) {
            var code = exception.getAuthErrorCode();
            if (code != null && Set.of(INVALID_ID_TOKEN, EXPIRED_ID_TOKEN, REVOKED_ID_TOKEN,
                    USER_DISABLED, USER_NOT_FOUND, TENANT_ID_MISMATCH).contains(code)) {
                throw new BadCredentialsException("Invalid Firebase token");
            }
            throw new AuthenticationServiceException("Firebase authentication unavailable", exception);
        }
    }

    public ResponseCookie sessionCookie(String value) {
        return buildSessionCookie(value, cookieProperties.maxAge());
    }

    public ResponseCookie expiredSessionCookie() {
        return buildSessionCookie("", java.time.Duration.ZERO);
    }

    private ResponseCookie buildSessionCookie(String value, java.time.Duration maxAge) {
        var builder = ResponseCookie.from(cookieProperties.name(), value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path("/")
                .maxAge(maxAge);
        if (cookieProperties.domain() != null && !cookieProperties.domain().isBlank()) {
            builder.domain(cookieProperties.domain());
        }
        return builder.build();
    }

    public void revokeSession(String sessionCookie) {
        if (sessionCookie == null || sessionCookie.isBlank()) {
            return;
        }
        try {
            var token = firebaseAuth.verifySessionCookie(sessionCookie, true);
            firebaseAuth.revokeRefreshTokens(token.getUid());
        } catch (FirebaseAuthException exception) {
            // Logout remains idempotent for missing, invalid, or unverifiable cookies.
        }
    }

}
