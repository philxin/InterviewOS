package com.philxin.interviewos.service;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.repository.KnowledgeRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeService {
    private final KnowledgeRepository knowledgeRepository;

    public KnowledgeService(KnowledgeRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
    }

    /**
     * 创建知识点，默认 mastery=0。
     */
    @Transactional
    public Knowledge createKnowledge(String title, String content) {
        Knowledge knowledge = new Knowledge();
        knowledge.setTitle(normalize(title));
        knowledge.setContent(normalize(content));
        knowledge.setMastery(0);
        return knowledgeRepository.save(knowledge);
    }

    /**
     * 获取全部知识点（按创建时间倒序）。
     */
    @Transactional(readOnly = true)
    public List<Knowledge> getKnowledgeList() {
        return knowledgeRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 根据主键查询知识点，不存在返回 404。
     */
    @Transactional(readOnly = true)
    public Knowledge getKnowledgeById(Long id) {
        return knowledgeRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    /**
     * 更新知识点基础信息，不修改掌握度。
     */
    @Transactional
    public Knowledge updateKnowledge(Long id, String title, String content) {
        Knowledge knowledge = getKnowledgeById(id);
        knowledge.setTitle(normalize(title));
        knowledge.setContent(normalize(content));
        return knowledgeRepository.save(knowledge);
    }

    /**
     * 删除知识点，不存在返回 404。
     */
    @Transactional
    public void deleteKnowledge(Long id) {
        Knowledge knowledge = getKnowledgeById(id);
        knowledgeRepository.delete(knowledge);
    }

    private BusinessException notFound(Long id) {
        return new BusinessException(HttpStatus.NOT_FOUND, "Knowledge not found with id: " + id);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
