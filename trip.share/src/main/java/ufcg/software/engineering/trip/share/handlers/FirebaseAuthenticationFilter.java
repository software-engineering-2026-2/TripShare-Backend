package ufcg.software.engineering.trip.share.handlers;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.http.Cookie;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;
import ufcg.software.engineering.trip.share.config.AuthProperties;
import ufcg.software.engineering.trip.share.service.AuthenticatedUser;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

public class FirebaseAuthenticationFilter extends OncePerRequestFilter {
    private final FirebaseTokenService tokenService;
    private final JsonMapper mapper;
    private final AuthProperties properties;

    public FirebaseAuthenticationFilter(FirebaseTokenService tokenService, JsonMapper mapper,
                                         AuthProperties properties) {
        this.tokenService = tokenService;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (request.getMethod().equals("OPTIONS") || path.equals("/auth/logout")) {
            chain.doFilter(request, response);
            return;
        }

        AuthenticatedUser identity;
        boolean usingSessionCookie = false;
        try {
            if (path.equals("/auth/login") || path.equals("/auth/register")) {
                identity = validateBearer(request, response);
                if (identity == null) return;
            } else {
                String sessionCookie = findCookie(request.getCookies(), properties.cookie().name());
                if (sessionCookie != null) {
                    usingSessionCookie = true;
                    identity = tokenService.validateSessionCookie(sessionCookie, properties.cookie().checkRevoked());
                } else if (properties.bearerFallbackEnabled()) {
                    identity = validateBearer(request, response);
                    if (identity == null) return;
                } else {
                    SecurityErrors.write(mapper, response, 401, "Firebase session cookie is required");
                    return;
                }
            }
        } catch (BadCredentialsException exception) {
            SecurityErrors.write(mapper, response, 401,
                    usingSessionCookie ? "Invalid Firebase session cookie" : "Invalid Firebase token");
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

    private AuthenticatedUser validateBearer(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)
                || header.substring(7).isBlank()) {
            SecurityErrors.write(mapper, response, 401, "Firebase Bearer token is required");
            return null;
        }
        return tokenService.validate(header.substring(7).trim());
    }

    private String findCookie(Cookie[] cookies, String name) {
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) return cookie.getValue();
        }
        return null;
    }
}
