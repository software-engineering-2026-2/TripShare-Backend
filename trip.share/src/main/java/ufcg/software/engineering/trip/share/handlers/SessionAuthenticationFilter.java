package ufcg.software.engineering.trip.share.handlers;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import ufcg.software.engineering.trip.share.exceptions.AuthException;
import ufcg.software.engineering.trip.share.service.SessionService;

public class SessionAuthenticationFilter extends OncePerRequestFilter {
    private final SessionService sessions;
    private final AuthProblemResponses problems;

    public SessionAuthenticationFilter(SessionService sessions, AuthProblemResponses problems) {
        this.sessions = sessions;
        this.problems = problems;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Um token antigo no cliente não deve impedir uma nova autenticação.
        return "POST".equals(request.getMethod())
                && (request.getContextPath() + "/auth/login").equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null) {
            try {
                if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) throw AuthException.invalidSession();
                var principal = sessions.authenticate(header.substring(7).trim());
                var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (AuthException ex) {
                SecurityContextHolder.clearContext();
                problems.write(request, response, ex.getStatus(), ex.getErrorCode(), ex.getMessage());
                return;
            } catch (DataAccessException ex) {
                SecurityContextHolder.clearContext();
                problems.write(request, response, HttpStatus.SERVICE_UNAVAILABLE,
                        "DATABASE_UNAVAILABLE", "Serviço temporariamente indisponível.");
                return;
            }
        }
        // Não capturar falhas do endpoint como se fossem falhas de autenticação.
        chain.doFilter(request, response);
    }
}
