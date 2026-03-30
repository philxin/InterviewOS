package com.philxin.interviewos.repository;

import com.philxin.interviewos.entity.KnowledgeConcept;
import com.philxin.interviewos.entity.KnowledgeConceptStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeConceptRepository extends JpaRepository<KnowledgeConcept, Long> {

    List<KnowledgeConcept> findByUserIdAndDocumentIdOrderByConfidenceDescCreatedAtDesc(Long userId, UUID documentId);

    Optional<KnowledgeConcept> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndDocumentIdAndStatus(Long userId, UUID documentId, KnowledgeConceptStatus status);
}
