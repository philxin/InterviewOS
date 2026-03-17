package com.philxin.interviewos.service;

import com.philxin.interviewos.entity.FeedbackBand;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeTag;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 统一处理掌握度和反馈档位规则，避免训练逻辑里散落魔法值。
 */
@Service
public class MasteryService {

    public int calculateMastery(Integer oldMastery, int score) {
        int base = normalizeScore(oldMastery == null ? 0 : oldMastery);
        return normalizeScore((int) (base * 0.7 + score * 0.3));
    }

    public FeedbackBand resolveBand(int score) {
        return FeedbackBand.fromScore(normalizeScore(score));
    }

    public List<String> resolveWeakTags(Knowledge knowledge) {
        if (knowledge == null || knowledge.getTags() == null) {
            return List.of();
        }
        return knowledge.getTags().stream().map(KnowledgeTag::getTag).toList();
    }

    public int normalizeScore(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
