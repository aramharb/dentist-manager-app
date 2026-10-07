package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.demo.dto.MessageDto;
import com.example.demo.dto.UserDto;
import com.example.demo.entity.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.*;

@Service
public class MessageService {
    private static final Set<String> MESSAGING_ROLES = Set.of("doctor", "secretaire");

    private final LoginUserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final MessageNotificationRepository notificationRepository;
    private final MessageRealtimeNotifier realtimeNotifier;
    private final UserPresenceService presenceService;

    public MessageService(LoginUserRepository userRepository, ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository, ChatMessageRepository messageRepository,
            MessageNotificationRepository notificationRepository, MessageRealtimeNotifier realtimeNotifier,
            UserPresenceService presenceService) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
        this.realtimeNotifier = realtimeNotifier;
        this.presenceService = presenceService;
    }

    @Transactional(readOnly = true)
    public List<MessageDto.ConversationResponse> conversations(Long userId) {
        requireUser(userId);
        return conversationRepository.findForUser(userId).stream().map(conversation -> toConversation(conversation, userId)).toList();
    }

    @Transactional
    public MessageDto.ConversationResponse create(MessageDto.CreateConversationRequest request, Long creatorId) {
        LoginUser creator = requireUser(creatorId);
        Set<Long> participantIds = new LinkedHashSet<>(request.participantIds());
        participantIds.add(creator.getId());
        participantIds.stream().sorted().forEach(id -> userRepository.lockMessagingUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id)));
        participantIds.stream().map(this::requireUser).forEach(user -> requireCanMessage(creator, user));
        if (participantIds.size() == 2) {
            Conversation existing = findDirectConversation(creator.getId(), participantIds);
            if (existing != null) {
                if (request.message() != null && !request.message().trim().isBlank()) {
                    send(existing.getId(), creator.getId(), request.message());
                }
                return toConversation(existing, creator.getId());
            }
        }

        Conversation conversation = new Conversation();
        conversation.setCreatedBy(creator);
        conversation.setTitle(normalize(request.title()));
        Conversation saved = conversationRepository.save(conversation);

        for (Long userId : participantIds) {
            ConversationParticipant participant = new ConversationParticipant();
            participant.setConversation(saved);
            participant.setUser(requireUser(userId));
            if (userId.equals(creator.getId())) {
                participant.setLastReadAt(LocalDateTime.now());
            }
            participantRepository.save(participant);
        }
        if (request.message() != null && !request.message().trim().isBlank()) {
            send(saved.getId(), creator.getId(), request.message());
        }
        return toConversation(saved, creator.getId());
    }

    @Transactional(readOnly = true)
    public List<MessageDto.MessageResponse> messages(Long conversationId, Long userId) {
        requireParticipant(conversationId, userId);
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId).stream()
                .map(message -> toMessage(message, userId))
                .toList();
    }

    @Transactional
    public MessageDto.MessageResponse send(Long conversationId, Long senderId, String body) {
        requireParticipant(conversationId, senderId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        LoginUser sender = requireUser(senderId);
        participantRepository.findByConversationId(conversationId).stream()
                .map(ConversationParticipant::getUser)
                .forEach(user -> requireCanMessage(sender, user));
        message.setSender(sender);
        if (body == null || body.isBlank() || body.trim().length() > 4000) {
            throw new IllegalArgumentException("Messages must contain between 1 and 4000 characters.");
        }
        message.setBody(body.trim());
        ChatMessage saved = messageRepository.save(message);
        createRecipientNotifications(conversation, saved, sender.getId());
        conversation.touch();
        conversationRepository.save(conversation);
        markRead(conversationId, senderId);
        return toMessage(saved, senderId);
    }

    @Transactional
    public void markRead(Long conversationId, Long userId) {
        ConversationParticipant participant = participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation participant", conversationId));
        participant.setLastReadAt(LocalDateTime.now());
        participantRepository.save(participant);
        notificationRepository.markConversationRead(conversationId, userId, participant.getLastReadAt());
    }

    private MessageDto.ConversationResponse toConversation(Conversation conversation, Long viewerId) {
        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversation.getId());
        MessageDto.MessageResponse lastMessage = messageRepository.findTopByConversationIdOrderBySentAtDesc(conversation.getId())
                .map(message -> toMessage(message, viewerId))
                .orElse(null);
        String title = conversation.getTitle();
        if (participants.size() == 2 || title == null || title.isBlank()) {
            title = participants.stream()
                    .map(ConversationParticipant::getUser)
                    .filter(user -> !user.getId().equals(viewerId))
                    .map(LoginUser::getFullName)
                    .findFirst()
                    .orElse("Conversation");
        }
        return new MessageDto.ConversationResponse(
                conversation.getId(),
                title,
                participants.stream().map(ConversationParticipant::getUser).map(this::toUser).toList(),
                lastMessage,
                unreadFor(conversation.getId(), viewerId),
                conversation.getUpdatedAt());
    }

    private MessageDto.MessageResponse toMessage(ChatMessage message, Long viewerId) {
        LoginUser sender = message.getSender();
        List<ConversationParticipant> participants = participantRepository.findByConversationId(message.getConversation().getId());
        LoginUser recipient = participants.stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.getId().equals(sender.getId()))
                .findFirst()
                .orElse(null);
        boolean read = participants.stream()
                .filter(participant -> !participant.getUser().getId().equals(sender.getId()))
                .allMatch(participant -> participant.getLastReadAt() != null
                        && !participant.getLastReadAt().isBefore(message.getSentAt()));
        return new MessageDto.MessageResponse(message.getId(), message.getConversation().getId(), sender.getId(),
                sender.getFullName(), sender.getRole(), recipient == null ? null : recipient.getId(),
                recipient == null ? null : recipient.getFullName(), message.getBody(), message.getSentAt(), read,
                sender.getId().equals(viewerId));
    }

    private long unreadFor(Long conversationId, Long userId) {
        requireParticipant(conversationId, userId);
        return notificationRepository.countByConversationIdAndRecipientIdAndReadAtIsNull(conversationId, userId);
    }

    private LoginUser requireUser(Long id) {
        return userRepository.findById(id).filter(user -> Boolean.TRUE.equals(user.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void requireParticipant(Long conversationId, Long userId) {
        if (!participantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ResourceNotFoundException("Conversation", conversationId);
        }
    }

    private UserDto.Response toUser(LoginUser user) {
        return new UserDto.Response(user.getId(), user.getUsername(), user.getFullName(), user.getRole(),
                presenceService.isOnline(user.getUsername()));
    }

    private Conversation findDirectConversation(Long viewerId, Set<Long> participantIds) {
        return conversationRepository.findForUser(viewerId).stream()
                .filter(conversation -> {
                    List<ConversationParticipant> participants = participantRepository.findByConversationId(conversation.getId());
                    if (participants.size() != participantIds.size()) {
                        return false;
                    }
                    return participants.stream()
                            .map(participant -> participant.getUser().getId())
                            .allMatch(participantIds::contains);
                })
                .findFirst()
                .orElse(null);
    }

    private void createRecipientNotifications(Conversation conversation, ChatMessage message, Long senderId) {
        participantRepository.findByConversationId(conversation.getId()).stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.getId().equals(senderId))
                .forEach(user -> {
                    MessageNotification notification = new MessageNotification();
                    notification.setConversation(conversation);
                    notification.setMessage(message);
                    notification.setRecipient(user);
                    notificationRepository.save(notification);
                    sendAfterCommit(user.getUsername(), toMessage(message, user.getId()));
                });
    }

    private void sendAfterCommit(String username, MessageDto.MessageResponse message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            realtimeNotifier.send(username, message);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                realtimeNotifier.send(username, message);
            }
        });
    }

    private void requireCanMessage(LoginUser sender, LoginUser recipient) {
        String senderRole = normalizeRole(sender.getRole());
        String recipientRole = normalizeRole(recipient.getRole());
        if (!MESSAGING_ROLES.contains(senderRole) || !MESSAGING_ROLES.contains(recipientRole)) {
            throw new MessagingAccessDeniedException("Messaging is not allowed for this user role.");
        }
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toLowerCase();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
