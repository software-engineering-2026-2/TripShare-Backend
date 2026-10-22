package ufcg.software.engineering.trip.share.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ufcg.software.engineering.trip.share.config.FirebaseProperties;
import ufcg.software.engineering.trip.share.exceptions.AuthException;

class FirebasePasswordClientTest {
    private MockRestServiceServer server;
    private FirebasePasswordClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://identitytoolkit.googleapis.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FirebasePasswordClient(builder.build(), new FirebaseProperties("file:/unused.json", "test-key"));
    }

    @Test
    void signIn_shouldNormalizeEmailPreservePasswordAndDiscardRefreshToken() {
        server.expect(requestTo("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=test-key"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"email\":\"alex@example.com\",\"password\":\" exact password \",\"returnSecureToken\":true}"))
                .andRespond(withSuccess("{\"idToken\":\"id-token\",\"refreshToken\":\"private\",\"localId\":\"uid\",\"expiresIn\":\"3600\"}", MediaType.APPLICATION_JSON));
        assertThat(client.signIn("Alex@Example.com", " exact password ")).isEqualTo("id-token");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID_LOGIN_CREDENTIALS", "EMAIL_NOT_FOUND", "INVALID_PASSWORD", "USER_DISABLED"})
    void signIn_invalidCredentials_shouldReturnSameGeneric401(String code) {
        server.expect(requestTo("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=test-key"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":400,\"message\":\"" + code + "\",\"errors\":[]}}"));
        assertThatThrownBy(() -> client.signIn("alex@example.com", "wrong"))
                .isInstanceOfSatisfying(AuthException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(ex.getMessage()).isEqualTo("Credenciais inválidas.");
                });
    }

    @Test
    void signIn_providerError_shouldReturn503() {
        server.expect(requestTo("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=test-key"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("private details"));
        assertUnavailable();
    }

    @Test
    void signIn_timeout_shouldReturn503WithoutLeakingEndpointOrKey() {
        server.expect(requestTo("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=test-key"))
                .andRespond(withException(new IOException("private transport details")));
        assertUnavailable();
    }

    @Test
    void signIn_invalidConfiguration_shouldNotBeMisreportedAsBadCredentials() {
        server.expect(requestTo("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=test-key"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"API_KEY_INVALID\"}}"));
        assertUnavailable();
    }

    private void assertUnavailable() {
        assertThatThrownBy(() -> client.signIn("alex@example.com", "password"))
                .isInstanceOfSatisfying(AuthException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(ex.getMessage()).doesNotContain("test-key", "private", "identitytoolkit");
                });
    }
}
