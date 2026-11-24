package com.ems.dto;

import com.ems.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record RegisterRequest(
            @NotBlank String username, @NotBlank @Email String email, @NotBlank String password,
            Role role) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}
    public record TokenResponse(String accessToken, String refreshToken) {}
    public record UserResponse(Long id, String username, String email, Role role) {}
}
