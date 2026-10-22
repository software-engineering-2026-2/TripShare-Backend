package ufcg.software.engineering.trip.share.service;

import java.time.Clock;
import java.time.Instant;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ufcg.software.engineering.trip.share.config.AuthProperties;
import ufcg.software.engineering.trip.share.dto.LoginResponse;
import ufcg.software.engineering.trip.share.exceptions.AuthException;
import ufcg.software.engineering.trip.share.models.UserSession;
import ufcg.software.engineering.trip.share.repository.UserRepository;
import ufcg.software.engineering.trip.share.repository.UserSessionRepository;
import ufcg.software.engineering.trip.share.security.SessionPrincipal;

@Service
public class SessionService {
    private static final Pattern TOKEN_FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");
    private final UserRepository users;
    private final UserSessionRepository sessions;
    private final SessionTokenService tokens;
    private final AuthProperties properties;
    private final Clock clock;

    public SessionService(UserRepository users, UserSessionRepository sessions,
            SessionTokenService tokens, AuthProperties properties, Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.tokens = tokens;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public LoginResponse create(String firebaseUid) {
        var user = users.findByFirebaseUid(firebaseUid).filter(u -> u.isActive())
                .orElseThrow(AuthException::invalidCredentials);
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.sessionTtl());
        String token = tokens.generate();
        sessions.save(new UserSession(user, tokens.hash(token), now, expiresAt));
        return new LoginResponse(token, "Bearer", expiresAt, LoginResponse.UserInfo.from(user));
    }

    @Transactional(readOnly = true)
    public SessionPrincipal authenticate(String token) {
        if (!TOKEN_FORMAT.matcher(token).matches()) throw AuthException.invalidSession();
        var session = sessions.findByTokenHash(tokens.hash(token)).orElseThrow(AuthException::invalidSession);
        if (session.getRevokedAt() != null || !session.getExpiresAt().isAfter(clock.instant())
                || !session.getUser().isActive()) {
            throw AuthException.invalidSession();
        }
        return new SessionPrincipal(session.getUser().getId(), session.getId(), session.getUser().getRole());
    }

    @Transactional
    public void logout(SessionPrincipal principal) {
        if (sessions.revoke(principal.sessionId(), clock.instant()) != 1) throw AuthException.invalidSession();
    }
}
