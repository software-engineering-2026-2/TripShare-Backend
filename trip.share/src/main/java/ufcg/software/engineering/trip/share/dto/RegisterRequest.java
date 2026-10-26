package ufcg.software.engineering.trip.share.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(

        @NotBlank(message="Username is required")
        String username,

        @Pattern(
                regexp = "^\\+[1-9]\\d{1,14}$",
                message = "Phone must be in E.164 format"
        )
        String phone,

        String bio,

        String hometown
) {}

