package ufcg.software.engineering.trip.share.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ufcg.software.engineering.trip.share.dto.*;
import ufcg.software.engineering.trip.share.service.AuthService;
import ufcg.software.engineering.trip.share.service.FirebaseIdentity;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request,
                              @AuthenticationPrincipal FirebaseIdentity identity) {
        return authService.login(request, identity);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request,
                                 @AuthenticationPrincipal FirebaseIdentity identity) {
        return authService.register(request, identity);
    }
}
