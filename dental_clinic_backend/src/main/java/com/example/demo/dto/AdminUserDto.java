package com.example.demo.dto;

import java.time.LocalDateTime;

public class AdminUserDto {
    public record Response(Long id, String username, String fullName, String role, boolean active,
            LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record CreateRequest(String username, String fullName, String role, String password) {}

    public record UpdateRequest(String username, String fullName, String role, Boolean active) {}

    public record PasswordRequest(String password) {}
}
