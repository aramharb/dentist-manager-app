package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationIdOrderBySentAtAsc(Long conversationId);
    Optional<ChatMessage> findTopByConversationIdOrderBySentAtDesc(Long conversationId);

    @Query("""
            select count(m) from ChatMessage m
            where m.conversation.id = :conversationId
              and m.sender.id <> :userId
              and (:lastReadAt is null or m.sentAt > :lastReadAt)
            """)
    long countUnread(@Param("conversationId") Long conversationId, @Param("userId") Long userId, @Param("lastReadAt") LocalDateTime lastReadAt);
}
