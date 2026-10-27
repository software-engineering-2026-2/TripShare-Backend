package ufcg.software.engineering.trip.share.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 128) String uuid,
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 32) @Pattern(regexp = "^\\+[1-9]\\d{1,14}$") String phone,
        String bio,
        String hometown) {
}
