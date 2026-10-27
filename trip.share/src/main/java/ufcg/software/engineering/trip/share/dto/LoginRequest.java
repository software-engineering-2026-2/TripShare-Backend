package ufcg.software.engineering.trip.share.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank @Size(max = 128) String uuid) {
}
