package com.marketplace.user.service;

import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.user.dto.response.UserProfileResponse;
import com.marketplace.user.dto.request.UpdateProfileRequest;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class UserService{
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String username){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username '" + username + "' was not found"));
        UserProfileResponse response=toResponse(user);
        return response;
    }

    @Transactional
    public UserProfileResponse updateMyProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.updateProfile(normalize(request.fullName()), normalize(request.phone()));
        return toResponse(user);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getStatus(),
                user.getFullName(), user.getPhone());
    }

}
