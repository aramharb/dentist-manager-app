package com.example.demo.dto;

import java.time.LocalDateTime;

public class AccessLogDto {
    public record Entry(Long id, String userName, String userRole, String action, String resource, Long resourceId,
            String clientAddress, LocalDateTime occurredAt) {}
}
