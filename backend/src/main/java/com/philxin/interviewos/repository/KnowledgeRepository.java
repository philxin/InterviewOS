package com.philxin.interviewos.repository;

import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {

    /**
     * 按当前用户和状态查询知识点列表，并预加载标签避免 DTO 转换阶段触发懒加载。
     */
    @EntityGraph(attributePaths = "tags")
    List<Knowledge> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, KnowledgeStatus status);

    /**
     * 查询当前用户的单个知识点详情，并预加载标签。
     */
    @EntityGraph(attributePaths = "tags")
    Optional<Knowledge> findByIdAndUserId(Long id, Long userId);
}
