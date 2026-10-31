package ufcg.software.engineering.trip.share.dto;

import ufcg.software.engineering.trip.share.models.User;

public record UserPublicResponse(
        String username,
        String name,
        String bio,
        String hometown
) {
    public static UserPublicResponse from(User user) {
        return new UserPublicResponse(user.getUsername(), user.getName(), user.getBio(), user.getHometown());
    }
}
