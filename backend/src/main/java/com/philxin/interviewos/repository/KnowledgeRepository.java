package com.philxin.interviewos.repository;

import com.philxin.interviewos.entity.Knowledge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {

    /**
     * 按创建时间倒序查询知识点列表。
     */
    List<Knowledge> findAllByOrderByCreatedAtDesc();
}
