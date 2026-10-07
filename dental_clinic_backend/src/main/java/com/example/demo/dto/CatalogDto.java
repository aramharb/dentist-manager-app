package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class CatalogDto {
    public record Request(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String category,
            @NotNull @PositiveOrZero BigDecimal defaultCost,
            @NotNull @Positive Integer defaultDurationMinutes,
            String description,
            Boolean active) {}

    public record ProcedureCatalogResponse(Long id, String code, String name, String category,
            BigDecimal defaultCost, Integer defaultDurationMinutes, String description,
            Boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {}
}
