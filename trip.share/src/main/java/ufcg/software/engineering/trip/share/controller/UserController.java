package ufcg.software.engineering.trip.share.controller;


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

    /*
    * Rever com o pessoal depois se o UID passado no endpoint é o do Firebase ou o criado pelo nosso BD.
    * (unificar o formato)
    * */
    @GetMapping("/{id}")
    public UserPublicResponse getPublicInfoProfile(@PathVariable String id) {
        return userService.getPublicInfoProfile(id);
    }

    @PatchMapping("/me")
    public UserResponse updateProfile(@AuthenticationPrincipal FirebaseIdentity identity,
                                      @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateProfile(request, identity);
    }

}
