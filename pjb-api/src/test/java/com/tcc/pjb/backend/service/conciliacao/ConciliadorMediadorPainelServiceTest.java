package com.tcc.pjb.backend.service.conciliacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.intelligence.JudgeAgreementApprovalService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConciliadorMediadorPainelServiceTest {

    private final PerfilDashboardContextFactory contextFactory = mock(PerfilDashboardContextFactory.class);
    private final PainelServiceCommons commons = mock(PainelServiceCommons.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final InstitutionalActorRoutingService institutionalActorRoutingService = mock(InstitutionalActorRoutingService.class);
    private final JudgeAgreementApprovalService judgeAgreementApprovalService = mock(JudgeAgreementApprovalService.class);

    private ConciliadorMediadorPainelService service;

    @BeforeEach
    void setUp() {
        service = new ConciliadorMediadorPainelService(
                contextFactory, commons, processoRepository, workItemRepository,
                institutionalActorRoutingService, judgeAgreementApprovalService);
    }

    @Test
    void deveEncerrarSessaoSemAcordoRetornandoStatusEResultado() {
        Usuario usuario = usuarioComTipo(TipoUsuario.CONCILIADOR_CEJUSC);
        PerfilDashboardContext ctx = new PerfilDashboardContext(
                usuario, null, LocalDateTime.now(), "CONCILIACAO", null,
                List.of(), List.of(), null, null, null, null, List.of(), null);
        when(contextFactory.build()).thenReturn(ctx);

        Map<String, Object> result = service.encerrarSessaoSemAcordo("SESSAO-001", Map.of());

        assertThat(result.get("status")).isEqualTo("ENCERRADA");
        assertThat(result.get("resultado")).isEqualTo("SEM_ACORDO");
        assertThat(result.get("sessaoId")).isEqualTo("SESSAO-001");
    }

    @Test
    void deveRegistrarAcordoSemProcessoIdQuandoRequestSemProcessoId() {
        Usuario usuario = usuarioComTipo(TipoUsuario.MEDIADOR);
        PerfilDashboardContext ctx = new PerfilDashboardContext(
                usuario, null, LocalDateTime.now(), "MEDIACAO", null,
                List.of(), List.of(), null, null, null, null, List.of(), null);
        when(contextFactory.build()).thenReturn(ctx);

        WorkItem savedItem = new WorkItem();
        savedItem.setId(55L);
        when(workItemRepository.save(any())).thenReturn(savedItem);

        Map<String, Object> result = service.registrarAcordo("SESSAO-002", Map.of("clausulas", "Pagamento 30 dias"));

        assertThat(result.get("status")).isEqualTo("ACORDO_REGISTRADO");
        assertThat(result).containsKey("workItemId");
    }

    @Test
    void deveCarregarMetricasMesComChavesObrigatorias() {
        Usuario usuario = usuarioComTipo(TipoUsuario.CONCILIADOR_CEJUSC);
        PerfilDashboardContext ctx = new PerfilDashboardContext(
                usuario, null, LocalDateTime.now(), "CONCILIACAO", null,
                List.of(), List.of(), null, null, null, null, List.of(), null);
        when(contextFactory.build()).thenReturn(ctx);
        when(commons.inboxHibrido(any(), anyInt())).thenReturn(List.of());
        when(commons.agenda(any(), any())).thenReturn(List.of());

        Map<String, Object> metricas = service.carregarMetricasMes();

        assertThat(metricas).containsKey("sessoes");
        assertThat(metricas).containsKey("homologacoesPendentes");
        assertThat(metricas).containsKey("taxaAcordoEstimada");
    }

    private Usuario usuarioComTipo(TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setId(10L);
        u.setTipoUsuario(tipo);
        return u;
    }
}
