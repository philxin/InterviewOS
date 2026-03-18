package com.philxin.interviewos.repository;

import com.philxin.interviewos.entity.KnowledgeFileImport;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeFileImportRepository extends JpaRepository<KnowledgeFileImport, UUID> {

    Optional<KnowledgeFileImport> findByIdAndUserId(UUID id, Long userId);
}
