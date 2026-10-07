package com.example.demo.dto;

import com.example.demo.entity.AppointmentPriority;
import com.example.demo.entity.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentResponse(
        Long id,
        Long patientId,
        String patientFirstName,
        String patientLastName,
        Long treatmentId,
        String treatmentObjective,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        LocalTime heure,
        Integer durationMinutes,
        String providerName,
        Long providerUserId,
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
