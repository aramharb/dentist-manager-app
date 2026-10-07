package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.demo.dto.MessageDto;
import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.Conversation;
import com.example.demo.entity.ConversationParticipant;
import com.example.demo.entity.LoginUser;
import com.example.demo.entity.MessageNotification;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.repository.ConversationParticipantRepository;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.repository.MessageNotificationRepository;

class MessageServiceTests {
    private LoginUserRepository userRepository;
    private ConversationRepository conversationRepository;
    private ConversationParticipantRepository participantRepository;
    private ChatMessageRepository messageRepository;
    private MessageNotificationRepository notificationRepository;
    private MessageRealtimeNotifier realtimeNotifier;
    private UserPresenceService presenceService;
    private MessageService messageService;

    private LoginUser doctor;
    private LoginUser secretary;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        userRepository = mock(LoginUserRepository.class);
        conversationRepository = mock(ConversationRepository.class);
        participantRepository = mock(ConversationParticipantRepository.class);
        messageRepository = mock(ChatMessageRepository.class);
        notificationRepository = mock(MessageNotificationRepository.class);
        realtimeNotifier = mock(MessageRealtimeNotifier.class);
        presenceService = mock(UserPresenceService.class);
        messageService = new MessageService(userRepository, conversationRepository, participantRepository,
                messageRepository, notificationRepository, realtimeNotifier, presenceService);

        doctor = user(1L, "doctor", "Dr. Wajih");
        secretary = user(2L, "secretaire", "Secretary");
        conversation = conversation(10L, doctor);

        when(userRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(secretary));
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(participantRepository.existsByConversationIdAndUserId(10L, 1L)).thenReturn(true);
        when(participantRepository.existsByConversationIdAndUserId(10L, 2L)).thenReturn(true);
        when(participantRepository.findByConversationId(10L)).thenReturn(List.of(
                participant(conversation, doctor),
                participant(conversation, secretary)));
        when(participantRepository.findByConversationIdAndUserId(10L, 1L)).thenReturn(Optional.of(participant(conversation, doctor)));
        when(participantRepository.findByConversationIdAndUserId(10L, 2L)).thenReturn(Optional.of(participant(conversation, secretary)));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            setId(message, 99L);
            return message;
        });
    }

    @Test
    void sendPersistsMessageAndCreatesNotificationForDestinationOnly() {
        MessageDto.MessageResponse response = messageService.send(10L, 1L, " Hello ");

        assertEquals("Hello", response.body());
        assertEquals(1L, response.senderId());
        verify(messageRepository).save(any(ChatMessage.class));
        verify(notificationRepository).save(any(MessageNotification.class));
        verify(realtimeNotifier).send(org.mockito.ArgumentMatchers.eq("secretaire"), any(MessageDto.MessageResponse.class));
        verify(notificationRepository).markConversationRead(any(), any(), any());
    }

    @Test
    void markReadClearsConversationNotificationsForUser() {
        messageService.markRead(10L, 2L);

        verify(notificationRepository).markConversationRead(any(), any(), any(LocalDateTime.class));
    }

    @Test
    void senderDoesNotReceiveOwnNotificationWhenAloneInConversation() {
        when(participantRepository.findByConversationId(10L)).thenReturn(List.of(participant(conversation, doctor)));

        messageService.send(10L, 1L, "Internal note");

        verify(notificationRepository, never()).save(any(MessageNotification.class));
        verify(realtimeNotifier, never()).send(any(), any());
    }

    @Test
    void unsupportedRoleCannotSendMessages() {
        secretary.setRole("patient");

        assertThrows(MessagingAccessDeniedException.class, () -> messageService.send(10L, 1L, "Hello"));

        verify(messageRepository, never()).save(any(ChatMessage.class));
    }

    private LoginUser user(Long id, String role, String fullName) {
        LoginUser user = new LoginUser();
        setId(user, id);
        user.setUsername(role);
        user.setRole(role);
        user.setFullName(fullName);
        user.setPassword("drwajih");
        user.setActive(true);
        return user;
    }

    private Conversation conversation(Long id, LoginUser creator) {
        Conversation conversation = new Conversation();
        setId(conversation, id);
        conversation.setCreatedBy(creator);
        return conversation;
    }

    private ConversationParticipant participant(Conversation conversation, LoginUser user) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setConversation(conversation);
        participant.setUser(user);
        return participant;
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
