package com.philxin.interviewos.controller.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求。
 */
public class RegisterRequest {
    private static final String PASSWORD_PATTERN =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,64}$";

    @NotBlank(message = "invitationCode must not be blank")
    private String invitationCode;

    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be a valid email address")
    @Size(max = 255, message = "email length must be <= 255")
    private String email;

    @NotBlank(message = "password must not be blank")
    @Pattern(
        regexp = PASSWORD_PATTERN,
        message = "password must include uppercase, lowercase, digit and special character with length 8-64"
    )
    private String password;

    @NotBlank(message = "displayName must not be blank")
    @Size(max = 50, message = "displayName length must be <= 50")
    private String displayName;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getInvitationCode() {
        return invitationCode;
    }

    public void setInvitationCode(String invitationCode) {
        this.invitationCode = invitationCode;
    }
}
