package ufcg.software.engineering.trip.share.auth;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ufcg.software.engineering.trip.share.service.FirebaseIdentity;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

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
    private FirebaseTokenService firebaseTokenService;

    @Test
    void shouldAllowAccessWithUidPrincipal_whenBearerTokenIsValid() throws Exception {
        when(firebaseTokenService.validate(VALID_TOKEN))
                .thenReturn(new FirebaseIdentity("test-firebase-uid", "test@example.invalid"));

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
                .thenThrow(new BadCredentialsException("Invalid Firebase token"));

        mockMvc.perform(get("/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-id-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"status\":401,\"message\":\"Invalid Firebase token\"}"));
    }

    @Test
    void shouldAllowAccess_whenBearerSchemeIsLowercase() throws Exception {
        // auth-scheme é case-insensitive por RFC 9110 §11.1: "bearer" deve autenticar igual a "Bearer"
        when(firebaseTokenService.validate(VALID_TOKEN))
                .thenReturn(new FirebaseIdentity("lowercase-bearer-uid", "test@example.invalid"));

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
            return Map.of("uid", ((FirebaseIdentity) authentication.getPrincipal()).uid());
        }
    }
}
