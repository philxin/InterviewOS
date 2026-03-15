package com.philxin.interviewos.service;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.auth.AuthResponse;
import com.philxin.interviewos.controller.dto.auth.AuthUserResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.repository.AppUserRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.security.JwtTokenService;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证相关核心业务逻辑。
 */
@Service
public class AuthService {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
        AppUserRepository appUserRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenService jwtTokenService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public AuthResponse register(String email, String password, String displayName) {
        String normalizedEmail = normalizeEmail(email);
        if (appUserRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Email already registered");
        }

        AppUser user = new AppUser();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(normalizeText(displayName));

        AppUser savedUser = appUserRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(String email, String password) {
        AppUser user = appUserRepository.findByEmail(normalizeEmail(email))
            .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse getCurrentUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return AuthUserResponse.fromPrincipal(authenticatedUser);
    }

    private AuthResponse buildAuthResponse(AppUser user) {
        AuthResponse response = new AuthResponse();
        response.setToken(jwtTokenService.generateToken(user));
        response.setTokenType(JwtTokenService.TOKEN_TYPE);
        response.setExpiresIn(jwtTokenService.getExpiresInSeconds());
        response.setUser(AuthUserResponse.fromEntity(user));
        return response;
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    private String normalizeEmail(String email) {
        return normalizeText(email).toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }
}
