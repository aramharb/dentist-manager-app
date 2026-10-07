package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.entity.ProcedureStatus;

import jakarta.validation.constraints.*;

public class ProcedureDto {
    public record Request(
            Long procedureCatalogId,
            Integer toothNumber,
            Boolean allTeeth,
            String toothDescription,
            @NotBlank String name,
            ProcedureStatus status,
            String practitioner,
            @PositiveOrZero BigDecimal cost,
            @Positive Integer durationMinutes,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            String notes) {}

    public record Response(
            Long id,
            Long treatmentId,
            Long procedureCatalogId,
            Integer toothNumber,
            Boolean allTeeth,
            String toothDescription,
            String name,
            ProcedureStatus status,
            String practitioner,
            BigDecimal cost,
            Integer durationMinutes,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            Long completedByUserId,
            String completedByName,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {}
}
