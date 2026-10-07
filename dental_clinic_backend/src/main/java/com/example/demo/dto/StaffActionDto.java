package com.example.demo.dto;

import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;

public class StaffActionDto {
    public record Response(
            Long id,
            Long actorUserId,
            String actorName,
            String actionType,
            String entityType,
            Long entityId,
            JsonNode oldValue,
            JsonNode newValue,
            String description,
            LocalDateTime timestamp,
            String status,
            boolean undoable,
            String undoneBy,
            LocalDateTime undoneAt) {}
}
