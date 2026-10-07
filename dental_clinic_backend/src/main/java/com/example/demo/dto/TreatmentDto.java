package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.entity.TreatmentPriority;
import com.example.demo.entity.TreatmentStatus;

import jakarta.validation.constraints.*;

public class TreatmentDto {
    public record PatientSummary(
            Long id,
            String patientNumber,
            String firstName,
            String lastName,
            String phoneNumber,
            String currentTreatment,
            TreatmentStatus treatmentStatus,
            Integer progressPercent,
            Long assignedDoctorUserId,
            String assignedDoctorName,
            LocalDateTime lastVisit,
            BigDecimal paidAmount,
            BigDecimal expectedAmount,
            BigDecimal unpaidBalance) {}

    public record Request(
            Long treatmentTypeId,
            @NotBlank String objective,
            TreatmentStatus status,
            TreatmentPriority priority,
            @Min(0) @Max(100) Integer progressPercent,
            @PositiveOrZero Integer estimatedDurationMinutes,
            @PositiveOrZero BigDecimal estimatedBill,
            @PositiveOrZero BigDecimal paidAmount,
            LocalDateTime upcomingAppointment,
            LocalDateTime lastVisit,
            String doctorNotes) {}

    public record Response(
            Long id,
            Long patientId,
            Long doctorUserId,
            String doctorName,
            Long treatmentTypeId,
            String treatmentTypeName,
            String objective,
            TreatmentStatus status,
            TreatmentPriority priority,
            Integer progressPercent,
            Integer estimatedDurationMinutes,
            BigDecimal estimatedBill,
            BigDecimal paidAmount,
            BigDecimal remainingBalance,
            LocalDateTime upcomingAppointment,
            LocalDateTime lastVisit,
            String doctorNotes,
            List<Integer> assignedTeeth,
            Boolean hasWholeMouthProcedure,
            Integer procedureCount,
            Integer completedProcedureCount,
            Integer remainingProcedureCount,
            BigDecimal plannedTreatmentCost,
            BigDecimal completedTreatmentValue,
            BigDecimal remainingTreatmentValue,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {}
}
