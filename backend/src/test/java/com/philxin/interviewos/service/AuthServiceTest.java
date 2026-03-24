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
import com.philxin.interviewos.entity.RegistrationInvitation;
import com.philxin.interviewos.repository.AppUserRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.security.JwtTokenService;
import com.philxin.interviewos.security.LoginSessionService;
import java.util.UUID;
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

    @Mock
    private LoginSessionService loginSessionService;

    @Mock
    private RegistrationInvitationService registrationInvitationService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesUserAndReturnsToken() {
        AppUser savedUser = buildUser();
        RegistrationInvitation invitation = buildInvitation();
        when(appUserRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(registrationInvitationService.lockInvitationForRegistration("invite-code", "user@example.com"))
            .thenReturn(invitation);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-password");
        when(appUserRepository.save(any(AppUser.class))).thenReturn(savedUser);
        when(jwtTokenService.generateToken(savedUser)).thenReturn("jwt-token");
        when(loginSessionService.getExpiresInSeconds()).thenReturn(604800L);

        AuthResponse response = authService.register("invite-code", "User@example.com", "Password123!", " philxin ");

        assertEquals("jwt-token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(604800L, response.getExpiresIn());
        assertEquals("user@example.com", response.getUser().getEmail());
        assertEquals("philxin", response.getUser().getDisplayName());
        verify(appUserRepository).save(any(AppUser.class));
        verify(registrationInvitationService).markAsUsed(invitation, savedUser);
        verify(loginSessionService).cacheSession("jwt-token", 1L);
    }

    @Test
    void registerDuplicateEmailThrows409() {
        when(appUserRepository.existsByEmail("user@example.com")).thenReturn(true);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.register("invite-code", "user@example.com", "Password123!", "philxin")
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
    void loginCreatesSlidingSessionAndReturnsOneWeekExpiry() {
        AppUser user = buildUser();
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashed-password")).thenReturn(true);
        when(jwtTokenService.generateToken(user)).thenReturn("jwt-token");
        when(loginSessionService.getExpiresInSeconds()).thenReturn(604800L);

        AuthResponse response = authService.login("user@example.com", "Password123!");

        assertEquals("jwt-token", response.getToken());
        assertEquals(604800L, response.getExpiresIn());
        verify(loginSessionService).cacheSession("jwt-token", 1L);
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

    private RegistrationInvitation buildInvitation() {
        RegistrationInvitation invitation = new RegistrationInvitation();
        invitation.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        invitation.setInviteeEmail("user@example.com");
        return invitation;
    }
}
