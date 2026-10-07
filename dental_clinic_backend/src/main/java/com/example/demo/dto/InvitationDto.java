package com.example.demo.dto;

import java.time.LocalDateTime;

public class InvitationDto {
    /** Returned once, when the link is created: the token is never stored in clear. */
    public record Created(Long userId, String username, String fullName, String path, LocalDateTime expiresAt) {}

    public record Preview(String username, String fullName, String role, String cabinetName) {}

    public record Accept(String password) {}
}
