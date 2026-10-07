package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.demo.entity.ExpenseCategory;
import com.example.demo.entity.ExpenseStatus;

import jakarta.validation.constraints.*;

public class ExpenseDto {
    public record Request(
            @NotNull LocalDate expenseDate,
            LocalDate dueDate,
            LocalDateTime paidAt,
            @NotNull ExpenseCategory category,
            @NotBlank String label,
            String description,
            String supplier,
            String invoiceNumber,
            @NotNull @PositiveOrZero BigDecimal amount,
            ExpenseStatus status,
            String owner,
            String sourceRole,
            String enteredBy,
            Boolean unexpected,
            String unexpectedNote,
            String paymentMethod,
            Boolean taxDeductible,
            Boolean recurring,
            String attachmentUrl,
            @Positive Integer billingPeriodMonths) {}

    public record Response(
            Long id,
            LocalDateTime recordedAt,
            LocalDate expenseDate,
            LocalDate dueDate,
            LocalDateTime paidAt,
            ExpenseCategory category,
            String label,
            String description,
            String supplier,
            String invoiceNumber,
            Integer billingPeriodMonths,
            BigDecimal amount,
            ExpenseStatus status,
            String owner,
            String sourceRole,
            String enteredBy,
            boolean unexpected,
            String unexpectedNote,
            String paymentMethod,
            boolean taxDeductible,
            boolean recurring,
            String attachmentUrl,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {}
}
