package com.tcc.pjb.backend.service.mp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.criminal.InqueritoPolicialDigitalService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.dashboard.PerfilPainelSupportService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorTopologyMeshService;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.painel.shared.PainelActionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelExecutionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelNativeCollectionCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.shared.PainelSignalReflectionService;
import com.tcc.pjb.backend.service.processual.guard.InstitutionalMaterialActionGuardService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
import com.tcc.pjb.backend.service.processual.recursal.RecursalPeticionamentoFacadeService;
import com.tcc.pjb.backend.service.profile.PerfilRealtimeTopicService;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalPanelBrandingService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class MinisterioPublicoPainelServiceInqueritosTest {

    private final InqueritoPolicialDigitalService inqueritoPolicialDigitalService = mock(InqueritoPolicialDigitalService.class);
    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final PerfilDashboardContextFactory contextFactory = mock(PerfilDashboardContextFactory.class);

    private MinisterioPublicoPainelService service() {
        PainelServiceCommons commons = new PainelServiceCommons(
                workItemRepository,
                mock(com.tcc.pjb.backend.service.calendar.UserCalendarService.class),
                mock(PerfilPainelSupportService.class),
                mock(OutboxPublisher.class),
                mock(PerfilRealtimeTopicService.class));
        return new MinisterioPublicoPainelService(
                contextFactory,
                commons,
                mock(ProcessoRepository.class),
                workItemRepository,
                mock(RecursalPeticionamentoFacadeService.class),
                mock(InstitutionalActorTopologyMeshService.class),
                mock(InstitutionalActorRoutingService.class),
                mock(InstitutionalMultimediaWorkspaceService.class),
                mock(InstitutionalPanelBrandingService.class),
                mock(PainelSharedExperienceService.class),
                mock(PainelSignalReflectionService.class),
                mock(PainelNativeCollectionCompositionService.class),
                mock(PainelActionSurfaceCompositionService.class),
                mock(PainelExecutionSurfaceCompositionService.class),
                mock(InstitutionalMaterialActionGuardService.class),
                inqueritoPolicialDigitalService);
    }

    private Usuario promotor() {
        return Usuario.builder().id(30L).nome("Promotor").tipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO).build();
    }

    private WorkItem workItem(Long id, String titulo, Long processoId) {
        Processo processo = new Processo();
        processo.setId(processoId);
        return WorkItem.builder().id(id).titulo(titulo).processo(processo).status(com.tcc.pjb.backend.model.entity.enums.WorkItemStatus.PENDENTE).build();
    }

    private InqueritoPolicialDigitalService.InqueritoView inqueritoView(Long id, Long processoId) {
        return new InqueritoPolicialDigitalService.InqueritoView(id, "IP-" + id, "INQUERITO", "EM_ANDAMENTO", "INVESTIGACAO",
                "furto", "resumo dos fatos", null, null, null, null, null, null, "DELEGACIA", null, null, null, null,
                "CE", "Fortaleza", NivelSigilo.PUBLICO, null, null, processoId, "PROC-" + processoId,
                Instant.now(), null, LocalDate.now().plusDays(30), Instant.now());
    }

    @Test
    void componeInqueritosDigitaisEDeduplicaContraOPainelOperacional() {
        Usuario usuario = promotor();
        when(contextFactory.build()).thenReturn(new PerfilDashboardContext(usuario, null, null, null, null, null, null, null, null, null, null, null, null));
        when(inqueritoPolicialDigitalService.listarMeus(null)).thenReturn(List.of(inqueritoView(1L, 100L)));
        WorkItem duplicado = workItem(10L, "INQUERITO em andamento", 100L);
        WorkItem exclusivoDoPainel = workItem(11L, "PIC instaurado", 200L);
        when(workItemRepository.inboxByUser(eq(30L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(duplicado, exclusivoDoPainel)));

        List<Map<String, Object>> resultado = service().listarInqueritosEmAcompanhamento();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0)).containsEntry("origem", "INQUERITO_DIGITAL").containsEntry("processoId", 100L);
        assertThat(resultado.get(1)).containsEntry("origem", "PAINEL_OPERACIONAL").containsEntry("id", 11L);
    }

    @Test
    void retornaSomenteInqueritosDigitaisQuandoNaoHaItemDePainel() {
        Usuario usuario = promotor();
        when(contextFactory.build()).thenReturn(new PerfilDashboardContext(usuario, null, null, null, null, null, null, null, null, null, null, null, null));
        when(inqueritoPolicialDigitalService.listarMeus(null)).thenReturn(List.of(inqueritoView(1L, 100L)));
        when(workItemRepository.inboxByUser(eq(30L), any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));
        when(workItemRepository.inboxByRoleAndTerritory(any(), any(), any(), any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        List<Map<String, Object>> resultado = service().listarInqueritosEmAcompanhamento();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst()).containsEntry("origem", "INQUERITO_DIGITAL");
    }
}
