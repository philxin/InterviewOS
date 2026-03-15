package com.philxin.interviewos.service;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.user.UserOnboardingResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.TargetRole;
import com.philxin.interviewos.repository.AppUserRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 当前用户资料相关业务逻辑。
 */
@Service
public class UserService {
    private final AppUserRepository appUserRepository;

    public UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * 保存当前用户训练方向，仅处理轻量岗位方向选择。
     */
    @Transactional
    public UserOnboardingResponse updateOnboarding(
        AuthenticatedUser authenticatedUser,
        TargetRole targetRole
    ) {
        AppUser user = getCurrentUserEntity(authenticatedUser);
        user.setTargetRole(targetRole);
        AppUser savedUser = appUserRepository.save(user);
        return UserOnboardingResponse.fromEntity(savedUser);
    }

    private AppUser getCurrentUserEntity(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return appUserRepository.findById(authenticatedUser.getId())
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
