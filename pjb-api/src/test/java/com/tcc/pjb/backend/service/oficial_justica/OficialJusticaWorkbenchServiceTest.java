package com.tcc.pjb.backend.service.oficial_justica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaDiligenciaQueueResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OficialJusticaWorkbenchServiceTest {

    private final PerfilDashboardContextFactory contextFactory = mock(PerfilDashboardContextFactory.class);
    private final PainelServiceCommons commons = mock(PainelServiceCommons.class);
    private final PjbTimeService timeService = mock(PjbTimeService.class);
    private final PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
    private final OficialJusticaProcessoVinculoService vinculoService = mock(OficialJusticaProcessoVinculoService.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final OficialJusticaOficioSecurityService oficioSecurityService = mock(OficialJusticaOficioSecurityService.class);
    private final OficialJusticaEnderecoTriageService enderecoTriageService = mock(OficialJusticaEnderecoTriageService.class);
    private final OficialJusticaOrganizationalScopeService organizationalScopeService = mock(OficialJusticaOrganizationalScopeService.class);
    private final OficialJusticaContextEnvelopeService contextEnvelopeService = mock(OficialJusticaContextEnvelopeService.class);
    private final OficialJusticaExecucaoVivaService execucaoVivaService = mock(OficialJusticaExecucaoVivaService.class);
    private final OficialJusticaPanelEgressService panelEgressService = mock(OficialJusticaPanelEgressService.class);

    private OficialJusticaWorkbenchService service;

    private Usuario oficial;

    @BeforeEach
    void setUp() {
        service = new OficialJusticaWorkbenchService(
                contextFactory, commons, timeService, authorizationService,
                vinculoService, processoRepository, oficioSecurityService,
                enderecoTriageService, organizationalScopeService, contextEnvelopeService,
                execucaoVivaService, panelEgressService
        );

        oficial = new Usuario();
        oficial.setId(1L);
        oficial.setTipoUsuario(TipoUsuario.OFICIAL_JUSTICA);
        oficial.setUf("CE");
        oficial.setComarca("Fortaleza");

        PerfilDashboardContext ctx = new PerfilDashboardContext(
                oficial, null, LocalDateTime.now(), null, null,
                List.of(), List.of(), null, null, null, null, List.of(), null
        );
        when(contextFactory.build()).thenReturn(ctx);
        when(timeService.nowUtc()).thenReturn(Instant.now());

        OficialJusticaOrganizationalScopeService.Scope scope =
                new OficialJusticaOrganizationalScopeService.Scope(
                        "COMARCA", "Fortaleza", false, true,
                        List.of(), List.of(), "CE", "Fortaleza"
                );
        when(vinculoService.vinculosDiretosUsuario(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(organizationalScopeService.filterByScope(any(), any())).thenReturn(List.of());
        when(panelEgressService.reconcileVisibility(any(), any()))
                .thenReturn(new OficialJusticaPanelEgressService.VisibilitySnapshot(List.of(), 0, List.of()));
        when(organizationalScopeService.resolve(any(), any())).thenReturn(scope);
        when(organizationalScopeService.processNumbersByRito(any())).thenReturn(Map.of());
        when(organizationalScopeService.availableVaras(any())).thenReturn(List.of());
        when(organizationalScopeService.availableLotacoes(any(), any())).thenReturn(List.of());
        when(organizationalScopeService.toMap(any())).thenReturn(Map.of());
        when(contextEnvelopeService.oficialEnvelope(any(), any())).thenReturn(Map.of());
    }

    @Test
    void deveRetornarFilaVaziaQuandoOficialNaoTemVinculos() {
        OficialJusticaDiligenciaQueueResponse response =
                service.filaViva(20, null, null, null, null, null);

        assertThat(response).isNotNull();
        assertThat(response.rows()).isEmpty();
        assertThat(response.summary().totalRows()).isZero();
        assertThat(response.summary().atrasadas()).isZero();
        assertThat(response.summary().criticas()).isZero();
    }

    @Test
    void deveLimitarFilaAoMaximoDe80Itens() {
        OficialJusticaDiligenciaQueueResponse resposta100 =
                service.filaViva(100, null, null, null, null, null);
        OficialJusticaDiligenciaQueueResponse resposta0 =
                service.filaViva(0, null, null, null, null, null);

        assertThat(resposta100).isNotNull();
        assertThat(resposta0).isNotNull();
    }

    @Test
    void devePainelResumoConterCamposEssenciais() {
        Map<String, Object> painel = service.painelResumo();

        assertThat(painel).containsKey("enabled");
        assertThat(painel).containsKey("mode");
        assertThat(painel).containsKey("summary");
        assertThat(painel).containsKey("execucaoVivaResumo");
        assertThat(painel.get("enabled")).isEqualTo(Boolean.TRUE);
    }
}
