package com.philxin.interviewos.repository;

import com.philxin.interviewos.entity.KnowledgeChunk;
import com.philxin.interviewos.entity.KnowledgeChunkStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    List<KnowledgeChunk> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, KnowledgeChunkStatus status);

    List<KnowledgeChunk> findByUserIdAndDocumentIdAndStatusOrderByChunkIndexAsc(
        Long userId,
        UUID documentId,
        KnowledgeChunkStatus status
    );

    List<KnowledgeChunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);
}
