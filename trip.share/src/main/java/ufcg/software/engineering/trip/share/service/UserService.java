package ufcg.software.engineering.trip.share.service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import ufcg.software.engineering.trip.share.dto.UpdateUserRequest;
import ufcg.software.engineering.trip.share.dto.UserPublicResponse;
import ufcg.software.engineering.trip.share.dto.UserResponse;
import ufcg.software.engineering.trip.share.models.User;
import ufcg.software.engineering.trip.share.models.UserStatus;
import ufcg.software.engineering.trip.share.repository.UserRepository;

import ufcg.software.engineering.trip.share.exceptions.AuthException;
import static ufcg.software.engineering.trip.share.exceptions.AuthException.Reason.*;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse updateProfile(UpdateUserRequest request, FirebaseIdentity identity) {
        User user = userRepository.findByAuthUuid(identity.uid())
                .orElseThrow(() -> new AuthException(USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthException(USER_SUSPENDED);
        }

        user.updateProfile(request.name(), request.phone(), request.bio(), request.hometown());
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    public UserPublicResponse getPublicInfoProfile(String id) {
        User user = userRepository.findByAuthUuid(id)
                .orElseThrow(() -> new AuthException(USER_NOT_FOUND));

        return UserPublicResponse.from(user);
    }
}
