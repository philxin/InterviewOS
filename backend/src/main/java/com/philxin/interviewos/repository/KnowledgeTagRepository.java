package com.philxin.interviewos.repository;

import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.entity.KnowledgeTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeTagRepository extends JpaRepository<KnowledgeTag, Long> {

    /**
     * 查询当前用户的标签列表，默认仅返回未归档知识点上的标签。
     */
    @Query(
        """
        select distinct kt.tag
        from KnowledgeTag kt
        join kt.knowledge k
        where k.user.id = :userId
          and k.status = :status
        order by kt.tag asc
        """
    )
    List<String> findDistinctTagsByUserIdAndKnowledgeStatus(
        @Param("userId") Long userId,
        @Param("status") KnowledgeStatus status
    );
}
