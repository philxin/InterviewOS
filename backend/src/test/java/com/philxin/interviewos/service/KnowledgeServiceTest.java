package com.philxin.interviewos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.repository.KnowledgeRepository;
import com.philxin.interviewos.repository.TrainingRecordRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private KnowledgeRepository knowledgeRepository;

    @Mock
    private TrainingRecordRepository trainingRecordRepository;

    private KnowledgeService knowledgeService;

    @BeforeEach
    void setUp() {
        knowledgeService = new KnowledgeService(knowledgeRepository, trainingRecordRepository);
    }

    @Test
    void deleteKnowledgeRemovesTrainingRecordsBeforeDeletingKnowledge() {
        Knowledge knowledge = new Knowledge();
        knowledge.setId(1L);
        when(knowledgeRepository.findById(1L)).thenReturn(Optional.of(knowledge));

        knowledgeService.deleteKnowledge(1L);

        InOrder inOrder = inOrder(trainingRecordRepository, knowledgeRepository);
        inOrder.verify(trainingRecordRepository).deleteByKnowledgeId(1L);
        inOrder.verify(knowledgeRepository).delete(knowledge);
    }

    @Test
    void deleteKnowledgeThrows404WhenKnowledgeNotFound() {
        when(knowledgeRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> knowledgeService.deleteKnowledge(99L));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(trainingRecordRepository, org.mockito.Mockito.never()).deleteByKnowledgeId(99L);
    }
}
