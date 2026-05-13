package com.tcc.pjb.backend.service.casefile;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleDecision;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualCatalogService;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualCategoria;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualDescriptor;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualSecurityPolicyService;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualSecurityProfile;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityConsistencyResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityObservabilityResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessLevel;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingRole;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaseContinuityReadinessServiceTest {

    @Test
    void shouldBuildCriticalReadinessWhenTrackDivergesAndSensitiveActionsBlocked() {
        CaseContinuityObservabilityService observabilityService = mock(CaseContinuityObservabilityService.class);
        CaseContinuityConsistencyService consistencyService = mock(CaseContinuityConsistencyService.class);
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        ProcessoLifecycleMachine lifecycleMachine = mock(ProcessoLifecycleMachine.class);
        AtoProcessualCatalogService catalogService = mock(AtoProcessualCatalogService.class);
        AtoProcessualSecurityPolicyService securityPolicyService = new AtoProcessualSecurityPolicyService(catalogService);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        CaseContinuityObservabilityMetrics metrics = mock(CaseContinuityObservabilityMetrics.class);
        CaseContinuityReadinessService service = new CaseContinuityReadinessService(
                observabilityService,
                consistencyService,
                processoRepository,
                lifecycleMachine,
                securityPolicyService,
                auditLedgerService,
                metrics
        );

        when(observabilityService.snapshot(200L)).thenReturn(new CaseContinuityObservabilityResponse(
                Instant.now(), 12L, 200L, 200L, "ROOT", CaseContinuityTrack.CONHECIMENTO, 2L, 1L, 3L,
                0L, 0L, 0L, 0L, 1L, 1L, Instant.now(),
                java.util.Map.of("CONHECIMENTO", 2L), java.util.Map.of("ROOT", 1L), java.util.Map.of("EM_ANDAMENTO", 1L),
                List.of("CASE_CONTINUITY_SYNCED"), List.of("atenção operacional"), true, true
        ));
        when(consistencyService.snapshot(200L)).thenReturn(new CaseContinuityConsistencyResponse(
                Instant.now(), 12L, 200L, CaseContinuityTrack.CONHECIMENTO, true, 2L, 1L, 0L, 0L, 0L, 0L, 0L, 0L,
                List.of(), List.of(), List.of("revisar track")
        ));

        Processo processo = new Processo();
        processo.setId(200L);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setStatusProcesso(StatusProcesso.RECURSO_INTERPOSTO);
        when(processoRepository.findById(200L)).thenReturn(Optional.of(processo));

        AtoProcessualDescriptor recursalDescriptor = new AtoProcessualDescriptor(
                ProcessoLifecycleAction.PROFERIR_VOTO.name(),
                "Voto",
                AtoProcessualCategoria.DECISORIO,
                WorkItemType.DECISAO,
                "GAB",
                "GAB",
                "fundamento",
                AtoProcessualSecurityProfile.reinforced()
        );
        AtoProcessualDescriptor standardDescriptor = new AtoProcessualDescriptor(
                ProcessoLifecycleAction.REALIZAR_JUNTADA.name(),
                "Juntada",
                AtoProcessualCategoria.ADMINISTRATIVO,
                WorkItemType.JUNTADA,
                "SEC",
                "SEC",
                "fundamento",
                AtoProcessualSecurityProfile.standard()
        );
        when(catalogService.descriptorFor(ProcessoLifecycleAction.PROFERIR_VOTO)).thenReturn(recursalDescriptor);
        when(catalogService.descriptorFor(ProcessoLifecycleAction.REALIZAR_JUNTADA)).thenReturn(standardDescriptor);
        when(catalogService.descriptorFor(any(ProcessoLifecycleAction.class))).thenReturn(standardDescriptor);
        when(catalogService.descriptorFor(any(String.class))).thenReturn(standardDescriptor);
        when(catalogService.canonicalActType(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(lifecycleMachine.preview(eq(processo), any(ProcessoLifecycleAction.class))).thenAnswer(invocation -> {
            ProcessoLifecycleAction action = invocation.getArgument(1);
            boolean permitted = action == ProcessoLifecycleAction.REALIZAR_JUNTADA;
            return new ProcessoLifecycleDecision(action, processo.getFaseAtual(), processo.getStatusProcesso(), processo.getFaseAtual(), processo.getStatusProcesso(), permitted, permitted ? "OK" : "NEGADO", "GAB", standardDescriptor, List.of());
        });

        var response = service.snapshot(200L);

        assertThat(response.readinessLevel()).isEqualTo(CaseContinuityReadinessLevel.CRITICA);
        assertThat(response.expectedTrack()).isEqualTo(CaseContinuityTrack.RECURSAL);
        assertThat(response.dominantTrack()).isEqualTo(CaseContinuityTrack.CONHECIMENTO);
        assertThat(response.totalAllowedActions()).isGreaterThan(0);
        assertThat(response.totalSensitiveBlockedActions()).isGreaterThan(0);
        assertThat(response.blockers()).isNotEmpty();
        verify(metrics).recordReadiness(response);
        verify(auditLedgerService).appendSafely(eq("CASE_CONTINUITY_READINESS_INSPECT"), eq("CASE_FILE"), eq("12"), any(String.class));
    }
}
