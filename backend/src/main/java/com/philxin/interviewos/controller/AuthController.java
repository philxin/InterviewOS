package com.philxin.interviewos.controller;

import com.philxin.interviewos.common.Result;
import com.philxin.interviewos.controller.dto.auth.AuthResponse;
import com.philxin.interviewos.controller.dto.auth.AuthUserResponse;
import com.philxin.interviewos.controller.dto.auth.LoginRequest;
import com.philxin.interviewos.controller.dto.auth.RegisterRequest;
import com.philxin.interviewos.controller.dto.invitation.PublicRegistrationInvitationResponse;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.AuthService;
import com.philxin.interviewos.service.RegistrationInvitationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final RegistrationInvitationService registrationInvitationService;

    public AuthController(
        AuthService authService,
        RegistrationInvitationService registrationInvitationService
    ) {
        this.authService = authService;
        this.registrationInvitationService = registrationInvitationService;
    }

    /**
     * 用户注册并直接返回登录态。
     */
    @PostMapping("/register")
    public ResponseEntity<Result<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(
            request.getInvitationCode(),
            request.getEmail(),
            request.getPassword(),
            request.getDisplayName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(response));
    }

    /**
     * 用户登录。
     */
    @PostMapping("/login")
    public ResponseEntity<Result<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(Result.success(response));
    }

    /**
     * 公开查询邀请是否有效，供被邀请人打开注册链接时预校验。
     */
    @GetMapping("/invitations/{invitationCode}")
    public ResponseEntity<Result<PublicRegistrationInvitationResponse>> getInvitation(
        @PathVariable String invitationCode
    ) {
        return ResponseEntity.ok(Result.success(registrationInvitationService.getInvitation(invitationCode)));
    }

    /**
     * 获取当前登录用户信息。
     */
    @GetMapping("/me")
    public ResponseEntity<Result<AuthUserResponse>> me(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(Result.success(authService.getCurrentUser(authenticatedUser)));
    }
}
