package com.tcc.pjb.backend.service.processual.observability.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.comunicacao.judicial.ExpedicaoJudicial;
import com.tcc.pjb.backend.core.comunicacao.judicial.ExpedicaoJudicialRepository;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalIntegrationSystem;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceeding;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingRole;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.repository.CaseFileEventRepository;
import com.tcc.pjb.backend.model.repository.CaseFileRepository;
import com.tcc.pjb.backend.model.repository.CaseProceedingRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.repository.outbox.OutboxEventRepository;
import com.tcc.pjb.backend.service.processual.observability.metrics.ProcessBusinessMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProcessBusinessObservabilityServiceTest {

    @Test
    void shouldExposeCaseContinuityCountersOnSnapshot() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
        ExpedicaoJudicialRepository expedicaoJudicialRepository = mock(ExpedicaoJudicialRepository.class);
        CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
        CaseProceedingRepository caseProceedingRepository = mock(CaseProceedingRepository.class);
        CaseFileEventRepository caseFileEventRepository = mock(CaseFileEventRepository.class);
        ProcessBusinessMetrics metrics = new ProcessBusinessMetrics(new SimpleMeterRegistry());
        ProcessBusinessObservabilityService service = new ProcessBusinessObservabilityService(
                processoRepository,
                workItemRepository,
                outboxEventRepository,
                expedicaoJudicialRepository,
                caseFileRepository,
                caseProceedingRepository,
                caseFileEventRepository,
                metrics
        );

        List<FaseProcessual> recursalPhases = Arrays.stream(FaseProcessual.values())
                .filter(FaseProcessual::isRecursal)
                .toList();

        when(processoRepository.count()).thenReturn(1L);
        when(processoRepository.countByStatusProcesso(eq(StatusProcesso.ARQUIVADO))).thenReturn(0L);
        when(processoRepository.countByStatusProcesso(eq(StatusProcesso.TRANSITO_EM_JULGADO))).thenReturn(0L);
        when(processoRepository.countRecursais(eq(StatusProcesso.RECURSO_INTERPOSTO), eq(recursalPhases))).thenReturn(1L);
        when(processoRepository.countPorRamo()).thenReturn(List.of(
                new Object[]{"CIVIL", 2L},
                new Object[]{"NAO_CLASSIFICADO", 1L},
                new Object[]{"PENAL", 1L}
        ));
        when(processoRepository.countPorStatus()).thenReturn(List.of(
                new Object[]{"ARQUIVADO", 1L},
                new Object[]{"EM_ANDAMENTO", 1L},
                new Object[]{"NAO_CLASSIFICADO", 1L},
                new Object[]{"RECURSO_INTERPOSTO", 1L}
        ));
        when(workItemRepository.countByStatus(WorkItemStatus.PENDENTE)).thenReturn(0L);
        when(workItemRepository.countByStatus(WorkItemStatus.EM_EXECUCAO)).thenReturn(0L);
        when(workItemRepository.countByStatusAndDueAtBefore(eq(WorkItemStatus.PENDENTE), any())).thenReturn(0L);
        when(workItemRepository.countByStatusAndDueAtBefore(eq(WorkItemStatus.EM_EXECUCAO), any())).thenReturn(0L);
        when(outboxEventRepository.peekIds(anyString(), any(), anyInt())).thenReturn(List.of());
        when(expedicaoJudicialRepository.countByStatus(ExpedicaoJudicial.StatusExpedicao.EXPEDIDA)).thenReturn(0L);
        when(expedicaoJudicialRepository.countByStatus(ExpedicaoJudicial.StatusExpedicao.FRUSTRADA_DEFINITIVA)).thenReturn(0L);
        when(expedicaoJudicialRepository.countByEvasaoDetectadaTrue()).thenReturn(0L);
        when(expedicaoJudicialRepository.countByEscalonadoParaJuizTrue()).thenReturn(0L);
        when(caseFileRepository.count()).thenReturn(3L);
        when(caseProceedingRepository.findAll()).thenReturn(List.of(
                proceeding(10L, "ROOT", null, CaseContinuityTrack.CONHECIMENTO, CaseProceedingRole.ROOT, Instant.now()),
                proceeding(10L, "REC", "ROOT", CaseContinuityTrack.RECURSAL, CaseProceedingRole.RECURSAL, Instant.now()),
                proceeding(10L, "EXEC", "ROOT", CaseContinuityTrack.EXECUCAO, CaseProceedingRole.EXECUCAO, Instant.now()),
                proceeding(11L, "ROOT-2", null, CaseContinuityTrack.CONHECIMENTO, CaseProceedingRole.ROOT, Instant.now().minusSeconds(60 * 60 * 72)),
                proceeding(11L, "BROKEN", "MISSING", CaseContinuityTrack.CUMPRIMENTO, CaseProceedingRole.CUMPRIMENTO, Instant.now().minusSeconds(60 * 60 * 72)),
                proceeding(12L, "ROOT-3", null, CaseContinuityTrack.CONHECIMENTO, CaseProceedingRole.ROOT, Instant.now()),
                proceeding(12L, "AUX-3", null, CaseContinuityTrack.CONHECIMENTO, CaseProceedingRole.VINCULADO, Instant.now()),
                proceeding(13L, "ROOT-4", null, CaseContinuityTrack.CONHECIMENTO, CaseProceedingRole.ROOT, Instant.now()),
                proceeding(13L, "EXEC-4", "ROOT-4", CaseContinuityTrack.CUMPRIMENTO, CaseProceedingRole.CUMPRIMENTO, Instant.now())
        ));
        when(caseFileEventRepository.count()).thenReturn(12L);
        when(workItemRepository.findFilasComMaisDe(25L)).thenReturn(List.of());

        var response = service.snapshot();

        assertEquals(1L, response.totalProcessos());
        assertEquals(0L, response.arquivados());
        assertEquals(0L, response.transitoJulgado());
        assertEquals(1L, response.recursais());
        assertEquals(Map.of("CIVIL", 2L, "NAO_CLASSIFICADO", 1L, "PENAL", 1L), response.processosPorRamo());
        assertEquals(
                Map.of("ARQUIVADO", 1L, "EM_ANDAMENTO", 1L, "NAO_CLASSIFICADO", 1L, "RECURSO_INTERPOSTO", 1L),
                response.processosPorStatus());
        assertEquals(3L, response.caseFilesUnificados());
        assertEquals(9L, response.proceedingsMaterializados());
        assertEquals(1L, response.proceedingsRecursais());
        assertEquals(3L, response.proceedingsExecutorios());
        assertEquals(2L, response.staleProceedings());
        assertEquals(12L, response.caseFileEvents());
        assertEquals(2L, response.caseFilesAttentionRequired());
        assertEquals(1L, response.orphanProceedingParents());
        assertEquals(1L, response.divergentRootProceedings());
    }

    private static CaseProceeding proceeding(Long caseFileId,
                                             String key,
                                             String parent,
                                             CaseContinuityTrack track,
                                             CaseProceedingRole role,
                                             Instant lastSyncAt) {
        return CaseProceeding.builder()
                .caseFileId(caseFileId)
                .proceedingKey(key)
                .shadow(false)
                .status(CaseProceedingStatus.ACTIVE)
                .instanceLevel(InstanceLevel.FIRST_INSTANCE)
                .court("TJCE")
                .numeroUnificado(key)
                .linkedProcessoId(caseFileId)
                .continuityTrack(track)
                .proceedingRole(role)
                .parentProceedingKey(parent)
                .sourceFaseProcessual(track != null && track.isExecutory() ? FaseProcessual.EXECUCAO : FaseProcessual.CONHECIMENTO)
                .sourceStatusProcesso(track != null && track.isExecutory() ? StatusProcesso.CUMPRIMENTO_SENTENCA : StatusProcesso.EM_ANDAMENTO)
                .lastSyncAt(lastSyncAt)
                .secrecy(NivelSigilo.PUBLICO)
                .sourceSystem(LegalIntegrationSystem.OTHER)
                .build();
    }
}
