package com.philxin.interviewos.repository;

import com.philxin.interviewos.entity.TrainingSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, UUID> {

    @EntityGraph(attributePaths = "knowledge")
    Optional<TrainingSession> findByIdAndUserId(UUID id, Long userId);

    @EntityGraph(attributePaths = "knowledge")
    List<TrainingSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "knowledge")
    List<TrainingSession> findByUserIdAndKnowledgeIdOrderByCreatedAtDesc(Long userId, Long knowledgeId);
}
