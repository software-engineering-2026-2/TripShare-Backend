package ufcg.software.engineering.trip.share.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record ExternalPlaceSearchQuery(
        String query,
        String near,
        BigDecimal latitude,
        BigDecimal longitude,
        List<String> categories,
        Integer limit) {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 50;

    public ExternalPlaceSearchQuery {
        query = normalize(query);
        near = normalize(near);
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("latitude and longitude must be provided together");
        }
        categories = categories == null
                ? List.of()
                : categories.stream()
                        .map(ExternalPlaceSearchQuery::normalize)
                        .filter(Objects::nonNull)
                        .toList();
        int resolvedLimit = limit == null ? DEFAULT_LIMIT : limit;
        if (resolvedLimit < 1 || resolvedLimit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        limit = resolvedLimit;
    }

    public static ExternalPlaceSearchQuery forDestination(
            String destinationName,
            String region,
            String countryCode,
            BigDecimal latitude,
            BigDecimal longitude,
            String query,
            List<String> categories,
            Integer limit) {
        if (latitude != null && longitude != null) {
            return byCoordinates(latitude, longitude, query, categories, limit);
        }
        String nearText = formatNear(
                Objects.requireNonNull(destinationName, "destinationName must not be null"),
                region,
                Objects.requireNonNull(countryCode, "countryCode must not be null"));
        return byNear(nearText, query, categories, limit);
    }

    public static ExternalPlaceSearchQuery byNear(
            String near, String query, List<String> categories, Integer limit) {
        return new ExternalPlaceSearchQuery(query, near, null, null, categories, limit);
    }

    public static ExternalPlaceSearchQuery byCoordinates(
            BigDecimal latitude, BigDecimal longitude, String query, List<String> categories, Integer limit) {
        return new ExternalPlaceSearchQuery(query, null, latitude, longitude, categories, limit);
    }

    private static String formatNear(String name, String region, String countryCode) {
        if (region != null && !region.isBlank()) {
            return name.trim() + ", " + region.trim() + ", " + countryCode.trim();
        }
        return name.trim() + ", " + countryCode.trim();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
