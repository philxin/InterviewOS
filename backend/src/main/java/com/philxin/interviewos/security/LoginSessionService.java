package com.philxin.interviewos.security;

import com.philxin.interviewos.config.LoginSessionProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 统一维护登录会话缓存，基于 Redis 实现一周有效期和访问续期。
 */
@Service
public class LoginSessionService {
    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final StringRedisTemplate stringRedisTemplate;
    private final LoginSessionProperties loginSessionProperties;

    public LoginSessionService(
        StringRedisTemplate stringRedisTemplate,
        LoginSessionProperties loginSessionProperties
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.loginSessionProperties = loginSessionProperties;
    }

    /**
     * 登录成功后创建服务端会话，后续接口访问通过该会话判断 token 是否仍然有效。
     */
    public void cacheSession(String token, Long userId) {
        stringRedisTemplate.opsForValue().set(
            buildSessionKey(token),
            String.valueOf(userId),
            loginSessionProperties.getTtl()
        );
    }

    /**
     * 校验 token 对应的会话是否存在，并在命中后刷新 TTL，实现滑动过期。
     */
    public boolean refreshSession(String token, Long userId) {
        String sessionKey = buildSessionKey(token);
        String cachedUserId = stringRedisTemplate.opsForValue().get(sessionKey);
        if (cachedUserId == null || !cachedUserId.equals(String.valueOf(userId))) {
            return false;
        }
        Boolean refreshed = stringRedisTemplate.expire(sessionKey, loginSessionProperties.getTtl());
        return Boolean.TRUE.equals(refreshed);
    }

    /**
     * 用户状态异常时主动清理会话，避免 Redis 中残留无效登录态。
     */
    public void deleteSession(String token) {
        stringRedisTemplate.delete(buildSessionKey(token));
    }

    public long getExpiresInSeconds() {
        return loginSessionProperties.getTtl().toSeconds();
    }

    private String buildSessionKey(String token) {
        return loginSessionProperties.getKeyPrefix() + sha256(token);
    }

    private String sha256(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HEX_FORMAT.formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
