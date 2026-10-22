package ufcg.software.engineering.trip.share.service;

import com.google.firebase.ErrorCode;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import java.util.Set;
import org.springframework.stereotype.Service;
import ufcg.software.engineering.trip.share.dto.LoginRequest;
import ufcg.software.engineering.trip.share.dto.LoginResponse;
import ufcg.software.engineering.trip.share.exceptions.AuthException;
import ufcg.software.engineering.trip.share.security.SessionPrincipal;

@Service
public class AuthService {
    private static final Set<AuthErrorCode> INVALID_IDENTITY = Set.of(AuthErrorCode.INVALID_ID_TOKEN,
            AuthErrorCode.EXPIRED_ID_TOKEN, AuthErrorCode.REVOKED_ID_TOKEN,
            AuthErrorCode.USER_DISABLED, AuthErrorCode.USER_NOT_FOUND, AuthErrorCode.TENANT_ID_MISMATCH);
    private final FirebasePasswordClient passwords;
    private final FirebaseTokenService firebaseTokens;
    private final SessionService sessions;

    public AuthService(FirebasePasswordClient passwords, FirebaseTokenService firebaseTokens, SessionService sessions) {
        this.passwords = passwords;
        this.firebaseTokens = firebaseTokens;
        this.sessions = sessions;
    }

    public LoginResponse login(LoginRequest request) {
        String idToken = request.method() == LoginRequest.Method.PASSWORD
                ? passwords.signIn(request.email(), request.password()) : request.idToken();
        String uid;
        try {
            uid = firebaseTokens.validate(idToken).getUid();
        } catch (FirebaseAuthException ex) {
            if (ex.getAuthErrorCode() != null && INVALID_IDENTITY.contains(ex.getAuthErrorCode())
                    || ex.getErrorCode() == ErrorCode.UNAUTHENTICATED || ex.getErrorCode() == ErrorCode.INVALID_ARGUMENT) {
                throw AuthException.invalidCredentials();
            }
            throw AuthException.unavailable();
        } catch (IllegalArgumentException ex) {
            throw AuthException.invalidCredentials();
        }
        // A chamada externa termina antes da transação que cria a sessão local.
        return sessions.create(uid);
    }

    public void logout(SessionPrincipal principal) {
        sessions.logout(principal);
    }
}
