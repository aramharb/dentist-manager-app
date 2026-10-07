package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class MessageDto {
    public record ConversationResponse(
            Long id,
            String title,
            List<UserDto.Response> participants,
            MessageResponse lastMessage,
            long unreadCount,
            LocalDateTime updatedAt) {}

    public record MessageResponse(
            Long id,
            Long conversationId,
            Long senderId,
            String senderName,
            String senderRole,
            Long recipientId,
            String recipientName,
            String body,
            LocalDateTime sentAt,
            boolean read,
            boolean mine) {}

    public record CreateConversationRequest(@NotEmpty List<@NotNull Long> participantIds, String title, String message) {}
    public record SendMessageRequest(@NotBlank String body) {}
}
