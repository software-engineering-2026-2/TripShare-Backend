package ufcg.software.engineering.trip.share.controller;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ufcg.software.engineering.trip.share.dto.UpdateUserRequest;
import ufcg.software.engineering.trip.share.dto.UserPublicResponse;
import ufcg.software.engineering.trip.share.dto.UserResponse;
import ufcg.software.engineering.trip.share.service.FirebaseIdentity;
import ufcg.software.engineering.trip.share.service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserPublicResponse getPublicInfoProfile(@PathVariable UUID id) {
        return userService.getPublicInfoProfile(id);
    }

    @PatchMapping("/me")
    public UserResponse updateProfile(@AuthenticationPrincipal FirebaseIdentity identity,
                                      @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateProfile(request, identity);
    }

}
