package com.philxin.interviewos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置统一收口到配置类，避免散落硬编码。
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {
    private String secret;
    private String issuer = "InterviewOS";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
