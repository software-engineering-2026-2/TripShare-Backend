package ufcg.software.engineering.trip.share.dto;

import java.util.UUID;

public record RegisterReponse(
        UUID id,
        String username,
        String email
) {}
