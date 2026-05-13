package com.tcc.pjb.backend.service.institutional.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchActionPreviewResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchActionResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchExplainabilityResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchOperationalQueueResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchProfileResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchQuickActionsResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchWorkspaceResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InstitutionalWorkbenchServiceTest {

    private CurrentUserService currentUserService;
    private InstitutionalWorkbenchProfileResolver profileResolver;
    private InstitutionalWorkbenchWidgetPolicyEngine widgetPolicyEngine;
    private InstitutionalWorkbenchProjectionService projectionService;
    private ProcessoRepository processoRepository;
    private InstitutionalWorkbenchService service;

    @BeforeEach
    void setUp() {
        currentUserService = Mockito.mock(CurrentUserService.class);
        profileResolver = Mockito.mock(InstitutionalWorkbenchProfileResolver.class);
        widgetPolicyEngine = Mockito.mock(InstitutionalWorkbenchWidgetPolicyEngine.class);
        projectionService = Mockito.mock(InstitutionalWorkbenchProjectionService.class);
        processoRepository = Mockito.mock(ProcessoRepository.class);
        service = new InstitutionalWorkbenchService(currentUserService, profileResolver, widgetPolicyEngine, projectionService, processoRepository);
    }

    @Test
    void shouldProjectWorkspaceWithoutDuplicatingSources() {
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.PROCURADORIA_FEDERAL);
        when(currentUserService.getRequired()).thenReturn(usuario);

        InstitutionalWorkbenchProfileResponse profile = new InstitutionalWorkbenchProfileResponse(
                "PROCURADORIA_FEDERAL",
                "AGU_PGF",
                "FEDERAL",
                "Workbench da advocacia pública federal",
                "Defesa judicial e consultiva federal",
                List.of("FEDERAL"),
                List.of("DF"),
                List.of(),
                List.of("CONTESTACAO")
        );
        when(profileResolver.resolve(usuario)).thenReturn(profile);

        InstitutionalWorkbenchQuickActionsResponse quickActions = new InstitutionalWorkbenchQuickActionsResponse(
                Instant.now(),
                "PROCURADORIA_FEDERAL",
                null,
                null,
                List.of(new InstitutionalWorkbenchActionResponse("PROCURADORIA_CONTESTACAO", "Apresentar contestação", "/api/v1/procuradoria/operacional/processos/{processoId}/contestacao", "POST", true, "ALLOW", "SUCCESS", null, List.of(), List.of(), Map.of())),
                List.of()
        );
        InstitutionalWorkbenchOperationalQueueResponse queue = new InstitutionalWorkbenchOperationalQueueResponse(
                Instant.now(),
                "PROCURADORIA_FEDERAL",
                12,
                1,
                1,
                0,
                List.of(),
                List.of()
        );
        when(projectionService.quickActions(null)).thenReturn(quickActions);
        when(projectionService.operationalQueue(12)).thenReturn(queue);
        when(widgetPolicyEngine.project(profile, quickActions, queue)).thenReturn(new InstitutionalWorkbenchWidgetPolicyEngine.Projection(List.of(), List.of(), List.of()));

        InstitutionalWorkbenchWorkspaceResponse response = service.workspace();

        assertEquals("PROCURADORIA_FEDERAL", response.profile().actorClass());
        assertNotNull(response.quickActions());
        assertNotNull(response.operationalQueue());
        assertFalse(response.warnings().isEmpty());
    }

    @Test
    void shouldBuildActionPreviewForProcessBoundAction() {
        Processo processo = new Processo();
        processo.setId(18L);
        processo.setNumeroProcesso("0011223-44.2026.4.01.3400");
        when(processoRepository.findById(18L)).thenReturn(Optional.of(processo));
        when(projectionService.previewAction(processo, "PROCURADORIA_CONTESTACAO")).thenReturn(
                new InstitutionalWorkbenchActionResponse("PROCURADORIA_CONTESTACAO", "Apresentar contestação", "/api/v1/procuradoria/operacional/processos/18/contestacao", "POST", true, "ALLOW", "SUCCESS", null, List.of("Fluxo federal compatível"), List.of(), Map.of("federalSignal", true))
        );
        when(projectionService.previewExplainability(processo, "PROCURADORIA_CONTESTACAO")).thenReturn(
                new InstitutionalWorkbenchExplainabilityResponse("PROCURADORIA_FEDERAL", "FEDERAL", "ALLOW", List.of("Fluxo federal compatível"), List.of(), Map.of("federalSignal", true))
        );
        when(projectionService.currentActorClass()).thenReturn("PROCURADORIA_FEDERAL");

        InstitutionalWorkbenchActionPreviewResponse response = service.actionPreview(18L, "PROCURADORIA_CONTESTACAO");

        assertEquals(18L, response.processoId());
        assertEquals("ALLOW", response.action().verdict());
        assertEquals("FEDERAL", response.explainability().targetSphere());
    }
}
