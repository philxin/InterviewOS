package com.philxin.interviewos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.knowledge.BatchImportKnowledgeItemRequest;
import com.philxin.interviewos.controller.dto.knowledge.BatchImportKnowledgeResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeSourceType;
import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.entity.KnowledgeTag;
import com.philxin.interviewos.repository.AppUserRepository;
import com.philxin.interviewos.repository.KnowledgeRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class KnowledgeImportServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private KnowledgeRepository knowledgeRepository;

    private KnowledgeImportService knowledgeImportService;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        knowledgeImportService = new KnowledgeImportService(appUserRepository, knowledgeRepository, validator);
    }

    @Test
    void batchImportCreatesKnowledgeAndNormalizesTags() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L)));
        when(knowledgeRepository.save(any(Knowledge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BatchImportKnowledgeResponse response = knowledgeImportService.batchImportKnowledge(
            authenticatedUser(1L),
            List.of(buildItem(" Spring ", " content ", List.of(" Java ", "backend", "java")))
        );

        assertEquals(1, response.getCreatedCount());
        assertEquals(0, response.getFailedCount());

        ArgumentCaptor<Knowledge> captor = ArgumentCaptor.forClass(Knowledge.class);
        org.mockito.Mockito.verify(knowledgeRepository).save(captor.capture());
        Knowledge saved = captor.getValue();
        assertEquals("Spring", saved.getTitle());
        assertEquals("content", saved.getContent());
        assertEquals(KnowledgeSourceType.BATCH_IMPORT, saved.getSourceType());
        assertEquals(KnowledgeStatus.ACTIVE, saved.getStatus());
        assertEquals(List.of("java", "backend"), saved.getTags().stream().map(KnowledgeTag::getTag).toList());
    }

    @Test
    void batchImportCollectsPerItemFailuresWithoutBlockingSuccessItems() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L)));
        when(knowledgeRepository.save(any(Knowledge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BatchImportKnowledgeResponse response = knowledgeImportService.batchImportKnowledge(
            authenticatedUser(1L),
            List.of(
                buildItem("Valid", "content", List.of("spring")),
                buildItem("Invalid", "", List.of("redis"))
            )
        );

        assertEquals(1, response.getCreatedCount());
        assertEquals(1, response.getFailedCount());
        assertEquals(1, response.getFailedItems().get(0).getIndex());
        assertEquals("Invalid", response.getFailedItems().get(0).getTitle());
        assertEquals("content: content must not be blank", response.getFailedItems().get(0).getReason());
    }

    @Test
    void batchImportThrows401WhenUserMissing() {
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> knowledgeImportService.batchImportKnowledge(null, List.of())
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    private AuthenticatedUser authenticatedUser(Long userId) {
        return AuthenticatedUser.fromEntity(buildUser(userId));
    }

    private AppUser buildUser(Long id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail("user@example.com");
        user.setDisplayName("philxin");
        user.setPasswordHash("hash");
        return user;
    }

    private BatchImportKnowledgeItemRequest buildItem(String title, String content, List<String> tags) {
        BatchImportKnowledgeItemRequest item = new BatchImportKnowledgeItemRequest();
        item.setTitle(title);
        item.setContent(content);
        item.setTags(tags);
        return item;
    }
}
