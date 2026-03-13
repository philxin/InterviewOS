package com.philxin.interviewos.repository;

import com.philxin.interviewos.entity.TrainingRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, Long> {

    /**
     * 按知识点和时间倒序查询训练历史。
     */
    List<TrainingRecord> findByKnowledgeIdOrderByCreatedAtDesc(Long knowledgeId);

    /**
     * 查询全部训练历史（按时间倒序）。
     */
    List<TrainingRecord> findAllByOrderByCreatedAtDesc();

    /**
     * 按知识点删除训练记录，确保知识点删除时不会触发外键约束错误。
     */
    void deleteByKnowledgeId(Long knowledgeId);
}
