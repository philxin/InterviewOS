package com.philxin.interviewos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.auth.AuthResponse;
import com.philxin.interviewos.controller.dto.auth.AuthUserResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.repository.AppUserRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesUserAndReturnsToken() {
        AppUser savedUser = buildUser();
        when(appUserRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-password");
        when(appUserRepository.save(any(AppUser.class))).thenReturn(savedUser);
        when(jwtTokenService.generateToken(savedUser)).thenReturn("jwt-token");
        when(jwtTokenService.getExpiresInSeconds()).thenReturn(7200L);

        AuthResponse response = authService.register("User@example.com", "Password123!", " philxin ");

        assertEquals("jwt-token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("user@example.com", response.getUser().getEmail());
        assertEquals("philxin", response.getUser().getDisplayName());
        verify(appUserRepository).save(any(AppUser.class));
    }

    @Test
    void registerDuplicateEmailThrows409() {
        when(appUserRepository.existsByEmail("user@example.com")).thenReturn(true);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.register("user@example.com", "Password123!", "philxin")
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Email already registered", exception.getMessage());
    }

    @Test
    void loginInvalidPasswordThrows401() {
        AppUser user = buildUser();
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("WrongPassword123!", "hashed-password")).thenReturn(false);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.login("user@example.com", "WrongPassword123!")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void getCurrentUserReturnsPrincipalSnapshot() {
        AuthenticatedUser authenticatedUser = AuthenticatedUser.fromEntity(buildUser());

        AuthUserResponse response = authService.getCurrentUser(authenticatedUser);

        assertEquals(1L, response.getId());
        assertEquals("user@example.com", response.getEmail());
    }

    private AppUser buildUser() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setDisplayName("philxin");
        user.setPasswordHash("hashed-password");
        return user;
    }
}
