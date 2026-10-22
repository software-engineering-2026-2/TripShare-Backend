package ufcg.software.engineering.trip.share.security;

import java.security.Principal;
import java.util.UUID;

public record SessionPrincipal(UUID userId, UUID sessionId, String role) implements Principal {
    @Override
    public String getName() { return userId.toString(); }
}
