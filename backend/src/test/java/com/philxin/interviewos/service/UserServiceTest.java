package com.philxin.interviewos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.user.UserOnboardingResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.TargetRole;
import com.philxin.interviewos.repository.AppUserRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void updateOnboardingPersistsTargetRole() {
        AppUser user = buildUser();
        AuthenticatedUser authenticatedUser = AuthenticatedUser.fromEntity(user);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(appUserRepository.save(user)).thenReturn(user);

        UserOnboardingResponse response = userService.updateOnboarding(
            authenticatedUser,
            TargetRole.JAVA_BACKEND
        );

        assertEquals(1L, response.getId());
        assertEquals("JAVA_BACKEND", response.getTargetRole());
        assertEquals(TargetRole.JAVA_BACKEND, user.getTargetRole());
        verify(appUserRepository).save(user);
    }

    @Test
    void updateOnboardingWithoutAuthenticatedUserThrows401() {
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userService.updateOnboarding(null, TargetRole.JAVA_BACKEND)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Unauthorized", exception.getMessage());
    }

    @Test
    void updateOnboardingWhenUserMissingThrows404() {
        AppUser user = buildUser();
        AuthenticatedUser authenticatedUser = AuthenticatedUser.fromEntity(user);
        when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userService.updateOnboarding(authenticatedUser, TargetRole.JAVA_BACKEND)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("User not found", exception.getMessage());
    }

    private AppUser buildUser() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setDisplayName("philxin");
        user.setPasswordHash("hash");
        return user;
    }
}
