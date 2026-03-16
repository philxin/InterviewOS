package com.philxin.interviewos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeSourceType;
import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.entity.KnowledgeTag;
import com.philxin.interviewos.repository.AppUserRepository;
import com.philxin.interviewos.repository.KnowledgeRepository;
import com.philxin.interviewos.repository.KnowledgeTagRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private KnowledgeRepository knowledgeRepository;

    @Mock
    private KnowledgeTagRepository knowledgeTagRepository;

    private KnowledgeService knowledgeService;

    @BeforeEach
    void setUp() {
        knowledgeService = new KnowledgeService(appUserRepository, knowledgeRepository, knowledgeTagRepository);
    }

    @Test
    void createKnowledgeAssignsOwnerAndNormalizesTags() {
        AppUser user = buildUser(1L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(knowledgeRepository.save(any(Knowledge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Knowledge knowledge = knowledgeService.createKnowledge(
            authenticatedUser(1L),
            " Spring Boot ",
            " auto config ",
            List.of(" Spring ", "backend", "spring", "", "  ")
        );

        assertEquals("Spring Boot", knowledge.getTitle());
        assertEquals("auto config", knowledge.getContent());
        assertEquals(0, knowledge.getMastery());
        assertEquals(KnowledgeSourceType.MANUAL, knowledge.getSourceType());
        assertEquals(KnowledgeStatus.ACTIVE, knowledge.getStatus());
        assertEquals(2, knowledge.getTags().size());
        assertEquals(List.of("spring", "backend"), knowledge.getTags().stream().map(KnowledgeTag::getTag).toList());
        assertEquals(user, knowledge.getUser());
    }

    @Test
    void getKnowledgeListOnlyReturnsActiveKnowledgeForCurrentUser() {
        when(knowledgeRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, KnowledgeStatus.ACTIVE))
            .thenReturn(List.of(buildKnowledge(10L, KnowledgeStatus.ACTIVE)));

        List<Knowledge> result = knowledgeService.getKnowledgeList(authenticatedUser(1L));

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    void getKnowledgeByIdThrows404WhenKnowledgeIsNotOwnedByCurrentUser() {
        when(knowledgeRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> knowledgeService.getKnowledgeById(authenticatedUser(1L), 99L)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void updateKnowledgeReplacesTagsWithNormalizedValues() {
        Knowledge knowledge = buildKnowledge(1L, KnowledgeStatus.ACTIVE);
        knowledge.replaceTags(List.of("legacy"));
        when(knowledgeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(knowledge));
        when(knowledgeRepository.save(any(Knowledge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Knowledge updated = knowledgeService.updateKnowledge(
            authenticatedUser(1L),
            1L,
            " Updated ",
            " Content ",
            List.of(" Redis ", "redis", " Cache ")
        );

        assertEquals("Updated", updated.getTitle());
        assertEquals("Content", updated.getContent());
        assertEquals(List.of("redis", "cache"), updated.getTags().stream().map(KnowledgeTag::getTag).toList());
    }

    @Test
    void deleteKnowledgeArchivesInsteadOfPhysicalDeletion() {
        Knowledge knowledge = buildKnowledge(1L, KnowledgeStatus.ACTIVE);
        when(knowledgeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(knowledge));

        knowledgeService.deleteKnowledge(authenticatedUser(1L), 1L);

        assertEquals(KnowledgeStatus.ARCHIVED, knowledge.getStatus());
        assertNotNull(knowledge.getArchivedAt());
        verify(knowledgeRepository).save(knowledge);
        verify(knowledgeRepository, never()).delete(any(Knowledge.class));
    }

    @Test
    void deleteKnowledgeIsIdempotentWhenAlreadyArchived() {
        Knowledge knowledge = buildKnowledge(1L, KnowledgeStatus.ARCHIVED);
        when(knowledgeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(knowledge));

        knowledgeService.deleteKnowledge(authenticatedUser(1L), 1L);

        verify(knowledgeRepository, never()).save(any(Knowledge.class));
    }

    @Test
    void getKnowledgeTagsReturnsDistinctTagsForCurrentUser() {
        when(knowledgeTagRepository.findDistinctTagsByUserIdAndKnowledgeStatus(1L, KnowledgeStatus.ACTIVE))
            .thenReturn(List.of("backend", "spring"));

        List<String> result = knowledgeService.getKnowledgeTags(authenticatedUser(1L));

        assertEquals(List.of("backend", "spring"), result);
    }

    @Test
    void getKnowledgeListThrows401WhenUserMissing() {
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> knowledgeService.getKnowledgeList(null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertTrue(exception.getMessage().contains("Unauthorized"));
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

    private Knowledge buildKnowledge(Long id, KnowledgeStatus status) {
        Knowledge knowledge = new Knowledge();
        knowledge.setId(id);
        knowledge.setUser(buildUser(1L));
        knowledge.setTitle("title");
        knowledge.setContent("content");
        knowledge.setMastery(0);
        knowledge.setSourceType(KnowledgeSourceType.MANUAL);
        knowledge.setStatus(status);
        return knowledge;
    }
}
