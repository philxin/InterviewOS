package com.philxin.interviewos.security;

import com.philxin.interviewos.entity.AppUser;

/**
 * 已认证用户上下文，避免 Controller 暴露实体。
 */
public class AuthenticatedUser {
    private final Long id;
    private final String email;
    private final String displayName;
    private final String targetRole;

    private AuthenticatedUser(Long id, String email, String displayName, String targetRole) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.targetRole = targetRole;
    }

    public static AuthenticatedUser fromEntity(AppUser user) {
        return new AuthenticatedUser(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            user.getTargetRole() == null ? null : user.getTargetRole().name()
        );
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTargetRole() {
        return targetRole;
    }
}
