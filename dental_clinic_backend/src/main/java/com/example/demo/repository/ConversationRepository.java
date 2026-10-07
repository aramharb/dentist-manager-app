package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @Query("""
            select c from Conversation c
            join ConversationParticipant p on p.conversation = c
            where p.user.id = :userId
            order by c.updatedAt desc
            """)
    List<Conversation> findForUser(@Param("userId") Long userId);
}
