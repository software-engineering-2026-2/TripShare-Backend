package ufcg.software.engineering.trip.share.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PlaceResponse(
        UUID id,
        String name,
        String address,
        String neighborhood,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer estimatedVisitMinutes,
        BigDecimal estimatedCost,
        String currency) {
}
