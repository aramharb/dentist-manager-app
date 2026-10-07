package com.example.demo.dto;

import com.example.demo.entity.AppointmentPriority;
import com.example.demo.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentRequest(
        @NotNull Long patientId,
        Long treatmentId,
        @NotNull LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        LocalTime heure,
        Integer durationMinutes,
        Long providerUserId,
        String providerName,
        AppointmentPriority priority,
        AppointmentStatus status,
        String notes
) {
    public LocalTime effectiveStartTime() {
        return startTime == null ? heure : startTime;
    }

    public LocalTime effectiveEndTime() {
        LocalTime start = effectiveStartTime();
        if (endTime != null) return endTime;
        return start == null || durationMinutes == null ? null : start.plusMinutes(durationMinutes);
    }
}
