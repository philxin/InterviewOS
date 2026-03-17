package com.philxin.interviewos.repository;

import com.philxin.interviewos.entity.TrainingQuestion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingQuestionRepository extends JpaRepository<TrainingQuestion, UUID> {

    List<TrainingQuestion> findBySessionIdOrderByOrderNoAsc(UUID sessionId);

    Optional<TrainingQuestion> findByIdAndSessionId(UUID id, UUID sessionId);

    List<TrainingQuestion> findBySessionIdIn(List<UUID> sessionIds);
}
