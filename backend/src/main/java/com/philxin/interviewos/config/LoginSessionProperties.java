package com.philxin.interviewos.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 登录会话配置，统一控制 Redis 登录态缓存和滑动续期时长。
 */
@ConfigurationProperties(prefix = "app.security.login-session")
public class LoginSessionProperties {
    private Duration ttl = Duration.ofDays(7);
    private String keyPrefix = "auth:session:";

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
}
