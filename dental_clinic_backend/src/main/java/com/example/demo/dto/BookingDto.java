package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.Size;

/** The secretary's view of the appointment requests sent from the client space. */
public class BookingDto {
    /** An existing client file that may be the same person (same phone number). */
    public record PatientMatch(Long id, String patientNumber, String firstName, String lastName, String phoneNumber,
            LocalDate birthDate) {}

    public record Pending(Long id, String clientName, String clientPhone, Long doctorId, String doctorName,
            LocalDate date, LocalTime startTime, LocalTime endTime, String note, LocalDateTime createdAt,
            String membershipStatus, Long linkedPatientId, ClientDto.JoinRequest profile,
            List<PatientMatch> matches) {}

    /** {@code patientId} links the client to that existing file; empty creates a new file (unless already linked). */
    public record ConfirmRequest(Long patientId) {}

    public record RejectRequest(@Size(max = 300) String reason) {}

    public record Confirmed(Long requestId, Long appointmentId, Long patientId, boolean patientCreated) {}
}
