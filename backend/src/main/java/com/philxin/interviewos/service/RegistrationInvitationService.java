package com.philxin.interviewos.service;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.config.RegistrationInvitationProperties;
import com.philxin.interviewos.controller.dto.invitation.PublicRegistrationInvitationResponse;
import com.philxin.interviewos.controller.dto.invitation.RegistrationInvitationResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.RegistrationInvitation;
import com.philxin.interviewos.repository.AppUserRepository;
import com.philxin.interviewos.repository.RegistrationInvitationRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 邀请注册核心业务，负责邀请码创建、校验和核销。
 */
@Service
public class RegistrationInvitationService {
    private final RegistrationInvitationRepository registrationInvitationRepository;
    private final AppUserRepository appUserRepository;
    private final RegistrationInvitationProperties registrationInvitationProperties;

    public RegistrationInvitationService(
        RegistrationInvitationRepository registrationInvitationRepository,
        AppUserRepository appUserRepository,
        RegistrationInvitationProperties registrationInvitationProperties
    ) {
        this.registrationInvitationRepository = registrationInvitationRepository;
        this.appUserRepository = appUserRepository;
        this.registrationInvitationProperties = registrationInvitationProperties;
    }

    @Transactional
    public RegistrationInvitationResponse createInvitation(AuthenticatedUser authenticatedUser, String email) {
        AppUser inviter = getCurrentUserEntity(authenticatedUser);
        String normalizedEmail = normalizeEmail(email);
        if (appUserRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Email already registered");
        }

        RegistrationInvitation invitation = new RegistrationInvitation();
        invitation.setInviter(inviter);
        invitation.setInviteeEmail(normalizedEmail);
        invitation.setExpiresAt(LocalDateTime.now().plus(registrationInvitationProperties.getTtl()));

        RegistrationInvitation savedInvitation = registrationInvitationRepository.save(invitation);
        return buildInvitationResponse(savedInvitation);
    }

    @Transactional(readOnly = true)
    public PublicRegistrationInvitationResponse getInvitation(String invitationCode) {
        RegistrationInvitation invitation = registrationInvitationRepository.findById(parseInvitationCode(invitationCode))
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Invitation not found"));
        validateInvitation(invitation);

        PublicRegistrationInvitationResponse response = new PublicRegistrationInvitationResponse();
        response.setInvitationCode(invitation.getId().toString());
        response.setInviteeEmail(invitation.getInviteeEmail());
        response.setExpiresAt(invitation.getExpiresAt());
        return response;
    }

    public RegistrationInvitation lockInvitationForRegistration(String invitationCode, String email) {
        RegistrationInvitation invitation = registrationInvitationRepository.findByIdForUpdate(parseInvitationCode(invitationCode))
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Invitation not found"));
        validateInvitation(invitation);

        if (!invitation.getInviteeEmail().equals(normalizeEmail(email))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Invitation does not match email");
        }
        return invitation;
    }

    public void markAsUsed(RegistrationInvitation invitation, AppUser usedBy) {
        invitation.setUsedBy(usedBy);
        invitation.setUsedAt(LocalDateTime.now());
    }

    private RegistrationInvitationResponse buildInvitationResponse(RegistrationInvitation invitation) {
        RegistrationInvitationResponse response = new RegistrationInvitationResponse();
        response.setInvitationCode(invitation.getId().toString());
        response.setInviteeEmail(invitation.getInviteeEmail());
        response.setRegistrationPath(buildRegistrationPath(invitation.getId()));
        response.setExpiresAt(invitation.getExpiresAt());
        return response;
    }

    private void validateInvitation(RegistrationInvitation invitation) {
        if (invitation.getUsedAt() != null) {
            throw new BusinessException(HttpStatus.GONE, "Invitation has already been used");
        }
        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.GONE, "Invitation has expired");
        }
    }

    private AppUser getCurrentUserEntity(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return appUserRepository.findById(authenticatedUser.getId())
            .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }

    private UUID parseInvitationCode(String invitationCode) {
        try {
            return UUID.fromString(normalizeText(invitationCode));
        } catch (RuntimeException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid invitation code");
        }
    }

    private String buildRegistrationPath(UUID invitationId) {
        String pathPrefix = normalizeText(registrationInvitationProperties.getRegistrationPathPrefix());
        if (pathPrefix == null || pathPrefix.isEmpty()) {
            pathPrefix = "/invite/";
        }
        if (!pathPrefix.startsWith("/")) {
            pathPrefix = "/" + pathPrefix;
        }
        return pathPrefix.endsWith("/") ? pathPrefix + invitationId : pathPrefix + "/" + invitationId;
    }

    private String normalizeEmail(String email) {
        return normalizeText(email).toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }
}
