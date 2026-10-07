package com.example.demo.entity;

import java.io.Serializable;
import java.util.Objects;

public class ConversationParticipantId implements Serializable {
    private Long conversation;
    private Long user;

    public ConversationParticipantId() {}
    public ConversationParticipantId(Long conversation, Long user) {
        this.conversation = conversation;
        this.user = user;
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConversationParticipantId that)) return false;
        return Objects.equals(conversation, that.conversation) && Objects.equals(user, that.user);
    }
    @Override public int hashCode() { return Objects.hash(conversation, user); }
}
