package com.philxin.interviewos.controller;

import com.philxin.interviewos.common.Result;
import com.philxin.interviewos.controller.dto.user.UpdateOnboardingRequest;
import com.philxin.interviewos.controller.dto.user.UserOnboardingResponse;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 保存当前登录用户的训练方向。
     */
    @PatchMapping("/me/onboarding")
    public ResponseEntity<Result<UserOnboardingResponse>> updateOnboarding(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @Valid @RequestBody UpdateOnboardingRequest request
    ) {
        UserOnboardingResponse response = userService.updateOnboarding(
            authenticatedUser,
            request.getTargetRole()
        );
        return ResponseEntity.ok(Result.success(response));
    }
}
