package ufcg.software.engineering.trip.share.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ufcg.software.engineering.trip.share.dto.*;
import ufcg.software.engineering.trip.share.service.AuthService;
import ufcg.software.engineering.trip.share.service.AuthenticatedUser;
import ufcg.software.engineering.trip.share.service.FirebaseSessionService;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final FirebaseSessionService sessionService;

    public AuthController(AuthService authService, FirebaseSessionService sessionService) {
        this.authService = authService;
        this.sessionService = sessionService;
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
                              @AuthenticationPrincipal AuthenticatedUser identity,
                              @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        UserResponse user = authService.login(request, identity);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionService.sessionCookie(
                        sessionService.createSessionCookie(bearerToken(authorization))).toString())
                .body(user);
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request,
                                 @AuthenticationPrincipal AuthenticatedUser identity,
                                 @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        UserResponse user = authService.register(request, identity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, sessionService.sessionCookie(
                        sessionService.createSessionCookie(bearerToken(authorization))).toString())
                .body(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "${app.auth.cookie.name}", required = false) String sessionCookie) {
        sessionService.revokeSession(sessionCookie);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionService.expiredSessionCookie().toString())
                .build();
    }

    private String bearerToken(String authorization) {
        return authorization.substring(7).trim();
    }
}
