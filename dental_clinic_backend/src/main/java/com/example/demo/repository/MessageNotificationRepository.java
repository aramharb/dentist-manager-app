package com.example.demo.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.MessageNotification;

public interface MessageNotificationRepository extends JpaRepository<MessageNotification, Long> {
    long countByConversationIdAndRecipientIdAndReadAtIsNull(Long conversationId, Long recipientId);
    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    @Modifying
    @Query("""
            update MessageNotification n
            set n.readAt = :readAt
            where n.conversation.id = :conversationId
              and n.recipient.id = :recipientId
              and n.readAt is null
            """)
    int markConversationRead(@Param("conversationId") Long conversationId,
            @Param("recipientId") Long recipientId,
            @Param("readAt") LocalDateTime readAt);
}
