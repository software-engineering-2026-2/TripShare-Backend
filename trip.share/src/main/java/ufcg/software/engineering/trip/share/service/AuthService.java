package ufcg.software.engineering.trip.share.service;

import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ufcg.software.engineering.trip.share.dto.*;
import ufcg.software.engineering.trip.share.exceptions.AuthException;
import ufcg.software.engineering.trip.share.models.*;
import ufcg.software.engineering.trip.share.repository.*;
import static ufcg.software.engineering.trip.share.exceptions.AuthException.Reason.*;

@Service
public class AuthService {
    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final Validator validator;

    public AuthService(AuthRepository authRepository, UserRepository userRepository, Validator validator) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request, FirebaseIdentity identity) {
        requireSameUid(request.uuid(), identity);
        User user = userRepository.findByAuthUuid(identity.uid())
                .orElseThrow(() -> new AuthException(USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthException(USER_SUSPENDED);
        }
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse register(RegisterRequest request, FirebaseIdentity identity) {
        requireSameUid(request.uuid(), identity);
        if (!validator.validate(new EmailClaim(identity.email())).isEmpty()) {
            throw new AuthException(INVALID_EMAIL);
        }
        String username = request.username().trim();
        if (authRepository.existsByUuid(identity.uid())
                || userRepository.existsByUsernameIgnoreCase(username)
                || userRepository.existsByEmailIgnoreCase(identity.email())) {
            throw new AuthException(DUPLICATE_USER);
        }
        Auth auth = authRepository.save(Auth.create(identity.uid()));
        User user = User.create(auth, username, request.name().trim(), identity.email(),
                request.phone(), request.bio(), request.hometown());
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    private void requireSameUid(String uuid, FirebaseIdentity identity) {
        if (!uuid.equals(identity.uid())) {
            throw new AuthException(UID_MISMATCH);
        }
    }

    private record EmailClaim(@NotBlank @Email @Size(max = 254) String email) {
    }
}
