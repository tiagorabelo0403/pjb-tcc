package com.tcc.pjb.backend.service.mp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorTopologyMeshService;
import com.tcc.pjb.backend.service.processual.guard.InstitutionalMaterialActionGuardService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MinisterioPublicoInstitutionalRoutingServiceTest {

    private final PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
    private final InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService = mock(InstitutionalActorTopologyMeshService.class);
    private final InstitutionalActorRoutingService institutionalActorRoutingService = mock(InstitutionalActorRoutingService.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final PainelServiceCommons commons = mock(PainelServiceCommons.class);
    private final PerfilDashboardContextFactory contextFactory = mock(PerfilDashboardContextFactory.class);
    private final InstitutionalMaterialActionGuardService institutionalMaterialActionGuardService = mock(InstitutionalMaterialActionGuardService.class);

    private final MinisterioPublicoInstitutionalRoutingService service = new MinisterioPublicoInstitutionalRoutingService(
            authorizationService,
            institutionalActorTopologyMeshService,
            institutionalActorRoutingService,
            processoRepository,
            workItemRepository,
            commons,
            contextFactory,
            institutionalMaterialActionGuardService
    );

    @Test
    void malhaProcessoExigeVinculoInstitucionalAntesDeConsultarAMalha() {
        var esperado = new InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot(
                77L, "0001", "AXIS", "SCOPE", "OFFICE", "INBOX1", "INBOX2", "INBOX3", "CANAL",
                java.util.List.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of());
        when(institutionalActorTopologyMeshService.snapshot(77L)).thenReturn(esperado);

        var resultado = service.malhaProcesso(77L);

        assertThat(resultado).isSameAs(esperado);
        verify(authorizationService).requireVinculoInstitucionalComProcesso(77L);
    }

    @Test
    void requisitarDiligenciaExigeVinculoERoteiaParaDelegacia() {
        Processo processo = new Processo();
        processo.setId(88L);
        when(processoRepository.findById(88L)).thenReturn(Optional.of(processo));
        Usuario usuario = Usuario.builder().id(5L).nome("Promotor").uf("CE").comarca("Fortaleza").tipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO).build();
        when(contextFactory.build()).thenReturn(new PerfilDashboardContext(usuario, null, null, null, null, null, null, null, null, null, null, null, null));
        var route = new InstitutionalActorRoutingService.InstitutionalRoute("FILA_DELEGACIA", "INBOX_DELEGACIA", TipoUsuario.SERVIDOR_FORUM, "DILIGENCIA", null, "motivo", java.util.Map.of());
        when(institutionalActorRoutingService.policeDiligence(88L)).thenReturn(route);
        when(workItemRepository.save(org.mockito.ArgumentMatchers.any(WorkItem.class))).thenAnswer(invocation -> {
            WorkItem item = invocation.getArgument(0);
            item.setId(500L);
            return item;
        });

        var resultado = service.requisitarDiligencia(88L, "detalhes da diligencia");

        verify(institutionalMaterialActionGuardService).requireAllowedForProcessAction(processo, InstitutionalMaterialActionGuardService.MaterialAction.MINISTERIO_PUBLICO_REQUISICAO_DILIGENCIA);
        assertThat(resultado.get("status")).isEqualTo("REQUISITADA");
        assertThat(resultado.get("workItemId")).isEqualTo(500L);
        assertThat(resultado.get("encaminhadoPara")).isEqualTo("INBOX_DELEGACIA");
        verify(commons).publishTerritoryHistory(usuario, "DELEGADO", "MP_REQUISITOU_DILIGENCIA", "Nova diligência recebida do MP.", processo, 500L);
    }
}
