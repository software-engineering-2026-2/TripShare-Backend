package ufcg.software.engineering.trip.share.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String username,
        String email
) {}
