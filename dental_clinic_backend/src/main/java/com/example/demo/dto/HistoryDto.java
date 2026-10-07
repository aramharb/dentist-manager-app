package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.entity.HistoryEventType;

import jakarta.validation.constraints.NotBlank;

public class HistoryDto {
    public record Request(HistoryEventType eventType, @NotBlank String title, String description, LocalDateTime eventAt, String createdBy) {}
    public record Response(Long id, Long treatmentId, HistoryEventType eventType, String title, String description, LocalDateTime eventAt, String createdBy) {}
}
