package ufcg.software.engineering.trip.share.handlers;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

/**
 * Lê o header Authorization Bearer, valida o ID token via FirebaseTokenService (Firebase Admin)
 * e popula o SecurityContext. Requests sem token, malformadas ou com token inválido ficam
 * sem autenticação e são bloqueadas com 401 pelo SecurityFilterChain.
 */
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    // o auth-scheme é case-insensitive por RFC 9110 §11.1 (aceita "Bearer", "bearer", "BEARER", ...)
    private static final String BEARER_PREFIX = "bearer ";

    private final FirebaseTokenService tokenService;

    public FirebaseAuthenticationFilter(FirebaseTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            String idToken = header.substring(BEARER_PREFIX.length()).trim();
            if (!idToken.isEmpty()) {
                try {
                    FirebaseToken token = tokenService.validate(idToken);

                    // campo nome = UID do Firebase; credentials = token completo (contém uid, claims, emissor)
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(token.getUid(), token, List.of());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (FirebaseAuthException e) {
                    // token inválido/expirado/ilegível: segue sem autenticação no contexto; a requisição é
                    // bloqueada pela autorização do SecurityFilterChain (SecurityConfig) com 401 genérico, sem vazar detalhes
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
