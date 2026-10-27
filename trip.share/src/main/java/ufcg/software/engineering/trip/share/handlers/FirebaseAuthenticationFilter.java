package ufcg.software.engineering.trip.share.handlers;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;
import ufcg.software.engineering.trip.share.service.FirebaseIdentity;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

public class FirebaseAuthenticationFilter extends OncePerRequestFilter {
    private final FirebaseTokenService tokenService;
    private final JsonMapper mapper;

    public FirebaseAuthenticationFilter(FirebaseTokenService tokenService, JsonMapper mapper) {
        this.tokenService = tokenService;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)
                || header.substring(7).isBlank()) {
            SecurityErrors.write(mapper, response, 401, "Firebase Bearer token is required");
            return;
        }
        FirebaseIdentity identity;
        try {
            identity = tokenService.validate(header.substring(7).trim());
        } catch (BadCredentialsException exception) {
            SecurityErrors.write(mapper, response, 401, "Invalid Firebase token");
            return;
        } catch (AuthenticationServiceException exception) {
            SecurityErrors.write(mapper, response, 503, "Firebase authentication unavailable");
            return;
        }
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(identity, null, List.of()));
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }
}
