package com.example.demo.dto;

import java.time.LocalDateTime;

public class AdminUserDto {
    public record Response(Long id, String username, String fullName, String role, boolean active,
            LocalDateTime createdAt, LocalDateTime updatedAt, Long cabinetId, String cabinetName) {}

    /** {@code cabinetId} is required for doctors and secretaries and must be empty for admins. */
    public record CreateRequest(String username, String fullName, String role, String password, Long cabinetId) {}

    public record UpdateRequest(String username, String fullName, String role, Boolean active, Long cabinetId) {}

    public record PasswordRequest(String password) {}
}
