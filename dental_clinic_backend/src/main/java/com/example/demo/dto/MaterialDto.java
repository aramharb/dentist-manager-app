package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.demo.entity.MaterialStatus;

import jakarta.validation.constraints.*;

public class MaterialDto {
    public record Request(
            @NotBlank String name,
            String category,
            @NotNull @PositiveOrZero Integer quantity,
            String unit,
            @NotNull @PositiveOrZero Integer minimumStock,
            @NotNull LocalDate expirationDate,
            String supplier,
            MaterialStatus status,
            String batchNumber,
            @PositiveOrZero Integer monthlyConsumption,
            @NotNull @Positive BigDecimal purchaseCost) {}

    public record Response(
            Long id,
            String name,
            String category,
            Integer quantity,
            String unit,
            Integer minimumStock,
            LocalDate expirationDate,
            String supplier,
            MaterialStatus status,
            String batchNumber,
            Integer monthlyConsumption,
            BigDecimal purchaseCost,
            Long purchaseExpenseId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {}
}
