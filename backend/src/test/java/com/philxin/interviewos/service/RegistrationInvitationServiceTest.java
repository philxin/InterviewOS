package com.philxin.interviewos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.config.RegistrationInvitationProperties;
import com.philxin.interviewos.controller.dto.invitation.PublicRegistrationInvitationResponse;
import com.philxin.interviewos.controller.dto.invitation.RegistrationInvitationResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.RegistrationInvitation;
import com.philxin.interviewos.repository.AppUserRepository;
import com.philxin.interviewos.repository.RegistrationInvitationRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RegistrationInvitationServiceTest {

    @Mock
    private RegistrationInvitationRepository registrationInvitationRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RegistrationInvitationProperties registrationInvitationProperties;

    @InjectMocks
    private RegistrationInvitationService registrationInvitationService;

    @Test
    void createInvitationReturnsRegistrationLink() {
        AppUser inviter = buildUser();
        RegistrationInvitation savedInvitation = buildInvitation();
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(inviter));
        when(appUserRepository.existsByEmail("invitee@example.com")).thenReturn(false);
        when(registrationInvitationProperties.getTtl()).thenReturn(Duration.ofDays(7));
        when(registrationInvitationProperties.getRegistrationPathPrefix()).thenReturn("/invite/");
        when(registrationInvitationRepository.save(any(RegistrationInvitation.class))).thenReturn(savedInvitation);

        RegistrationInvitationResponse response = registrationInvitationService.createInvitation(
            AuthenticatedUser.fromEntity(inviter),
            " Invitee@example.com "
        );

        assertEquals("11111111-1111-1111-1111-111111111111", response.getInvitationCode());
        assertEquals("invitee@example.com", response.getInviteeEmail());
        assertEquals("/invite/11111111-1111-1111-1111-111111111111", response.getRegistrationPath());

        ArgumentCaptor<RegistrationInvitation> invitationCaptor = ArgumentCaptor.forClass(RegistrationInvitation.class);
        verify(registrationInvitationRepository).save(invitationCaptor.capture());
        assertEquals("invitee@example.com", invitationCaptor.getValue().getInviteeEmail());
    }

    @Test
    void createInvitationRegisteredEmailThrows409() {
        AppUser inviter = buildUser();
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(inviter));
        when(appUserRepository.existsByEmail("invitee@example.com")).thenReturn(true);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> registrationInvitationService.createInvitation(AuthenticatedUser.fromEntity(inviter), "invitee@example.com")
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Email already registered", exception.getMessage());
    }

    @Test
    void getInvitationReturnsPublicDetails() {
        RegistrationInvitation invitation = buildInvitation();
        when(registrationInvitationRepository.findById(UUID.fromString("11111111-1111-1111-1111-111111111111")))
            .thenReturn(Optional.of(invitation));

        PublicRegistrationInvitationResponse response =
            registrationInvitationService.getInvitation("11111111-1111-1111-1111-111111111111");

        assertEquals("invitee@example.com", response.getInviteeEmail());
        assertEquals("11111111-1111-1111-1111-111111111111", response.getInvitationCode());
    }

    @Test
    void lockInvitationForRegistrationRejectsMismatchedEmail() {
        RegistrationInvitation invitation = buildInvitation();
        when(registrationInvitationRepository.findByIdForUpdate(UUID.fromString("11111111-1111-1111-1111-111111111111")))
            .thenReturn(Optional.of(invitation));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> registrationInvitationService.lockInvitationForRegistration(
                "11111111-1111-1111-1111-111111111111",
                "other@example.com"
            )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Invitation does not match email", exception.getMessage());
    }

    @Test
    void getInvitationRejectsExpiredInvitation() {
        RegistrationInvitation invitation = buildInvitation();
        invitation.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(registrationInvitationRepository.findById(UUID.fromString("11111111-1111-1111-1111-111111111111")))
            .thenReturn(Optional.of(invitation));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> registrationInvitationService.getInvitation("11111111-1111-1111-1111-111111111111")
        );

        assertEquals(HttpStatus.GONE, exception.getStatus());
        assertEquals("Invitation has expired", exception.getMessage());
    }

    @Test
    void markAsUsedMarksInvitationConsumed() {
        RegistrationInvitation invitation = buildInvitation();
        AppUser usedBy = new AppUser();
        usedBy.setId(99L);

        registrationInvitationService.markAsUsed(invitation, usedBy);

        assertEquals(99L, invitation.getUsedBy().getId());
        assertTrue(invitation.getUsedAt() != null);
    }

    private AppUser buildUser() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail("owner@example.com");
        user.setDisplayName("owner");
        return user;
    }

    private RegistrationInvitation buildInvitation() {
        RegistrationInvitation invitation = new RegistrationInvitation();
        invitation.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        invitation.setInviteeEmail("invitee@example.com");
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        invitation.setInviter(buildUser());
        return invitation;
    }
}
