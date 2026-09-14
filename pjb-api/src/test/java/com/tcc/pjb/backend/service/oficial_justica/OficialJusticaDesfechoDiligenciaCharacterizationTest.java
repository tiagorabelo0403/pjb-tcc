package com.tcc.pjb.backend.service.oficial_justica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Caracteriza o desfecho de mandado (cumprimento/frustracao/avaliacao) DEPOIS da extracao F6
 * de OficialJusticaPainelService (25 -> 21 deps) para OficialJusticaDesfechoDiligenciaService.
 * Mesmas asserções que valiam sobre o metodo na raiz antes da fatia -- prova que mover o corpo
 * nao mudou o comportamento.
 */
class OficialJusticaDesfechoDiligenciaCharacterizationTest {

    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final PainelServiceCommons commons = mock(PainelServiceCommons.class);
    private final PerfilDashboardContextFactory contextFactory = mock(PerfilDashboardContextFactory.class);
    private final InstitutionalActorRoutingService institutionalActorRoutingService = mock(InstitutionalActorRoutingService.class);
    private final InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService = mock(InstitutionalMultimediaWorkspaceService.class);
    private final OficialJusticaCommunicationFormalModelService communicationFormalModelService = mock(OficialJusticaCommunicationFormalModelService.class);

    private final OficialJusticaDesfechoDiligenciaService service = new OficialJusticaDesfechoDiligenciaService(
            workItemRepository,
            processoRepository,
            commons,
            contextFactory,
            institutionalActorRoutingService,
            institutionalMultimediaWorkspaceService,
            communicationFormalModelService
    );

    private Usuario usuario(Long id) {
        return Usuario.builder().id(id).nome("Oficial").uf("CE").comarca("Fortaleza").tipoUsuario(TipoUsuario.OFICIAL_JUSTICA_AVALIADOR).build();
    }

    private void stubUsuario(Usuario usuario) {
        PerfilDashboardContext ctx = new PerfilDashboardContext(usuario, null, null, null, null, null, null, null, null, null, null, null, null);
        when(contextFactory.build()).thenReturn(ctx);
    }

    @Test
    void registrarCumprimentoMarcaConcluidoESalvaComFormalizacao() {
        Usuario usuario = usuario(10L);
        stubUsuario(usuario);
        Processo processo = new Processo();
        processo.setId(77L);
        WorkItem item = WorkItem.builder().processo(processo).status(WorkItemStatus.PENDENTE).descricao("original").build();
        item.setId(5L);
        when(workItemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(workItemRepository.save(item)).thenReturn(item);
        Map<String, Object> formalization = Map.of("formal", true);
        when(communicationFormalModelService.formalizeOutcome(processo, item, usuario, "req", false)).thenReturn(formalization);
        when(communicationFormalModelService.appendFormalTrace("original", processo, item, usuario, "req", false, formalization)).thenReturn("original+trace");
        Map<String, Object> mapWorkItemOut = Map.of("id", 5L);
        when(commons.mapWorkItem(item)).thenReturn(mapWorkItemOut);
        when(institutionalMultimediaWorkspaceService.enrich(any())).thenReturn(Map.of("anexos", true));

        Map<String, Object> out = service.registrarCumprimento("5", "req");

        assertThat(item.getStatus()).isEqualTo(WorkItemStatus.CONCLUIDO);
        assertThat(item.getDescricao()).isEqualTo("original+trace");
        verify(workItemRepository).save(item);
        verify(commons).publishTerritoryHistory(usuario, "OFICIAL", "MANDADO_CUMPRIDO", "Mandado cumprido registrado.", processo, 5L);
        assertThat(out.get("id")).isEqualTo(5L);
        assertThat(out.get("formalization")).isEqualTo(formalization);
        assertThat(out.get("anexos")).isEqualTo(true);
    }

    @Test
    void registrarFrustracaoCriaFollowupComRotaDaSecretaria() {
        Usuario usuario = usuario(11L);
        stubUsuario(usuario);
        Processo processo = new Processo();
        processo.setId(88L);
        WorkItem item = WorkItem.builder().processo(processo).status(WorkItemStatus.PENDENTE).descricao("original").build();
        item.setId(6L);
        when(workItemRepository.findById(6L)).thenReturn(Optional.of(item));
        when(workItemRepository.save(item)).thenReturn(item);
        InstitutionalActorRoutingService.InstitutionalRoute route = new InstitutionalActorRoutingService.InstitutionalRoute(
                "FILA_SECRETARIA", "INBOX_SECRETARIA", TipoUsuario.SERVIDOR_FORUM, "CERTIDAO_NEGATIVA", "TOPO", "motivo", Map.of());
        when(institutionalActorRoutingService.secretaryExecution(88L, "CERTIDAO_NEGATIVA")).thenReturn(route);
        WorkItem savedFollowup = WorkItem.builder().processo(processo).status(WorkItemStatus.PENDENTE).build();
        savedFollowup.setId(7L);
        when(workItemRepository.save(org.mockito.ArgumentMatchers.argThat(w -> w != item))).thenReturn(savedFollowup);
        Map<String, Object> formalization = Map.of("formal", true);
        when(communicationFormalModelService.formalizeOutcome(processo, item, usuario, "req", true)).thenReturn(formalization);
        when(communicationFormalModelService.appendFormalTrace(eq("original"), eq(processo), eq(item), eq(usuario), eq("req"), eq(true), eq(formalization))).thenReturn("original+trace");
        when(commons.mapWorkItem(item)).thenReturn(Map.of("mandadoId", 6L));
        when(commons.mapWorkItem(savedFollowup)).thenReturn(Map.of("followupId", 7L));
        when(institutionalMultimediaWorkspaceService.enrich(any())).thenReturn(Map.of());

        Map<String, Object> out = service.registrarFrustracao("6", "req");

        assertThat(item.getStatus()).isEqualTo(WorkItemStatus.CONCLUIDO);
        verify(institutionalActorRoutingService).secretaryExecution(88L, "CERTIDAO_NEGATIVA");
        assertThat(out.get("mandado")).isEqualTo(Map.of("mandadoId", 6L));
        assertThat(out.get("followup")).isEqualTo(Map.of("followupId", 7L));
    }

    @Test
    void registrarAvaliacaoUsaRotaDeOficialJusticaERegistraConcluido() {
        Usuario usuario = usuario(12L);
        stubUsuario(usuario);
        Processo processo = new Processo();
        processo.setId(99L);
        when(processoRepository.findById(99L)).thenReturn(Optional.of(processo));
        InstitutionalActorRoutingService.InstitutionalRoute route = new InstitutionalActorRoutingService.InstitutionalRoute(
                "FILA_AVALIACAO", "INBOX_AVALIACAO", TipoUsuario.OFICIAL_JUSTICA_AVALIADOR, "AVALIACAO_PENHORA", "TOPO", "motivo", Map.of());
        when(institutionalActorRoutingService.officialJustice(99L, true, "AVALIACAO_PENHORA")).thenReturn(route);
        WorkItem saved = WorkItem.builder().processo(processo).status(WorkItemStatus.CONCLUIDO).build();
        saved.setId(8L);
        when(workItemRepository.save(any(WorkItem.class))).thenReturn(saved);
        when(commons.mapWorkItem(saved)).thenReturn(Map.of("avaliacaoId", 8L));
        when(institutionalMultimediaWorkspaceService.enrich(any())).thenReturn(Map.of());

        Map<String, Object> out = service.registrarAvaliacao(99L, "req-avaliacao");

        verify(institutionalActorRoutingService).officialJustice(99L, true, "AVALIACAO_PENHORA");
        verify(commons).publishUserHistory(usuario, "OFICIAL", "AVALIACAO_REGISTRADA", "Avaliação patrimonial registrada.", processo, 8L);
        assertThat(out.get("avaliacaoId")).isEqualTo(8L);
    }
}
