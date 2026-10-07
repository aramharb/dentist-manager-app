package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import com.example.demo.security.ClinicPrincipal;
import tools.jackson.databind.json.JsonMapper;

class UserPresenceServiceTests {
    private final UserPresenceService service = new UserPresenceService(
            mock(SimpMessagingTemplate.class), new JsonMapper());
    private final ClinicPrincipal user = new ClinicPrincipal(2L, "Secretary", "secretaire");

    @Test
    void userStaysOnlineUntilLastSessionCloses() {
        connect("tab-one");
        connect("tab-two");
        disconnect("tab-one");
        assertTrue(service.isOnline("secretary"));
        disconnect("tab-two");
        assertFalse(service.isOnline("secretary"));
    }

    @Test
    void duplicateDisconnectDoesNotRemoveAnotherSession() {
        connect("tab-one");
        connect("tab-two");
        disconnect("tab-one");
        disconnect("tab-one");
        assertTrue(service.isOnline("SECRETARY"));
    }

    @Test
    void unknownUserIsOffline() {
        assertFalse(service.isOnline("absent"));
    }

    private void connect(String id) {
        SessionConnectedEvent event = mock(SessionConnectedEvent.class);
        when(event.getUser()).thenReturn(user);
        when(event.getMessage()).thenReturn(new GenericMessage<>(new byte[0], Map.of("simpSessionId", id)));
        service.connected(event);
    }

    private void disconnect(String id) {
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getUser()).thenReturn(user);
        when(event.getSessionId()).thenReturn(id);
        service.disconnected(event);
    }
}
