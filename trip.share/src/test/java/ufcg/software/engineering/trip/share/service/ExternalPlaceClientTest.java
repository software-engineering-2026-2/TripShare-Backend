package ufcg.software.engineering.trip.share.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ufcg.software.engineering.trip.share.exceptions.ExternalPlaceException.Reason.SOURCE_UNAVAILABLE;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;
import ufcg.software.engineering.trip.share.dto.ExternalPlaceData;
import ufcg.software.engineering.trip.share.dto.ExternalPlaceSearchQuery;
import ufcg.software.engineering.trip.share.exceptions.ExternalPlaceException;

@ExtendWith(MockitoExtension.class)
class ExternalPlaceClientTest {

    @Mock private HttpClient httpClient;
    @Mock private HttpResponse<String> httpResponse;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private ExternalPlaceClient client;

    @BeforeEach
    void setUp() {
        client = new ExternalPlaceClient("test-foursquare-key", 5L, httpClient, jsonMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByExternalId_status200_shouldParsePlaceAndCacheSubsequentCalls() throws Exception {
        String payload = """
                {
                  "fsq_place_id": "4b058720f964a520b48422e3",
                  "name": "Cristo Redentor",
                  "description": "Monumento histórico no Corcovado",
                  "latitude": -22.951916,
                  "longitude": -43.210487,
                  "location": {
                    "address": "Parque Nacional da Tijuca",
                    "locality": "Rio de Janeiro",
                    "region": "RJ",
                    "country": "BR",
                    "neighborhood": ["Alto da Boa Vista"],
                    "formatted_address": "Parque Nacional da Tijuca - Alto da Boa Vista, Rio de Janeiro - RJ"
                  },
                  "categories": [
                    {
                      "fsq_category_id": "16026",
                      "name": "Monument",
                      "short_name": "Monument",
                      "plural_name": "Monuments"
                    }
                  ],
                  "hours": {
                    "display": "Open Daily 8:00 AM-7:00 PM",
                    "open_now": true,
                    "regular": [
                      {"day": 1, "open": "0800", "close": "1900"}
                    ]
                  },
                  "photos": [
                    {
                      "fsq_photo_id": "photo-1",
                      "prefix": "https://fastly.4sqi.net/img/general/",
                      "suffix": "/cristo.jpg",
                      "width": 800,
                      "height": 600
                    }
                  ],
                  "price": 2,
                  "rating": 9.5,
                  "link": "https://foursquare.com/v/4b058720f964a520b48422e3"
                }
                """;
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(payload);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        Optional<ExternalPlaceData> firstCall = client.findByExternalId("4b058720f964a520b48422e3");
        Optional<ExternalPlaceData> secondCall = client.findByExternalId("4b058720f964a520b48422e3");

        assertThat(firstCall).isPresent();
        assertThat(secondCall).isSameAs(firstCall);

        ExternalPlaceData place = firstCall.orElseThrow();
        assertThat(place.externalId()).isEqualTo("4b058720f964a520b48422e3");
        assertThat(place.name()).isEqualTo("Cristo Redentor");
        assertThat(place.description()).isEqualTo("Monumento histórico no Corcovado");
        assertThat(place.latitude()).isEqualByComparingTo("-22.951916");
        assertThat(place.longitude()).isEqualByComparingTo("-43.210487");
        assertThat(place.location().neighborhood()).containsExactly("Alto da Boa Vista");
        assertThat(place.categories()).hasSize(1);
        assertThat(place.categories().get(0).name()).isEqualTo("Monument");
        assertThat(place.hours().regular()).hasSize(1);
        assertThat(place.photos().get(0).url())
                .isEqualTo("https://fastly.4sqi.net/img/general/800x600/cristo.jpg");
        assertThat(place.source()).isEqualTo("FOURSQUARE");
        assertThat(place.sourceUrl()).isEqualTo("https://foursquare.com/v/4b058720f964a520b48422e3");

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(1)).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest sentRequest = requestCaptor.getValue();
        assertThat(sentRequest.uri().toString())
                .isEqualTo("https://places-api.foursquare.com/places/4b058720f964a520b48422e3");
        assertThat(sentRequest.headers().firstValue("Authorization"))
                .contains("Bearer test-foursquare-key");
        assertThat(sentRequest.headers().firstValue("X-Places-Api-Version"))
                .contains("2025-06-17");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByExternalId_status404_shouldReturnEmptyOptional() throws Exception {
        when(httpResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        Optional<ExternalPlaceData> result = client.findByExternalId("removed-place-id");

        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 429, 500, 502, 503})
    @SuppressWarnings("unchecked")
    void findByExternalId_unexpectedHttpStatus_shouldThrowSourceUnavailable(int statusCode) throws Exception {
        when(httpResponse.statusCode()).thenReturn(statusCode);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        assertThatThrownBy(() -> client.findByExternalId("fsq-1"))
                .isInstanceOf(ExternalPlaceException.class)
                .extracting(ex -> ((ExternalPlaceException) ex).reason())
                .isEqualTo(SOURCE_UNAVAILABLE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByExternalId_timeoutOrNetworkError_shouldThrowSourceUnavailable() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpTimeoutException("Request timed out"));

        assertThatThrownBy(() -> client.findByExternalId("fsq-1"))
                .isInstanceOf(ExternalPlaceException.class)
                .extracting(ex -> ((ExternalPlaceException) ex).reason())
                .isEqualTo(SOURCE_UNAVAILABLE);
    }

    @Test
    void findByExternalId_missingApiKey_shouldThrowSourceUnavailable() {
        ExternalPlaceClient unconfiguredClient = new ExternalPlaceClient("", 5L, httpClient, jsonMapper);

        assertThatThrownBy(() -> unconfiguredClient.findByExternalId("fsq-1"))
                .isInstanceOf(ExternalPlaceException.class)
                .extracting(ex -> ((ExternalPlaceException) ex).reason())
                .isEqualTo(SOURCE_UNAVAILABLE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchPlaces_forDestination_shouldCacheResultsAndWarmUpPlaceCache() throws Exception {
        String searchPayload = """
                {
                  "results": [
                    {
                      "fsq_place_id": "pao-de-acucar",
                      "name": "Pão de Açúcar",
                      "latitude": -22.9492,
                      "longitude": -43.1545
                    }
                  ]
                }
                """;
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(searchPayload);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ExternalPlaceSearchQuery query = ExternalPlaceSearchQuery.forDestination(
                "Rio de Janeiro", "RJ", "BR", null, null, "mirante", List.of("16000"), null);

        List<ExternalPlaceData> firstSearch = client.searchPlaces(query);
        List<ExternalPlaceData> cachedSearch = client.searchPlaces(query);
        Optional<ExternalPlaceData> warmedUpPlace = client.findByExternalId("pao-de-acucar");

        assertThat(firstSearch).hasSize(1);
        assertThat(cachedSearch).isSameAs(firstSearch);
        assertThat(warmedUpPlace).isPresent();
        assertThat(warmedUpPlace.get().name()).isEqualTo("Pão de Açúcar");

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(1)).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(requestCaptor.getValue().uri().toString())
                .isEqualTo("https://places-api.foursquare.com/places/search?query=mirante&near=Rio+de+Janeiro%2C+RJ%2C+BR&categories=16000&limit=50");
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchPlaces_byCoordinatesFailure_shouldThrowSourceUnavailable() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection reset"));

        ExternalPlaceSearchQuery query = ExternalPlaceSearchQuery.byCoordinates(
                new BigDecimal("-22.9068"), new BigDecimal("-43.1729"), null, List.of(), 20);

        assertThatThrownBy(() -> client.searchPlaces(query))
                .isInstanceOf(ExternalPlaceException.class)
                .extracting(ex -> ((ExternalPlaceException) ex).reason())
                .isEqualTo(SOURCE_UNAVAILABLE);
    }
}
