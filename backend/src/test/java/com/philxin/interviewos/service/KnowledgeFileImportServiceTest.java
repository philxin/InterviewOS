package com.philxin.interviewos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.knowledge.KnowledgeFileImportResponse;
import com.philxin.interviewos.controller.dto.knowledge.KnowledgeFileImportStartResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.KnowledgeFileImport;
import com.philxin.interviewos.entity.KnowledgeFileImportStatus;
import com.philxin.interviewos.repository.AppUserRepository;
import com.philxin.interviewos.repository.KnowledgeFileImportRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class KnowledgeFileImportServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private KnowledgeFileImportRepository knowledgeFileImportRepository;

    @Mock
    private KnowledgeFileImportProcessor knowledgeFileImportProcessor;

    private KnowledgeFileImportParser parser;
    private KnowledgeFileImportService knowledgeFileImportService;

    @BeforeEach
    void setUp() {
        parser = new KnowledgeFileImportParser();
        knowledgeFileImportService = new KnowledgeFileImportService(
            appUserRepository,
            knowledgeFileImportRepository,
            knowledgeFileImportProcessor,
            parser,
            new ObjectMapper()
        );
    }

    @Test
    void createFileImportCreatesPendingTaskAndNormalizesDefaultTags() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L)));
        when(knowledgeFileImportRepository.save(any(KnowledgeFileImport.class))).thenAnswer(invocation -> {
            KnowledgeFileImport fileImport = invocation.getArgument(0);
            if (fileImport.getId() == null) {
                fileImport.setId(UUID.randomUUID());
            }
            if (fileImport.getStatus() == null) {
                fileImport.setStatus(KnowledgeFileImportStatus.PENDING);
            }
            return fileImport;
        });
        doNothing().when(knowledgeFileImportProcessor).processImport(any(UUID.class), any(byte[].class), anyList());

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "Redis Notes.md",
            "text/markdown",
            "# Redis".getBytes(StandardCharsets.UTF_8)
        );

        KnowledgeFileImportStartResponse response = knowledgeFileImportService.createFileImport(
            authenticatedUser(1L),
            file,
            " Redis , cache ,redis "
        );

        assertEquals("PENDING", response.getStatus());

        ArgumentCaptor<KnowledgeFileImport> captor = ArgumentCaptor.forClass(KnowledgeFileImport.class);
        verify(knowledgeFileImportRepository).save(captor.capture());
        KnowledgeFileImport saved = captor.getValue();
        assertEquals("Redis Notes.md", saved.getFileName());
        assertEquals("text/markdown", saved.getContentType());
        assertEquals(KnowledgeFileImportStatus.PENDING, saved.getStatus());
        assertEquals("[\"redis\",\"cache\"]", saved.getDefaultTags());
        verify(knowledgeFileImportProcessor).processImport(any(UUID.class), any(byte[].class), anyList());
    }

    @Test
    void createFileImportDefersAsyncProcessingUntilAfterCommitWhenTransactionActive() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L)));
        when(knowledgeFileImportRepository.save(any(KnowledgeFileImport.class))).thenAnswer(invocation -> {
            KnowledgeFileImport fileImport = invocation.getArgument(0);
            if (fileImport.getId() == null) {
                fileImport.setId(UUID.randomUUID());
            }
            if (fileImport.getStatus() == null) {
                fileImport.setStatus(KnowledgeFileImportStatus.PENDING);
            }
            return fileImport;
        });
        doNothing().when(knowledgeFileImportProcessor).processImport(any(UUID.class), any(byte[].class), anyList());

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "Java Notes.md",
            "text/markdown",
            "# Java".getBytes(StandardCharsets.UTF_8)
        );

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            knowledgeFileImportService.createFileImport(authenticatedUser(1L), file, "java");
            verify(knowledgeFileImportProcessor, never()).processImport(any(UUID.class), any(byte[].class), anyList());

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, synchronizations.size());
            synchronizations.forEach(TransactionSynchronization::afterCommit);

            verify(knowledgeFileImportProcessor).processImport(any(UUID.class), any(byte[].class), anyList());
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createFileImportRejectsUnsupportedFileType() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L)));
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "doc".getBytes(StandardCharsets.UTF_8)
        );

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> knowledgeFileImportService.createFileImport(authenticatedUser(1L), file, "backend")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Unsupported file type: docx", exception.getMessage());
    }

    @Test
    void createFileImportRejectsOversizedFile() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L)));
        byte[] content = new byte[(int) KnowledgeFileImportParser.MAX_FILE_SIZE_BYTES + 1];
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "too-large.pdf",
            "application/pdf",
            content
        );

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> knowledgeFileImportService.createFileImport(authenticatedUser(1L), file, "backend")
        );

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, exception.getStatus());
        assertEquals("file size must be <= 5MB", exception.getMessage());
    }

    @Test
    void getFileImportReturnsCurrentUserTaskStatus() {
        UUID importId = UUID.randomUUID();
        KnowledgeFileImport fileImport = new KnowledgeFileImport();
        fileImport.setId(importId);
        fileImport.setUser(buildUser(1L));
        fileImport.setFileName("notes.txt");
        fileImport.setContentType("text/plain");
        fileImport.setFileSize(1024L);
        fileImport.setStatus(KnowledgeFileImportStatus.SUCCESS);
        fileImport.setDefaultTags("[\"redis\",\"cache\"]");
        fileImport.setCreatedCount(1);
        fileImport.setFailureReason(null);
        fileImport.setCreatedAt(LocalDateTime.of(2026, 3, 18, 10, 0, 0));
        fileImport.setUpdatedAt(LocalDateTime.of(2026, 3, 18, 10, 0, 5));
        fileImport.setCompletedAt(LocalDateTime.of(2026, 3, 18, 10, 0, 5));
        when(knowledgeFileImportRepository.findByIdAndUserId(importId, 1L)).thenReturn(Optional.of(fileImport));

        KnowledgeFileImportResponse response = knowledgeFileImportService.getFileImport(authenticatedUser(1L), importId);

        assertEquals(importId, response.getImportId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(List.of("redis", "cache"), response.getDefaultTags());
        assertEquals(1, response.getCreatedCount());
    }

    private AuthenticatedUser authenticatedUser(Long id) {
        return AuthenticatedUser.fromEntity(buildUser(id));
    }

    private AppUser buildUser(Long id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail("user@example.com");
        user.setDisplayName("philxin");
        user.setPasswordHash("hash");
        return user;
    }
}
