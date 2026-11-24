package com.ems.service;

import com.ems.domain.Role;
import com.ems.domain.User;
import com.ems.dto.AuthDtos.*;
import com.ems.exception.ApiException;
import com.ems.repository.UserRepository;
import com.ems.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        User user = users.findByUsername(request.username())
                .orElseThrow(() -> new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED));
        return new TokenResponse(jwtService.generateAccessToken(user), jwtService.generateRefreshToken(user));
    }

    public TokenResponse refresh(RefreshRequest request) {
        if (!jwtService.isValid(request.refreshToken())) {
            throw new ApiException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
        }
        String username = jwtService.extractUsername(request.refreshToken());
        User user = users.findByUsername(username)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        return new TokenResponse(jwtService.generateAccessToken(user), jwtService.generateRefreshToken(user));
    }

    public UserResponse me() {
        return toResponse(currentUser());
    }

    public UserResponse register(RegisterRequest request) {
        if (users.existsByUsername(request.username()) || users.existsByEmail(request.email())) {
            throw new ApiException("Username or email already exists", HttpStatus.CONFLICT);
        }
        Role role = request.role() == null ? Role.EMPLOYEE : request.role();
        User user = User.builder()
                .username(request.username()).email(request.email())
                .password(passwordEncoder.encode(request.password())).role(role).enabled(true).build();
        return toResponse(users.save(user));
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = currentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ApiException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        users.save(user);
    }

    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException("Unauthenticated", HttpStatus.UNAUTHORIZED);
        }
        return users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
