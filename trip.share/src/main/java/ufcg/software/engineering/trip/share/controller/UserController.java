package ufcg.software.engineering.trip.share.controller;


import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ufcg.software.engineering.trip.share.dto.UpdateUserRequest;
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

    @PatchMapping("/me")
    public UserResponse updateProfile(@AuthenticationPrincipal FirebaseIdentity identity,
                                      @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateProfile(request, identity);
    }

}
