package com.marketplace.user.controller;

import com.marketplace.user.dto.response.UserProfileResponse;
import com.marketplace.user.dto.request.UpdateProfileRequest;
import jakarta.validation.Valid;
import com.marketplace.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController{
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getUserProfile(Authentication authentication){
        String username=authentication.getName();
        UserProfileResponse profile=userService.getMyProfile(username);
        return ResponseEntity.ok().body(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateMyProfile(authentication.getName(), request));
    }
}
