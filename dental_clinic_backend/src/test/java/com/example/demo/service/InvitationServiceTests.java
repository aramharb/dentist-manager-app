package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.demo.dto.InvitationDto;
import com.example.demo.entity.LoginUser;
import com.example.demo.entity.UserInvitation;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CabinetRepository;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.repository.UserInvitationRepository;

class InvitationServiceTests {
    private UserInvitationRepository invitations;
    private LoginUserRepository users;
    private InvitationService service;
    private LoginUser user;

    @BeforeEach
    void setUp() {
        invitations = mock(UserInvitationRepository.class);
        users = mock(LoginUserRepository.class);
        service = new InvitationService(invitations, users, mock(CabinetRepository.class));
        user = new LoginUser();
        user.setUsername("joy");
        user.setFullName("Nurse Joy");
        user.setRole("secretaire");
        user.setPassword("unusable");
        user.setActive(true);
    }

    @Test
    void createdLinkIsStoredOnlyAsHashAndOlderLinksAreClosed() {
        InvitationDto.Created created = service.create(user, 1L);

        ArgumentCaptor<UserInvitation> saved = ArgumentCaptor.forClass(UserInvitation.class);
        verify(invitations).save(saved.capture());
        String token = created.path().substring("/invite/".length());
        assertEquals(64, saved.getValue().getTokenHash().length());
        assertTrue(!saved.getValue().getTokenHash().contains(token));
        verify(invitations).closeOpenInvitations(any(), any());
    }

    @Test
    void acceptSetsThePasswordChosenByTheUser() {
        UserInvitation invitation = invitation(LocalDateTime.now().plusHours(1), null);
        when(invitations.findByTokenHash(anyString())).thenReturn(Optional.of(invitation));

        service.accept("token", "my-new-password");

        assertTrue(com.example.demo.security.PasswordHasher.matches("my-new-password", user.getPassword()));
        verify(users).save(user);
    }

    @Test
    void expiredUsedOrUnknownLinksAreRejectedTheSameWay() {
        when(invitations.findByTokenHash(anyString())).thenReturn(
                Optional.of(invitation(LocalDateTime.now().minusMinutes(1), null)),
                Optional.of(invitation(LocalDateTime.now().plusHours(1), LocalDateTime.now())),
                Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.preview("expired"));
        assertThrows(ResourceNotFoundException.class, () -> service.preview("used"));
        assertThrows(ResourceNotFoundException.class, () -> service.preview("unknown"));
    }

    @Test
    void shortPasswordIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.accept("token", "123"));
    }

    private UserInvitation invitation(LocalDateTime expiresAt, LocalDateTime usedAt) {
        UserInvitation invitation = new UserInvitation();
        invitation.setUser(user);
        invitation.setExpiresAt(expiresAt);
        invitation.setUsedAt(usedAt);
        return invitation;
    }
}
