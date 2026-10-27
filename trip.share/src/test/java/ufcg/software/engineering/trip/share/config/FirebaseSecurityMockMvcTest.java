package ufcg.software.engineering.trip.share.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ufcg.software.engineering.trip.share.handlers.FirebaseAuthenticationFilter;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;
import ufcg.software.engineering.trip.share.service.FirebaseIdentity;

/**
 * Testa o SecurityConfig + FirebaseAuthenticationFilter com Firebase mockado
 * (FirebaseApp/FirebaseAuth/FirebaseTokenService via MockitoBean): sem chave real e sem rede.
 * Usa um @RestController declarado apenas neste teste (não entra na aplicação) para constatar
 * autenticação efetiva em endpoint protegido.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FirebaseSecurityMockMvcTest {

    private static final String VALID_TOKEN = "valid-id-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private FirebaseApp firebaseApp;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @MockitoBean
    private FirebaseTokenService firebaseTokenService;

    @Test
    void shouldNotRegisterFirebaseFilterAsBean_toAvoidContainerRegistration() {
        // Sem bean de Filter, o Boot não registra o FirebaseAuthenticationFilter no container
        // de servlets (registro duplicado em relação ao SecurityFilterChain).
        assertThat(applicationContext.getBeansOfType(FirebaseAuthenticationFilter.class)).isEmpty();
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
    void shouldReturn401_whenBearerTokenIsEmpty() throws Exception {
        mockMvc.perform(get("/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401_whenTokenIsInvalid() throws Exception {
        when(firebaseTokenService.validate("invalid-token"))
                .thenThrow(new BadCredentialsException("Invalid Firebase token"));

        mockMvc.perform(get("/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"status\":401,\"message\":\"Invalid Firebase token\"}"));
    }

    @Test
    void shouldReturnUid_whenTokenIsValid_onProtectedEndpoint() throws Exception {
        when(firebaseTokenService.validate(VALID_TOKEN))
                .thenReturn(new FirebaseIdentity("test-firebase-uid", "test@example.invalid"));

        // Endpoint protegido responde com o UID do principal: prova de autenticação efetiva
        // dentro do SecurityFilterChain (não apenas de execução do filtro).
        mockMvc.perform(get("/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"uid\":\"test-firebase-uid\"}"));
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
