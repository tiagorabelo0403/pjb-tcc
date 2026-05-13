package com.tcc.pjb.backend.service.casefile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalRelationType;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingRole;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CaseContinuityConsistencyServiceTest {

    @Test
    void shouldFlagOrphanAndTrackMismatch() {
        CaseContinuityOrchestratorService orchestratorService = mock(CaseContinuityOrchestratorService.class);
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        CaseContinuityObservabilityMetrics metrics = mock(CaseContinuityObservabilityMetrics.class);
        CaseContinuityConsistencyService service = new CaseContinuityConsistencyService(
                orchestratorService,
                processoRepository,
                auditLedgerService,
                metrics
        );

        when(orchestratorService.inspect(100L)).thenReturn(new CaseContinuitySnapshot(
                10L,
                100L,
                100L,
                "ROOT",
                CaseContinuityTrack.RECURSAL,
                List.of(
                        new CaseContinuityProceedingNode("ROOT", 100L, false, CaseProceedingStatus.ACTIVE, CaseContinuityTrack.CONHECIMENTO, CaseProceedingRole.ROOT, InstanceLevel.FIRST_INSTANCE, "TJCE", "1", null, FaseProcessual.CONHECIMENTO, StatusProcesso.EM_ANDAMENTO, NivelSigilo.PUBLICO, Instant.now()),
                        new CaseContinuityProceedingNode("BROKEN-REC", 100L, false, CaseProceedingStatus.ACTIVE, CaseContinuityTrack.RECURSAL, CaseProceedingRole.CUMPRIMENTO, InstanceLevel.SECOND_INSTANCE, "TJCE", "1", "MISSING", FaseProcessual.RECURSAL, StatusProcesso.RECURSO_INTERPOSTO, NivelSigilo.PUBLICO, Instant.now().minusSeconds(60 * 60 * 72))
                ),
                List.of(new CaseContinuityEdgeLink("ROOT", "ROOT", RecursalRelationType.PROCESSUAL_LINKAGE, LegalAppealType.OUTRO)),
                List.of()
        ));

        Processo processo = new Processo();
        processo.setId(100L);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setStatusProcesso(StatusProcesso.RECURSO_INTERPOSTO);
        when(processoRepository.findById(100L)).thenReturn(Optional.of(processo));

        var response = service.snapshot(100L);

        assertThat(response.healthy()).isFalse();
        assertThat(response.orphanParentCount()).isEqualTo(1L);
        assertThat(response.incompatibleRoleTrackCount()).isEqualTo(1L);
        assertThat(response.recursalBranchesWithoutEdge()).isEqualTo(1L);
        assertThat(response.staleProceedings()).isEqualTo(1L);
        assertThat(response.inconsistencies()).isNotEmpty();
        verify(metrics).recordConsistency(response);
    }
}
