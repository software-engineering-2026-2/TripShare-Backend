package ufcg.software.engineering.trip.share.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.json.JsonMapper;
import ufcg.software.engineering.trip.share.handlers.FirebaseAuthenticationFilter;
import ufcg.software.engineering.trip.share.service.FirebaseIdentity;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

@ExtendWith(MockitoExtension.class)
class FirebaseAuthenticationFilterTest {

    @Mock
    private FirebaseTokenService tokenService;

    private final JsonMapper mapper = mock(JsonMapper.class);

    private FirebaseAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new FirebaseAuthenticationFilter(tokenService, mapper);
        when(mapper.writeValueAsBytes(org.mockito.ArgumentMatchers.any())).thenReturn(new byte[0]);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticate_whenBearerTokenIsValid() throws ServletException, IOException {
        when(tokenService.validate("id-token")).thenReturn(new FirebaseIdentity("uid-1", "user@example.invalid"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Authorization", "Bearer id-token");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(new FirebaseIdentity("uid-1", "user@example.invalid"));
        assertThat(authentication.isAuthenticated()).isTrue();
        verify(tokenService).validate("id-token");
    }

    @Test
    void shouldNotAuthenticate_whenAuthorizationHeaderMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(401); // o filtro escreve o 401 diretamente
        verifyNoInteractions(tokenService);
    }

    @Test
    void shouldAuthenticate_whenBearerSchemeIsNotExactCase() throws ServletException, IOException {
        // auth-scheme é case-insensitive por RFC 9110 §11.1: "bearer"/"BEARER" deve autenticar
        when(tokenService.validate("id-token")).thenReturn(new FirebaseIdentity("uid-2", "user@example.invalid"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Authorization", "bearer id-token");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(new FirebaseIdentity("uid-2", "user@example.invalid"));
        verify(tokenService).validate("id-token");
    }

    @Test
    void shouldNotAuthenticate_whenSchemeIsNotBearer() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(tokenService);
    }

    @Test
    void shouldNotAuthenticate_whenBearerTokenIsEmpty() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Authorization", "Bearer ");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        verify(tokenService, never()).validate(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldNotAuthenticate_whenTokenIsInvalid() throws ServletException, IOException {
        when(tokenService.validate("tampered"))
                .thenThrow(new BadCredentialsException("Invalid Firebase token"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Authorization", "Bearer tampered");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(401); // o filtro retorna 401 para token inválido
        verify(tokenService).validate("tampered");
    }
}
