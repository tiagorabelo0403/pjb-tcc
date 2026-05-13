package com.tcc.pjb.backend.service.institutional.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchOperationalQueueResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchQuickActionsResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.processual.guard.InstitutionalMaterialActionGuardService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InstitutionalWorkbenchProjectionServiceTest {

    private CurrentUserService currentUserService;
    private PainelServiceCommons painelServiceCommons;
    private ProcessoRepository processoRepository;
    private InstitutionalMaterialActionGuardService guardService;
    private InstitutionalWorkbenchProjectionService service;

    @BeforeEach
    void setUp() {
        currentUserService = Mockito.mock(CurrentUserService.class);
        painelServiceCommons = Mockito.mock(PainelServiceCommons.class);
        processoRepository = Mockito.mock(ProcessoRepository.class);
        guardService = Mockito.mock(InstitutionalMaterialActionGuardService.class);
        service = new InstitutionalWorkbenchProjectionService(currentUserService, painelServiceCommons, processoRepository, guardService);
    }

    @Test
    void shouldProjectQuickActionsForFederalDefensoriaWithProcessDecision() {
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.DEFENSOR_PUBLICO_FEDERAL);
        when(currentUserService.getRequired()).thenReturn(usuario);

        Processo processo = new Processo();
        processo.setId(9L);
        processo.setNumeroProcesso("0001234-56.2026.4.05.0001");
        when(processoRepository.findWorkspaceScopedById(9L)).thenReturn(Optional.of(processo));
        when(guardService.analyzeProcessAction(any(), any())).thenReturn(new InstitutionalMaterialActionGuardService.GuardDecision(
                InstitutionalMaterialActionGuardService.ActorBranch.DEFENSORIA_FEDERAL,
                InstitutionalMaterialActionGuardService.MaterialAction.DEFENSORIA_PETICAO,
                InstitutionalMaterialActionGuardService.Verdict.ALLOW,
                InstitutionalMaterialActionGuardService.TargetSphere.FEDERAL,
                List.of("Fluxo federal compatível com a DPU"),
                List.of(),
                Map.of("score", 1)
        ));

        InstitutionalWorkbenchQuickActionsResponse response = service.quickActions(9L);

        assertEquals("DEFENSORIA_FEDERAL", response.actorClass());
        assertEquals(9L, response.processoId());
        assertEquals(3, response.actions().size());
        assertTrue(response.actions().stream().allMatch(action -> action.enabled()));
    }

    @Test
    void shouldProjectOperationalQueueWithExplainability() {
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.PROCURADORIA_FEDERAL);
        when(currentUserService.getRequired()).thenReturn(usuario);

        Processo processo = new Processo();
        processo.setId(12L);
        processo.setNumeroProcesso("0099999-11.2026.4.01.3400");

        WorkItem item = WorkItem.builder()
                .id(77L)
                .processo(processo)
                .titulo("Contestação urgente")
                .queueCode("PROC:FED")
                .status(WorkItemStatus.PENDENTE)
                .prioridade(1)
                .dueAt(Instant.now().plusSeconds(3600))
                .build();

        when(painelServiceCommons.inboxHibrido(usuario, 20)).thenReturn(List.of(item));
        when(guardService.analyzeProcessAction(any(), any())).thenReturn(new InstitutionalMaterialActionGuardService.GuardDecision(
                InstitutionalMaterialActionGuardService.ActorBranch.PROCURADORIA_FEDERAL,
                InstitutionalMaterialActionGuardService.MaterialAction.PROCURADORIA_CONTESTACAO,
                InstitutionalMaterialActionGuardService.Verdict.ALLOW,
                InstitutionalMaterialActionGuardService.TargetSphere.FEDERAL,
                List.of("Ação compatível com a defesa federal"),
                List.of(),
                Map.of("federalSignal", true)
        ));

        InstitutionalWorkbenchOperationalQueueResponse response = service.operationalQueue(20);

        assertEquals(1, response.totalItems());
        assertEquals(1, response.actionableItems());
        assertFalse(response.items().isEmpty());
        assertNotNull(response.items().get(0).primaryAction());
        assertEquals("ALLOW", response.items().get(0).explainability().verdict());
    }
}
