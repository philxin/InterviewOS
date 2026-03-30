package com.philxin.interviewos.repository;

import com.philxin.interviewos.entity.TrainingQuestionReference;
import com.philxin.interviewos.entity.TrainingQuestionReferenceUsageType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingQuestionReferenceRepository extends JpaRepository<TrainingQuestionReference, Long> {

    List<TrainingQuestionReference> findByQuestionIdAndUsageTypeOrderByRankNoAsc(
        UUID questionId,
        TrainingQuestionReferenceUsageType usageType
    );

    List<TrainingQuestionReference> findByQuestionIdOrderByUsageTypeAscRankNoAsc(UUID questionId);
}
