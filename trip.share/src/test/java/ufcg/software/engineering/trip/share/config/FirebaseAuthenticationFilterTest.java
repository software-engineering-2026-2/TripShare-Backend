package ufcg.software.engineering.trip.share.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.firebase.ErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
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
import ufcg.software.engineering.trip.share.handlers.FirebaseAuthenticationFilter;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

@ExtendWith(MockitoExtension.class)
class FirebaseAuthenticationFilterTest {

    @Mock
    private FirebaseTokenService tokenService;

    private FirebaseAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new FirebaseAuthenticationFilter(tokenService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticate_whenBearerTokenIsValid() throws FirebaseAuthException, ServletException, IOException {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("uid-1");
        when(tokenService.validate("id-token")).thenReturn(token);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Authorization", "Bearer id-token");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo("uid-1");
        assertThat(authentication.getCredentials()).isSameAs(token);
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
        assertThat(chain.getRequest()).isNotNull(); // a requisição segue; o bloqueio (401) fica com o SecurityFilterChain
        verifyNoInteractions(tokenService);
    }

    @Test
    void shouldAuthenticate_whenBearerSchemeIsNotExactCase() throws FirebaseAuthException, ServletException, IOException {
        // auth-scheme é case-insensitive por RFC 9110 §11.1: "bearer"/"BEARER" deve autenticar
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("uid-2");
        when(tokenService.validate("id-token")).thenReturn(token);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Authorization", "bearer id-token");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo("uid-2");
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
        verifyNoInteractions(tokenService);
    }

    @Test
    void shouldNotAuthenticate_whenBearerTokenIsEmpty() throws FirebaseAuthException, ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Authorization", "Bearer ");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenService, never()).validate(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldNotAuthenticate_whenTokenIsInvalid() throws FirebaseAuthException, ServletException, IOException {
        when(tokenService.validate("tampered"))
                .thenThrow(new FirebaseAuthException(ErrorCode.UNAUTHENTICATED, "INVALID_ID_TOKEN", null, null, null));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Authorization", "Bearer tampered");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(200); // o filtro não muda o status; o 401 é do entry point do chain
        assertThat(chain.getRequest()).isNotNull();
        verify(tokenService).validate("tampered");
    }
}
