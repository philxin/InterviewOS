package com.philxin.interviewos.controller;

import com.philxin.interviewos.common.Result;
import com.philxin.interviewos.controller.dto.invitation.CreateRegistrationInvitationRequest;
import com.philxin.interviewos.controller.dto.invitation.RegistrationInvitationResponse;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.RegistrationInvitationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/invitations")
public class RegistrationInvitationController {
    private final RegistrationInvitationService registrationInvitationService;

    public RegistrationInvitationController(RegistrationInvitationService registrationInvitationService) {
        this.registrationInvitationService = registrationInvitationService;
    }

    /**
     * 由已登录用户创建注册邀请链接。
     */
    @PostMapping
    public ResponseEntity<Result<RegistrationInvitationResponse>> createInvitation(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @Valid @RequestBody CreateRegistrationInvitationRequest request
    ) {
        RegistrationInvitationResponse response = registrationInvitationService.createInvitation(
            authenticatedUser,
            request.getEmail()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(response));
    }
}
