package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "conversation_participant")
@IdClass(ConversationParticipantId.class)
public class ConversationParticipant {
    @Id @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    @Id @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private LoginUser user;
    private LocalDateTime lastReadAt;
    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @PrePersist void prePersist() { joinedAt = LocalDateTime.now(); }
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    public LoginUser getUser() { return user; }
    public void setUser(LoginUser user) { this.user = user; }
    public LocalDateTime getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(LocalDateTime lastReadAt) { this.lastReadAt = lastReadAt; }
}
