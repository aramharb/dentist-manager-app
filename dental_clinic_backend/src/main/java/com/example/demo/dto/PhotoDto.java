package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.entity.PhotoType;

import jakarta.validation.constraints.NotBlank;

public class PhotoDto {
    public record Request(PhotoType photoType, @NotBlank String fileName, @NotBlank String contentType, @NotBlank String url, String description, String uploadedBy) {}
    public record Response(Long id, Long treatmentId, PhotoType photoType, String fileName, String contentType, String url, String description, String uploadedBy, LocalDateTime uploadedAt) {}
}
