package com.philxin.interviewos.repository;

import com.philxin.interviewos.entity.KnowledgeDocument;
import com.philxin.interviewos.entity.KnowledgeDocumentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {

    List<KnowledgeDocument> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, KnowledgeDocumentStatus status);

    List<KnowledgeDocument> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<KnowledgeDocument> findByIdAndUserId(UUID id, Long userId);
}
