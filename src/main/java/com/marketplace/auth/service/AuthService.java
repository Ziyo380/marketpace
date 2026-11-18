package com.marketplace.auth.service;

import com.marketplace.auth.dto.request.LoginRequest;
import com.marketplace.auth.dto.request.RegisterRequest;
import com.marketplace.auth.dto.response.LoginResponse;
import com.marketplace.auth.exception.InvalidCredentialsException;
import com.marketplace.auth.exception.UserAlreadyExistsException;
import com.marketplace.security.jwt.JwtTokenProvider;
import com.marketplace.user.entity.Role;
import com.marketplace.user.entity.RoleName;
import com.marketplace.user.entity.User;
import com.marketplace.user.entity.UserStatus;
import com.marketplace.user.repository.RoleRepository;
import com.marketplace.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public UUID register(RegisterRequest request) {
        String username = request.username();
        String rawPassword = request.password();
        boolean userExists = userRepository.existsByUsername(username);
        if (userExists) {
            throw new UserAlreadyExistsException(username);
        }

        String passwordHash = passwordEncoder.encode(rawPassword);

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("Required role CUSTOMER is not configured"));

        User user = new User(username, passwordHash);
        user.addRole(customerRole);
        User savedUser = userRepository.save(user);
        return savedUser.getId();
    }

    @Transactional
    public LoginResponse login(LoginRequest request){
        String username = request.username();
        String rawPassword = request.password();
        User user = userRepository.findByUsername(username)
                .orElseThrow(()-> new InvalidCredentialsException());
        String passwordHash = user.getPasswordHash();
        boolean passwordMatch = passwordEncoder.matches(rawPassword, passwordHash);
        if (!passwordMatch) {
            throw new InvalidCredentialsException();
        }
        UserStatus  userStatus = user.getStatus();
        if (userStatus!=UserStatus.ACTIVE) {
            throw new InvalidCredentialsException();
        }
        return tokensFor(user);

    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        User user = refreshTokenService.consume(refreshToken);
        return tokensFor(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private LoginResponse tokensFor(User user) {
        return new LoginResponse(jwtTokenProvider.generateToken(user.getUsername()),
                "Bearer", refreshTokenService.issue(user));
    }
}
