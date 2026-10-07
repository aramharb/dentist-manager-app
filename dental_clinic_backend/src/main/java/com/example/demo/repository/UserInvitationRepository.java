package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.UserInvitation;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, Long> {
    @Query("select i from UserInvitation i join fetch i.user where i.tokenHash = :hash")
    Optional<UserInvitation> findByTokenHash(@Param("hash") String hash);

    /** Closes every still-open invitation of a user (a new link replaces the old ones). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UserInvitation i set i.usedAt = :now where i.user.id = :userId and i.usedAt is null")
    int closeOpenInvitations(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("select count(i) > 0 from UserInvitation i where i.user.id = :userId and i.usedAt is null and i.expiresAt > :now")
    boolean hasOpenInvitation(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
