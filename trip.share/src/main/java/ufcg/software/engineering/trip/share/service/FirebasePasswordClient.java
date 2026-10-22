package ufcg.software.engineering.trip.share.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ufcg.software.engineering.trip.share.config.FirebaseProperties;
import ufcg.software.engineering.trip.share.exceptions.AuthException;

@Service
public class FirebasePasswordClient {
    private static final Set<String> CREDENTIAL_ERRORS = Set.of(
            "INVALID_LOGIN_CREDENTIALS", "EMAIL_NOT_FOUND", "INVALID_PASSWORD", "USER_DISABLED", "INVALID_EMAIL");
    private final RestClient client;
    private final FirebaseProperties properties;

    public FirebasePasswordClient(RestClient firebaseRestClient, FirebaseProperties properties) {
        this.client = firebaseRestClient;
        this.properties = properties;
    }

    public String signIn(String email, String password) {
        try {
            var response = client.post()
                    .uri(uri -> uri.path("/v1/accounts:signInWithPassword").queryParam("key", properties.webApiKey()).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email.toLowerCase(Locale.ROOT), "password", password, "returnSecureToken", true))
                    .retrieve().body(PasswordResponse.class);
            if (response == null || response.idToken() == null || response.idToken().isBlank()) {
                throw AuthException.unavailable();
            }
            return response.idToken();
        } catch (RestClientResponseException ex) {
            // Não propagar a exceção HTTP: ela pode conter a API key na URL e dados do provedor.
            if (ex.getStatusCode().value() == 400 && isCredentialError(ex)) throw AuthException.invalidCredentials();
            throw AuthException.unavailable();
        } catch (RestClientException ex) {
            throw AuthException.unavailable();
        }
    }

    private boolean isCredentialError(RestClientResponseException failure) {
        try {
            var body = failure.getResponseBodyAs(ProviderError.class);
            return body != null && body.error() != null && CREDENTIAL_ERRORS.contains(body.error().message());
        } catch (RestClientException ex) {
            return false;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PasswordResponse(String idToken) {
        @Override public String toString() { return "PasswordResponse[idToken=[REDACTED]]"; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProviderError(ErrorInfo error) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorInfo(String message) { }
}
