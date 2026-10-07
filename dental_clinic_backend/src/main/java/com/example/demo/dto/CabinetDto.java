package com.example.demo.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CabinetDto {
    public record Response(Long id, String name, String code, String address, String phoneNumber, String email,
            boolean active, LocalDateTime createdAt, LocalDateTime updatedAt, Stats stats) {}

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
            Long copyCatalogFromCabinetId) {}
}
