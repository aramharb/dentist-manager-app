package com.example.demo.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

public class PrescriptionDto {
    public record Request(@NotBlank String medicineName, @NotBlank String dosage, @NotBlank String duration, String instructions, LocalDateTime issuedAt, String pdfUrl) {}
    public record Response(Long id, Long treatmentId, String medicineName, String dosage, String duration, String instructions, LocalDateTime issuedAt, String pdfUrl) {}
}
