package ufcg.software.engineering.trip.share.service;

import static ufcg.software.engineering.trip.share.exceptions.ExternalPlaceException.Reason.SOURCE_UNAVAILABLE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import ufcg.software.engineering.trip.share.dto.ExternalPlaceData;
import ufcg.software.engineering.trip.share.dto.ExternalPlaceSearchQuery;
import ufcg.software.engineering.trip.share.exceptions.ExternalPlaceException;

@Service
public class ExternalPlaceClient {

    private static final String BASE_URL = "https://places-api.foursquare.com/places/";
    private static final String SEARCH_URL = "https://places-api.foursquare.com/places/search";
    private static final String API_VERSION = "2025-06-17";
    private static final String DEFAULT_KEY_PATH = "file:../.secrets/foursquare.txt";
    private static final String ROOT_KEY_PATH = "file:./.secrets/foursquare.txt";

    private final long timeoutTime;
    private final String apikey;
    private final HttpClient client;
    private final JsonMapper jsonMapper;
    private final Map<String, Optional<ExternalPlaceData>> cache = new ConcurrentHashMap<>();
    private final Map<ExternalPlaceSearchQuery, List<ExternalPlaceData>> searchCache = new ConcurrentHashMap<>();

    @Autowired
    public ExternalPlaceClient(
            @Value("${app.foursquare.key-path:" + DEFAULT_KEY_PATH + "}") String keyPath,
            @Value("${app.foursquare.timeout-seconds:5}") long timeoutTime,
            ResourceLoader resourceLoader,
            JsonMapper jsonMapper) {
        this(
                loadApiKey(resourceLoader, keyPath),
                timeoutTime,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(Math.max(1, timeoutTime)))
                        .build(),
                jsonMapper);
    }

    public ExternalPlaceClient(String apikey, long timeoutTime, HttpClient client, JsonMapper jsonMapper) {
        this.apikey = apikey == null ? "" : apikey.trim();
        this.timeoutTime = Math.max(1, timeoutTime);
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
    }

    private static String loadApiKey(ResourceLoader loader, String keyPath) {
        Resource resource = loader.getResource(keyPath);
        if (!resource.exists() && DEFAULT_KEY_PATH.equals(keyPath)) {
            resource = loader.getResource(ROOT_KEY_PATH);
        }
        if (!resource.exists()) {
            return "";
        }
        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new ExternalPlaceException(SOURCE_UNAVAILABLE, e);
        }
    }

    public Optional<ExternalPlaceData> findByExternalId(String externalId) {
        Objects.requireNonNull(externalId, "externalId must not be null");
        return cache.computeIfAbsent(externalId, this::fetchByExternalId);
    }

    public List<ExternalPlaceData> searchPlaces(ExternalPlaceSearchQuery searchQuery) {
        Objects.requireNonNull(searchQuery, "searchQuery must not be null");
        return searchCache.computeIfAbsent(searchQuery, this::fetchPlaces);
    }

    private Optional<ExternalPlaceData> fetchByExternalId(String externalId) {
        requireApiKey();
        String encodedId = encode(externalId);
        String url = BASE_URL + encodedId;

        HttpRequest request = buildGetRequest(url);

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return switch (response.statusCode()) {
                case 200 -> Optional.of(jsonMapper.readValue(response.body(), ExternalPlaceData.class));
                case 404 -> Optional.empty();
                default -> throw new ExternalPlaceException(SOURCE_UNAVAILABLE);
            };
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalPlaceException(SOURCE_UNAVAILABLE, e);
        } catch (IOException | JacksonException e) {
            throw new ExternalPlaceException(SOURCE_UNAVAILABLE, e);
        }
    }

    private List<ExternalPlaceData> fetchPlaces(ExternalPlaceSearchQuery searchQuery) {
        requireApiKey();
        String url = buildSearchUrl(searchQuery);
        HttpRequest request = buildGetRequest(url);

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ExternalPlaceException(SOURCE_UNAVAILABLE);
            }
            SearchResponse parsed = jsonMapper.readValue(response.body(), SearchResponse.class);
            if (parsed == null || parsed.results() == null || parsed.results().isEmpty()) {
                return List.of();
            }
            List<ExternalPlaceData> places = List.copyOf(parsed.results());
            for (ExternalPlaceData place : places) {
                if (place.externalId() != null && !place.externalId().isBlank()) {
                    cache.putIfAbsent(place.externalId(), Optional.of(place));
                }
            }
            return places;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalPlaceException(SOURCE_UNAVAILABLE, e);
        } catch (IOException | JacksonException e) {
            throw new ExternalPlaceException(SOURCE_UNAVAILABLE, e);
        }
    }

    private HttpRequest buildGetRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutTime))
                .header("Authorization", "Bearer " + apikey)
                .header("X-Places-Api-Version", API_VERSION)
                .header("Accept", "application/json")
                .GET()
                .build();
    }

    private String buildSearchUrl(ExternalPlaceSearchQuery searchQuery) {
        List<String> params = new ArrayList<>();
        if (searchQuery.query() != null) {
            params.add("query=" + encode(searchQuery.query()));
        }
        if (searchQuery.latitude() != null && searchQuery.longitude() != null) {
            params.add("ll=" + encode(searchQuery.latitude() + "," + searchQuery.longitude()));
        } else if (searchQuery.near() != null) {
            params.add("near=" + encode(searchQuery.near()));
        }
        if (!searchQuery.categories().isEmpty()) {
            params.add("categories=" + encode(String.join(",", searchQuery.categories())));
        }
        params.add("limit=" + searchQuery.limit());
        return SEARCH_URL + "?" + String.join("&", params);
    }

    private void requireApiKey() {
        if (apikey.isBlank()) {
            throw new ExternalPlaceException(SOURCE_UNAVAILABLE);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchResponse(List<ExternalPlaceData> results) {
    }
}
