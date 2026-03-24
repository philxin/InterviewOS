package com.philxin.interviewos.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 邀请注册配置，统一控制邀请码有效期和前端注册链接前缀。
 */
@ConfigurationProperties(prefix = "app.security.registration-invitation")
public class RegistrationInvitationProperties {
    private Duration ttl = Duration.ofDays(7);
    private String registrationPathPrefix = "/invite/";

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public String getRegistrationPathPrefix() {
        return registrationPathPrefix;
    }

    public void setRegistrationPathPrefix(String registrationPathPrefix) {
        this.registrationPathPrefix = registrationPathPrefix;
    }
}
