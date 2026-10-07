package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class DashboardDto {
    public record AppointmentItem(
            Long id,
            Long patientId,
            String patientName,
            LocalDate date,
            LocalTime time,
            String providerName,
            String status) {
    }

    public record Response(
            SessionUserDto user,
            LocalDate date,
            long todayAppointments,
            long upcomingAppointments,
            long waitingPatients,
            long totalPatients,
            long treatmentsInProgress,
            long unreadMessages,
            List<AppointmentItem> appointments) {
    }
}
