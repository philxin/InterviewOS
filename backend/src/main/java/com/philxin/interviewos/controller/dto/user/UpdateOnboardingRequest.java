package com.philxin.interviewos.controller.dto.user;

import com.philxin.interviewos.entity.TargetRole;
import jakarta.validation.constraints.NotNull;

/**
 * 当前用户训练方向初始化请求。
 */
public class UpdateOnboardingRequest {
    @NotNull(message = "targetRole must not be null")
    private TargetRole targetRole;

    public TargetRole getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(TargetRole targetRole) {
        this.targetRole = targetRole;
    }
}
