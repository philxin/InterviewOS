package com.philxin.interviewos.controller.dto.auth;

import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.security.AuthenticatedUser;

/**
 * 当前登录用户的对外信息。
 */
public class AuthUserResponse {
    private Long id;
    private String email;
    private String displayName;
    private String targetRole;

    public static AuthUserResponse fromEntity(AppUser user) {
        AuthUserResponse response = new AuthUserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setDisplayName(user.getDisplayName());
        response.setTargetRole(user.getTargetRole() == null ? null : user.getTargetRole().name());
        return response;
    }

    public static AuthUserResponse fromPrincipal(AuthenticatedUser authenticatedUser) {
        AuthUserResponse response = new AuthUserResponse();
        response.setId(authenticatedUser.getId());
        response.setEmail(authenticatedUser.getEmail());
        response.setDisplayName(authenticatedUser.getDisplayName());
        response.setTargetRole(authenticatedUser.getTargetRole());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }
}
