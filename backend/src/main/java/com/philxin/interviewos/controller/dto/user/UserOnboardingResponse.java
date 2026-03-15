package com.philxin.interviewos.controller.dto.user;

import com.philxin.interviewos.entity.AppUser;

/**
 * 当前用户训练方向初始化响应。
 */
public class UserOnboardingResponse {
    private Long id;
    private String targetRole;

    public static UserOnboardingResponse fromEntity(AppUser user) {
        UserOnboardingResponse response = new UserOnboardingResponse();
        response.setId(user.getId());
        response.setTargetRole(user.getTargetRole() == null ? null : user.getTargetRole().name());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }
}
