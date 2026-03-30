package com.philxin.interviewos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.knowledge.KnowledgeDocumentResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.KnowledgeDocument;
import com.philxin.interviewos.entity.KnowledgeDocumentStatus;
import com.philxin.interviewos.repository.KnowledgeDocumentRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class KnowledgeDocumentServiceTest {

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    private KnowledgeDocumentService knowledgeDocumentService;

    @BeforeEach
    void setUp() {
        knowledgeDocumentService = new KnowledgeDocumentService(knowledgeDocumentRepository);
    }

    @Test
    void listDocumentsShouldQueryByCurrentUser() {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        document.setTitle("java-notes");
        document.setOriginalFileName("java-notes.pdf");
        document.setStatus(KnowledgeDocumentStatus.ACTIVE);
        document.setTotalChunks(12);
        document.setActiveChunks(12);
        when(knowledgeDocumentRepository.findByUserIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(document));

        List<KnowledgeDocumentResponse> responses = knowledgeDocumentService.listDocuments(authenticatedUser(1L));

        assertEquals(1, responses.size());
        assertEquals("java-notes", responses.get(0).getTitle());
        verify(knowledgeDocumentRepository).findByUserIdOrderByUpdatedAtDesc(eq(1L));
    }

    @Test
    void getDocumentShouldReturn404WhenNotOwned() {
        UUID documentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(knowledgeDocumentRepository.findByIdAndUserId(documentId, 1L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> knowledgeDocumentService.getDocument(authenticatedUser(1L), documentId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Knowledge document not found", exception.getMessage());
    }

    private AuthenticatedUser authenticatedUser(Long userId) {
        AppUser user = new AppUser();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setDisplayName("user");
        user.setPasswordHash("hash");
        return AuthenticatedUser.fromEntity(user);
    }
}
