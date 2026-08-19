package com.tcc.pjb.backend.service.defensor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
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
import com.tcc.pjb.backend.service.institutional.movimentacao.MovimentacaoProcessualRegistrar;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorTopologyMeshService;
import com.tcc.pjb.backend.service.painel.shared.PainelActionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelExecutionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelNativeCollectionCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.shared.PainelSignalReflectionService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DefensoriaPublicaOperacionalServiceAjgComoParteTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final PerfilDashboardContextFactory contextFactory = mock(PerfilDashboardContextFactory.class);
    private final InstitutionalActorRoutingService institutionalActorRoutingService = mock(InstitutionalActorRoutingService.class);
    private final MovimentacaoProcessualRegistrar movimentacaoRegistrar = mock(MovimentacaoProcessualRegistrar.class);

    private DefensoriaPublicaOperacionalService service() {
        return new DefensoriaPublicaOperacionalService(
                contextFactory,
                mock(PainelServiceCommons.class),
                processoRepository,
                workItemRepository,
                mock(PjbAuthorizationService.class),
                mock(InstitutionalActorTopologyMeshService.class),
                institutionalActorRoutingService,
                mock(PainelSharedExperienceService.class),
                mock(PainelSignalReflectionService.class),
                mock(PainelNativeCollectionCompositionService.class),
                mock(PainelActionSurfaceCompositionService.class),
                mock(PainelExecutionSurfaceCompositionService.class),
                movimentacaoRegistrar);
    }

    private Processo processoComParte(String cpfParte) {
        Processo processo = new Processo();
        processo.setId(80L);
        processo.setNumeroProcesso("PROC-80");
        processo.setParteAutoraCpf(cpfParte);
        return processo;
    }

    @Test
    void cidadaoParteDoProcessoConsegueSolicitarAjgDiretamente() {
        Usuario cidadao = Usuario.builder().id(40L).nome("Cidadao").tipoUsuario(TipoUsuario.CIDADAO).cpf("11122233344").build();
        when(contextFactory.build()).thenReturn(new PerfilDashboardContext(cidadao, null, null, null, null, null, null, null, null, null, null, null, null));
        when(processoRepository.findById(80L)).thenReturn(Optional.of(processoComParte("11122233344")));
        when(institutionalActorRoutingService.gabineteDecision(anyLong(), anyString()))
                .thenReturn(new InstitutionalActorRoutingService.InstitutionalRoute("QUEUE", "INBOX", TipoUsuario.SERVIDOR_FORUM, "AXIS", "KEY", "rationale", Map.of()));
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> {
            WorkItem item = invocation.getArgument(0);
            item.setId(99L);
            return item;
        });

        Map<String, Object> resultado = service().solicitarAssistenciaJudiciariaGratuitaComoParte(80L, "salario minimo", "desempregado");

        assertThat(resultado).containsEntry("status", "AJG_SOLICITADA");
    }

    @Test
    void rejeitaCidadaoQueNaoEParteDoProcesso() {
        Usuario cidadao = Usuario.builder().id(41L).nome("Outro Cidadao").tipoUsuario(TipoUsuario.CIDADAO).cpf("99988877766").build();
        when(contextFactory.build()).thenReturn(new PerfilDashboardContext(cidadao, null, null, null, null, null, null, null, null, null, null, null, null));
        when(processoRepository.findById(80L)).thenReturn(Optional.of(processoComParte("11122233344")));

        assertThatThrownBy(() -> service().solicitarAssistenciaJudiciariaGratuitaComoParte(80L, "salario minimo", "desempregado"))
                .isInstanceOf(AccessDeniedPjbException.class);
    }

    @Test
    void rejeitaUsuarioQueNaoEhCidadao() {
        Usuario advogado = Usuario.builder().id(42L).nome("Advogado").tipoUsuario(TipoUsuario.ADVOGADO).cpf("11122233344").build();
        when(contextFactory.build()).thenReturn(new PerfilDashboardContext(advogado, null, null, null, null, null, null, null, null, null, null, null, null));
        when(processoRepository.findById(80L)).thenReturn(Optional.of(processoComParte("11122233344")));

        assertThatThrownBy(() -> service().solicitarAssistenciaJudiciariaGratuitaComoParte(80L, "salario minimo", "desempregado"))
                .isInstanceOf(AccessDeniedPjbException.class);
    }
}
