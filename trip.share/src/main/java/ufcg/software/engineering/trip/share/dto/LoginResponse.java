package ufcg.software.engineering.trip.share.dto;

import java.time.Instant;
import java.util.UUID;
import ufcg.software.engineering.trip.share.models.User;

public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, UserInfo user) {
    public record UserInfo(UUID id, String username, String name, String email, String role) {
        public static UserInfo from(User user) {
            return new UserInfo(user.getId(), user.getUsername(), user.getName(), user.getEmail(), user.getRole());
        }
    }

    @Override
    public String toString() {
        return "LoginResponse[accessToken=[REDACTED], expiresAt=" + expiresAt + "]";
    }
}
