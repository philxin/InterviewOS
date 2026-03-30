package com.philxin.interviewos.repository;

import com.philxin.interviewos.entity.RetrievalTrace;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetrievalTraceRepository extends JpaRepository<RetrievalTrace, Long> {

    Optional<RetrievalTrace> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}
