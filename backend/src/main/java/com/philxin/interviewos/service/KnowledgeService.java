package com.philxin.interviewos.service;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeSourceType;
import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.repository.AppUserRepository;
import com.philxin.interviewos.repository.KnowledgeRepository;
import com.philxin.interviewos.repository.KnowledgeTagRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeService {
    private final AppUserRepository appUserRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final KnowledgeTagRepository knowledgeTagRepository;

    public KnowledgeService(
        AppUserRepository appUserRepository,
        KnowledgeRepository knowledgeRepository,
        KnowledgeTagRepository knowledgeTagRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.knowledgeTagRepository = knowledgeTagRepository;
    }

    /**
     * 创建知识点，默认 mastery=0。
     */
    @Transactional
    public Knowledge createKnowledge(AuthenticatedUser authenticatedUser, String title, String content, List<String> tags) {
        AppUser user = getCurrentUserEntity(authenticatedUser);
        Knowledge knowledge = new Knowledge();
        knowledge.setUser(user);
        knowledge.setTitle(normalize(title));
        knowledge.setContent(normalize(content));
        knowledge.setMastery(0);
        knowledge.setSourceType(KnowledgeSourceType.MANUAL);
        knowledge.setStatus(KnowledgeStatus.ACTIVE);
        knowledge.replaceTags(normalizeTags(tags));
        return knowledgeRepository.save(knowledge);
    }

    /**
     * 获取当前用户的未归档知识点列表（按创建时间倒序）。
     */
    @Transactional(readOnly = true)
    public List<Knowledge> getKnowledgeList(AuthenticatedUser authenticatedUser) {
        return knowledgeRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
            getCurrentUserId(authenticatedUser),
            KnowledgeStatus.ACTIVE
        );
    }

    /**
     * 根据主键查询当前用户的知识点，不存在或不属于当前用户都返回 404。
     */
    @Transactional(readOnly = true)
    public Knowledge getKnowledgeById(AuthenticatedUser authenticatedUser, Long id) {
        return knowledgeRepository.findByIdAndUserId(id, getCurrentUserId(authenticatedUser))
            .orElseThrow(() -> notFound(id));
    }

    /**
     * 更新知识点基础信息，不修改掌握度。
     */
    @Transactional
    public Knowledge updateKnowledge(
        AuthenticatedUser authenticatedUser,
        Long id,
        String title,
        String content,
        List<String> tags
    ) {
        Knowledge knowledge = getKnowledgeById(authenticatedUser, id);
        knowledge.setTitle(normalize(title));
        knowledge.setContent(normalize(content));
        knowledge.replaceTags(normalizeTags(tags));
        return knowledgeRepository.save(knowledge);
    }

    /**
     * 删除知识点语义为归档，不做物理删除，确保历史训练记录仍可追溯。
     */
    @Transactional
    public void deleteKnowledge(AuthenticatedUser authenticatedUser, Long id) {
        Knowledge knowledge = getKnowledgeById(authenticatedUser, id);
        if (knowledge.getStatus() == KnowledgeStatus.ARCHIVED) {
            return;
        }
        knowledge.setStatus(KnowledgeStatus.ARCHIVED);
        knowledge.setArchivedAt(LocalDateTime.now());
        knowledgeRepository.save(knowledge);
    }

    /**
     * 查询当前用户可见知识点标签列表。
     */
    @Transactional(readOnly = true)
    public List<String> getKnowledgeTags(AuthenticatedUser authenticatedUser) {
        return knowledgeTagRepository.findDistinctTagsByUserIdAndKnowledgeStatus(
            getCurrentUserId(authenticatedUser),
            KnowledgeStatus.ACTIVE
        );
    }

    private BusinessException notFound(Long id) {
        return new BusinessException(HttpStatus.NOT_FOUND, "Knowledge not found with id: " + id);
    }

    private AppUser getCurrentUserEntity(AuthenticatedUser authenticatedUser) {
        Long userId = getCurrentUserId(authenticatedUser);
        return appUserRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Long getCurrentUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authenticatedUser.getId();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalizedTags = new LinkedHashSet<>();
        for (String tag : tags) {
            String normalizedTag = normalize(tag);
            if (normalizedTag == null || normalizedTag.isBlank()) {
                continue;
            }
            normalizedTags.add(normalizedTag.toLowerCase(Locale.ROOT));
        }
        return List.copyOf(normalizedTags);
    }
}
