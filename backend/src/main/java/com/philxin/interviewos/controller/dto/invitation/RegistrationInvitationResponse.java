package com.philxin.interviewos.controller.dto.invitation;

import java.time.LocalDateTime;

/**
 * 创建邀请成功后的响应。
 */
public class RegistrationInvitationResponse {
    private String invitationCode;
    private String inviteeEmail;
    private String registrationPath;
    private LocalDateTime expiresAt;

    public String getInvitationCode() {
        return invitationCode;
    }

    public void setInvitationCode(String invitationCode) {
        this.invitationCode = invitationCode;
    }

    public String getInviteeEmail() {
        return inviteeEmail;
    }

    public void setInviteeEmail(String inviteeEmail) {
        this.inviteeEmail = inviteeEmail;
    }

    public String getRegistrationPath() {
        return registrationPath;
    }

    public void setRegistrationPath(String registrationPath) {
        this.registrationPath = registrationPath;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
