package ufcg.software.engineering.trip.share.dto;

import java.time.Instant;
import java.util.UUID;
import ufcg.software.engineering.trip.share.models.User;
import ufcg.software.engineering.trip.share.models.UserRole;
import ufcg.software.engineering.trip.share.models.UserStatus;

public record UserResponse(UUID id, UUID authId, String uuid, String username, String name,
        String email, String phone, String bio, String hometown, UserRole role, UserStatus status,
        Instant createdAt, Instant updatedAt) {
    /*
    * Rever se o que retorna nesse Response é dois UID: definido pelo Model do Auth (String e UID).
    * Aparenta estar retornando o mesmo UID. (user.getId() == user.getAuth.getId())
    * */
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getAuth().getId(), user.getAuth().getUuid(),
                user.getUsername(), user.getName(), user.getEmail(), user.getPhone(), user.getBio(),
                user.getHometown(), user.getRole(), user.getStatus(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
