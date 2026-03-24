package com.philxin.interviewos.controller.dto.invitation;

import java.time.LocalDateTime;

/**
 * 公开邀请信息响应，用于邀请注册页预校验。
 */
public class PublicRegistrationInvitationResponse {
    private String invitationCode;
    private String inviteeEmail;
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

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
