package com.tcc.pjb.backend.service.casefile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.kernel.casefile.CaseFileEventStore;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.LegalIntegrationSystem;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalRelationType;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.casefile.CaseEdge;
import com.tcc.pjb.backend.model.entity.casefile.CaseFile;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceeding;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingRole;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.CaseEdgeRepository;
import com.tcc.pjb.backend.model.repository.CaseFileRepository;
import com.tcc.pjb.backend.model.repository.CaseProceedingRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CaseContinuityOrchestratorServiceTest {
    @Test
    void deveMontarSnapshotComAlertaQuandoNaoHaRamificacaoEstrutural() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        CaseFileResolverService resolverService = mock(CaseFileResolverService.class);
        CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
        CaseProceedingRepository proceedingRepository = mock(CaseProceedingRepository.class);
        CaseEdgeRepository edgeRepository = mock(CaseEdgeRepository.class);
        CaseFileEventStore eventStore = mock(CaseFileEventStore.class);
        PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
        com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService auditLedgerService = mock(com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService.class);
        CaseContinuityObservabilityMetrics observabilityMetrics = new CaseContinuityObservabilityMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        CaseContinuityOrchestratorService service = new CaseContinuityOrchestratorService(processoRepository, resolverService, caseFileRepository, proceedingRepository, edgeRepository, eventStore, authorizationService, auditLedgerService, observabilityMetrics);

        Processo processo = Processo.builder().id(10L).rito(RitoProcessual.COMUM_ORDINARIO).faseAtual(FaseProcessual.CONHECIMENTO).statusProcesso(StatusProcesso.EM_ANDAMENTO).numeroUnificado("1").build();
        CaseFile caseFile = CaseFile.builder().id(99L).rootProcessoId(10L).build();
        CaseProceeding proceeding = CaseProceeding.builder().caseFileId(99L).proceedingKey("ROOT").linkedProcessoId(10L).status(CaseProceedingStatus.ACTIVE).continuityTrack(CaseContinuityTrack.CONHECIMENTO).proceedingRole(CaseProceedingRole.ROOT).instanceLevel(InstanceLevel.FIRST_INSTANCE).numeroUnificado("1").secrecy(NivelSigilo.PUBLICO).build();

        when(processoRepository.findProcessoCompletoById(10L)).thenReturn(Optional.of(processo));
        when(resolverService.resolveForProcesso(10L, LegalIntegrationSystem.OTHER)).thenReturn(new CaseFileResolution(caseFile, proceeding, null));
        when(proceedingRepository.findAllByCaseFileId(99L)).thenReturn(List.of(proceeding));
        when(edgeRepository.findAllByCaseFileId(99L)).thenReturn(List.of());
        doNothing().when(authorizationService).requireReadProcesso(processo);

        CaseContinuitySnapshot snapshot = service.inspect(10L);

        assertThat(snapshot.proceedingCount()).isEqualTo(1);
        assertThat(snapshot.requiresAttention()).isTrue();
        assertThat(snapshot.warnings()).anyMatch(item -> item.contains("desdobramentos internos"));
    }

    @Test
    void deveMontarSnapshotExecutivoSemAlertaDeContinuidadeAusente() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        CaseFileResolverService resolverService = mock(CaseFileResolverService.class);
        CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
        CaseProceedingRepository proceedingRepository = mock(CaseProceedingRepository.class);
        CaseEdgeRepository edgeRepository = mock(CaseEdgeRepository.class);
        CaseFileEventStore eventStore = mock(CaseFileEventStore.class);
        PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
        com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService auditLedgerService = mock(com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService.class);
        CaseContinuityObservabilityMetrics observabilityMetrics = new CaseContinuityObservabilityMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        CaseContinuityOrchestratorService service = new CaseContinuityOrchestratorService(processoRepository, resolverService, caseFileRepository, proceedingRepository, edgeRepository, eventStore, authorizationService, auditLedgerService, observabilityMetrics);

        Processo processo = Processo.builder().id(10L).rito(RitoProcessual.COMUM_ORDINARIO).faseAtual(FaseProcessual.EXECUCAO).statusProcesso(StatusProcesso.CUMPRIMENTO_SENTENCA).numeroUnificado("1").build();
        CaseFile caseFile = CaseFile.builder().id(99L).rootProcessoId(10L).build();
        CaseProceeding root = CaseProceeding.builder().caseFileId(99L).proceedingKey("ROOT").linkedProcessoId(10L).status(CaseProceedingStatus.ACTIVE).continuityTrack(CaseContinuityTrack.CONHECIMENTO).proceedingRole(CaseProceedingRole.ROOT).instanceLevel(InstanceLevel.FIRST_INSTANCE).numeroUnificado("1").secrecy(NivelSigilo.PUBLICO).build();
        CaseProceeding exec = CaseProceeding.builder().caseFileId(99L).proceedingKey("EXEC").linkedProcessoId(10L).status(CaseProceedingStatus.ACTIVE).continuityTrack(CaseContinuityTrack.EXECUCAO).proceedingRole(CaseProceedingRole.EXECUCAO).instanceLevel(InstanceLevel.FIRST_INSTANCE).numeroUnificado("1").parentProceedingKey("ROOT").secrecy(NivelSigilo.PUBLICO).build();
        CaseEdge edge = CaseEdge.builder().caseFileId(99L).fromProceedingKey("ROOT").toProceedingKey("EXEC").relationType(RecursalRelationType.EXECUTION_CONTINUATION).appealType(LegalAppealType.OUTRO).build();

        when(processoRepository.findProcessoCompletoById(10L)).thenReturn(Optional.of(processo));
        when(resolverService.resolveForProcesso(10L, LegalIntegrationSystem.OTHER)).thenReturn(new CaseFileResolution(caseFile, root, null));
        when(proceedingRepository.findAllByCaseFileId(99L)).thenReturn(List.of(root, exec));
        when(edgeRepository.findAllByCaseFileId(99L)).thenReturn(List.of(edge));
        doNothing().when(authorizationService).requireReadProcesso(processo);

        CaseContinuitySnapshot snapshot = service.inspect(10L);

        assertThat(snapshot.hasExecutoryBranch()).isTrue();
        assertThat(snapshot.warnings()).noneMatch(item -> item.contains("continuidade executiva"));
        assertThat(snapshot.dominantTrack()).isEqualTo(CaseContinuityTrack.EXECUCAO);
    }
}
