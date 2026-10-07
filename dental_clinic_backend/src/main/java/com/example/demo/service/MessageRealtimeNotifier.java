package com.example.demo.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.example.demo.dto.MessageDto;

import tools.jackson.databind.ObjectMapper;

@Component
public class MessageRealtimeNotifier {
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public MessageRealtimeNotifier(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    public void send(String username, MessageDto.MessageResponse message) {
        messagingTemplate.convertAndSendToUser(username, "/queue/messages", objectMapper.writeValueAsString(message));
    }
}
