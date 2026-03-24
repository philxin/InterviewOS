package com.philxin.interviewos.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.philxin.interviewos.config.LoginSessionProperties;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class LoginSessionServiceTest {
    private static final String SESSION_KEY =
        "auth:test:session:637dca1ed85901f74d2634ec978c3e441598b7cc2f86a2b9a004662222009808";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private ValueOperations<String, String> valueOperations;

    private LoginSessionService loginSessionService;

    @BeforeEach
    void setUp() {
        valueOperations = mock(ValueOperations.class);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        LoginSessionProperties loginSessionProperties = new LoginSessionProperties();
        loginSessionProperties.setTtl(Duration.ofDays(7));
        loginSessionProperties.setKeyPrefix("auth:test:session:");
        loginSessionService = new LoginSessionService(stringRedisTemplate, loginSessionProperties);
    }

    @Test
    void cacheSessionWritesUserIdWithOneWeekTtl() {
        loginSessionService.cacheSession("jwt-token", 42L);

        verify(valueOperations).set(
            SESSION_KEY,
            "42",
            Duration.ofDays(7)
        );
    }

    @Test
    void refreshSessionReturnsFalseWhenSessionMissing() {
        when(valueOperations.get(SESSION_KEY))
            .thenReturn(null);

        boolean refreshed = loginSessionService.refreshSession("jwt-token", 42L);

        assertFalse(refreshed);
    }

    @Test
    void refreshSessionReturnsFalseWhenUserIdMismatch() {
        when(valueOperations.get(SESSION_KEY))
            .thenReturn("7");

        boolean refreshed = loginSessionService.refreshSession("jwt-token", 42L);

        assertFalse(refreshed);
    }

    @Test
    void refreshSessionExtendsTtlWhenSessionMatches() {
        when(valueOperations.get(SESSION_KEY))
            .thenReturn("42");
        when(stringRedisTemplate.expire(
            SESSION_KEY,
            Duration.ofDays(7)
        )).thenReturn(true);

        boolean refreshed = loginSessionService.refreshSession("jwt-token", 42L);

        assertTrue(refreshed);
    }

    @Test
    void getExpiresInSecondsReturnsOneWeek() {
        assertEquals(604800L, loginSessionService.getExpiresInSeconds());
    }
}
