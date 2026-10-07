package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.InvitationDto;
import com.example.demo.entity.LoginUser;
import com.example.demo.entity.UserInvitation;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CabinetRepository;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.repository.UserInvitationRepository;

/**
 * One-time links that let a person choose their own password, so no password has to be
 * typed by an admin or manager and passed around.
 */
@Service
public class InvitationService {
    static final int MIN_PASSWORD_LENGTH = 6;
    private static final long VALID_HOURS = 72;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserInvitationRepository invitationRepository;
    private final LoginUserRepository userRepository;
    private final CabinetRepository cabinetRepository;

    public InvitationService(UserInvitationRepository invitationRepository, LoginUserRepository userRepository,
            CabinetRepository cabinetRepository) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.cabinetRepository = cabinetRepository;
    }

    /** Creates a fresh link for the user; links created earlier stop working. */
    @Transactional
    public InvitationDto.Created create(LoginUser user, Long createdBy) {
        invitationRepository.closeOpenInvitations(user.getId(), LocalDateTime.now());
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        UserInvitation invitation = new UserInvitation();
        invitation.setUser(user);
        invitation.setTokenHash(hash(token));
        invitation.setCreatedBy(createdBy);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(VALID_HOURS));
        invitationRepository.save(invitation);
        return new InvitationDto.Created(user.getId(), user.getUsername(), user.getFullName(),
                "/invite/" + token, invitation.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public InvitationDto.Preview preview(String token) {
        LoginUser user = openInvitation(token).getUser();
        String cabinetName = user.getCabinetId() == null ? null
                : cabinetRepository.findById(user.getCabinetId()).map(c -> c.getName()).orElse(null);
        return new InvitationDto.Preview(user.getUsername(), user.getFullName(), user.getRole(), cabinetName);
    }

    @Transactional
    public void accept(String token, String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > 255) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        UserInvitation invitation = openInvitation(token);
        LoginUser user = invitation.getUser();
        user.setPassword(password);
        userRepository.save(user);
        invitationRepository.closeOpenInvitations(user.getId(), LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public boolean hasOpenInvitation(Long userId) {
        return invitationRepository.hasOpenInvitation(userId, LocalDateTime.now());
    }

    /** The same answer for unknown, used and expired links, so nothing can be probed. */
    private UserInvitation openInvitation(String token) {
        UserInvitation invitation = invitationRepository.findByTokenHash(hash(token == null ? "" : token))
                .filter(candidate -> candidate.getUsedAt() == null)
                .filter(candidate -> candidate.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(candidate -> Boolean.TRUE.equals(candidate.getUser().getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", 0L));
        return invitation;
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
