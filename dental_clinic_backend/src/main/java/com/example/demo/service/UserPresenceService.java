package com.example.demo.service;

import java.security.Principal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.example.demo.dto.PresenceDto;

import tools.jackson.databind.ObjectMapper;

@Service
public class UserPresenceService {
    private final ConcurrentMap<String, Set<String>> sessionsByUsername = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public UserPresenceService(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public synchronized void connected(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        String sessionId = sessionId(event.getMessage().getHeaders().get("simpSessionId"));
        if (principal == null || sessionId == null) return;

        Set<String> sessions = sessionsByUsername.computeIfAbsent(
                normalize(principal.getName()), ignored -> ConcurrentHashMap.newKeySet());
        boolean firstSession = sessions.isEmpty();
        sessions.add(sessionId);
        if (firstSession) broadcast(principal.getName(), true);
    }

    @EventListener
    public synchronized void disconnected(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal == null) return;

        String username = normalize(principal.getName());
        Set<String> sessions = sessionsByUsername.get(username);
        if (sessions == null) return;

        sessions.remove(event.getSessionId());
        if (sessions.isEmpty() && sessionsByUsername.remove(username, sessions)) {
            broadcast(principal.getName(), false);
        }
    }

    public synchronized boolean isOnline(String username) {
        Set<String> sessions = sessionsByUsername.get(normalize(username));
        return sessions != null && !sessions.isEmpty();
    }

    private void broadcast(String username, boolean online) {
        String payload = objectMapper.writeValueAsString(new PresenceDto.Update(username, online));
        messagingTemplate.convertAndSend("/topic/presence", payload);
    }

    private String sessionId(Object value) {
        return value instanceof String id && !id.isBlank() ? id : null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
