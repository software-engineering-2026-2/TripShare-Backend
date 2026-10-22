package ufcg.software.engineering.trip.share.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Integração da autenticação Firebase: SecurityFilterChain (SecurityConfig) + filtro
 * FirebaseAuthenticationFilter + FirebaseTokenService, com Firebase mockado via MockitoBean
 * (FirebaseApp/FirebaseAuth/FirebaseTokenService): sem chave real e sem rede.
 * Usa um @RestController declarado apenas neste teste para constatar o UID autenticado num
 * endpoint protegido.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FirebaseAuthIntegrationTest {

    private static final String VALID_TOKEN = "valid-id-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FirebaseApp firebaseApp;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @MockitoBean
    private ufcg.software.engineering.trip.share.service.FirebaseTokenService firebaseTokenService;

    @Test
    void shouldAllowAccessWithUidPrincipal_whenBearerTokenIsValid() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("test-firebase-uid");
        when(firebaseTokenService.validate(VALID_TOKEN)).thenReturn(token);

        mockMvc.perform(get("/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"uid\":\"test-firebase-uid\"}"));
    }

    @Test
    void shouldReturn401_whenAuthorizationHeaderMissing() throws Exception {
        mockMvc.perform(get("/whoami"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401_whenAuthorizationSchemeIsNotBearer() throws Exception {
        mockMvc.perform(get("/whoami").header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401_whenFirebaseTokenIsInvalid() throws Exception {
        when(firebaseTokenService.validate("invalid-id-token"))
                .thenThrow(new FirebaseAuthException(ErrorCode.UNAUTHENTICATED, "INVALID_ID_TOKEN", null, null, null));

        mockMvc.perform(get("/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-id-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEmpty());
    }

    @Test
    void shouldAllowAccess_whenBearerSchemeIsLowercase() throws Exception {
        // auth-scheme é case-insensitive por RFC 9110 §11.1: "bearer" deve autenticar igual a "Bearer"
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("lowercase-bearer-uid");
        when(firebaseTokenService.validate(VALID_TOKEN)).thenReturn(token);

        mockMvc.perform(get("/whoami").header(HttpHeaders.AUTHORIZATION, "bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"uid\":\"lowercase-bearer-uid\"}"));
    }

    @TestConfiguration
    static class WhoAmITestConfig {

        @Bean
        public WhoAmIController whoAmIController() {
            return new WhoAmIController();
        }
    }

    @RestController
    static class WhoAmIController {

        @GetMapping("/whoami")
        Map<String, Object> whoAmI(Authentication authentication) {
            return Map.of("uid", authentication.getName());
        }
    }
}
