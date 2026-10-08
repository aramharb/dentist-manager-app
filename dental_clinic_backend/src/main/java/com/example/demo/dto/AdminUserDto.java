package com.example.demo.dto;

import java.time.LocalDateTime;

public class AdminUserDto {
    /** {@code invitationPending}: the person has not chosen a password yet and still has a valid link. */
    public record Response(Long id, String username, String fullName, String role, boolean active,
            LocalDateTime createdAt, LocalDateTime updatedAt, Long cabinetId, String cabinetName,
            boolean invitationPending, boolean locked) {}

    /** The account plus the one-time link its owner uses to choose a password. */
    public record Created(Response user, InvitationDto.Created invitation) {}

    /** {@code cabinetId} is required for every role except admin, which must have none. */
    public record CreateRequest(String username, String fullName, String role, Long cabinetId) {}

    public record UpdateRequest(String username, String fullName, String role, Boolean active, Long cabinetId) {}

    /** What a cabinet manager sends: the cabinet is always the manager's own. */
    public record MemberCreateRequest(String username, String fullName, String role) {}

    public record MemberUpdateRequest(String username, String fullName, String role, Boolean active) {}
}
