package com.example.demo.dto;

import java.time.LocalDateTime;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CabinetDto {
    public record Response(Long id, String name, String code, String address, String phoneNumber, String email,
            boolean active, LocalDateTime createdAt, LocalDateTime updatedAt, Stats stats,
            AdminUserDto.Response manager, String ownerName, String tagline, String primaryColor) {}

    /** A new cabinet comes with its manager and the one-time link that manager uses to set a password. */
    public record Created(Response cabinet, InvitationDto.Created managerInvitation) {}

    public record ManagerRequest(
            @NotBlank @Size(max = 80) String username,
            @NotBlank @Size(max = 160) String fullName) {}

    /** Size of a cabinet group and of the data it owns. */
    public record Stats(long doctors, long secretaries, long patients, long appointments, long treatments) {}

    public record Request(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 40) String code,
            @Size(max = 255) String address,
            @Size(max = 30) String phoneNumber,
            @Size(max = 150) String email,
            Boolean active,
            /** On creation only: copy the active procedure catalog of this cabinet into the new one. */
            Long copyCatalogFromCabinetId,
            /** Required when creating a cabinet; ignored on update (use the manager endpoint). */
            @Valid ManagerRequest manager) {}
}
