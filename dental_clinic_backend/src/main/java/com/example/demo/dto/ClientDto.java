package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Everything the client (patient) space exchanges with the server. */
public class ClientDto {
    public record RegisterRequest(
            @NotBlank @Size(max = 160) String fullName,
            @NotBlank @Size(max = 30) String phone,
            @NotBlank @Size(max = 200) String password) {}

    public record LoginRequest(@NotBlank String phone, @NotBlank String password) {}

    public record Account(Long id, String fullName, String phone) {}

    /** Everything the platform holds about a client account (right of access). */
    public record Export(java.time.LocalDateTime exportedAt, Account account, List<Membership> cabinets,
            List<MyRequest> appointmentRequests) {}

    public record DeleteAccountRequest(@NotBlank String password) {}

    public record AuthResponse(String token, Account account) {}

    /** The details a client gives to a cabinet: the same fields as "add client" on the secretary side. */
    public record JoinRequest(
            @NotBlank(message = "First name is required.") @Size(max = 100) String firstName,
            @NotBlank(message = "Last name is required.") @Size(max = 100) String lastName,
            @NotBlank(message = "Gender is required.") @Pattern(regexp = "Male|Female", message = "Gender must be Male or Female.") String gender,
            LocalDate birthDate,
            @Size(max = 255) String address,
            @Size(max = 150) String email,
            @Pattern(regexp = "A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-", message = "Blood type is invalid.") String bloodType,
            @Size(max = 1000) String allergies,
            Boolean cnamCovered,
            @Size(max = 30) String cnamNumber) {}

    public record Membership(BrandingDto.Response cabinet, String status, JoinRequest profile) {}

    public record Doctor(Long id, String fullName) {}

    /** Free start times of one day. Nothing else is ever exposed about the doctor's agenda. */
    public record SlotDay(LocalDate date, List<LocalTime> times) {}

    public record BookingBody(Long doctorId, LocalDate date, LocalTime time, @Size(max = 500) String note) {}

    /** {@code status} is the request status; {@code effectiveStatus} also reflects a later cancellation by the cabinet. */
    public record MyRequest(Long id, Long cabinetId, String cabinetName, Long doctorId, String doctorName,
            LocalDate date, LocalTime startTime, LocalTime endTime, String status, String effectiveStatus,
            String note, String rejectReason) {}
}
