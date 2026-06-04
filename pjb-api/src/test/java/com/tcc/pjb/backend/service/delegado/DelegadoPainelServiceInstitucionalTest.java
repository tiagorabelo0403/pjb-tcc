package com.tcc.pjb.backend.service.delegado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.profile.operational.DelegadoDiligenciaRequest;
import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.criminal.InqueritoPolicialDigital;
import com.tcc.pjb.backend.model.entity.enums.StatusUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.InqueritoPolicialDigitalRepository;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.criminal.InqueritoMultimidiaWorkspaceService;
import com.tcc.pjb.backend.service.criminal.PjbPoliceNativeExecutionService;
import com.tcc.pjb.backend.service.criminal.PjbPoliceNativeToolbeltService;
import com.tcc.pjb.backend.service.criminal.PoliceInvestigationSystemLandscapeService;
import com.tcc.pjb.backend.service.criminal.PoliceSovereignOperationalWorkbenchService;
import com.tcc.pjb.backend.service.criminal.PoliceTraceableExecutionLedgerService;
import com.tcc.pjb.backend.service.criminal.PoliceTransactionalAdapterMeshService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorTopologyMeshService;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoIntelligenceSummaryService;
import com.tcc.pjb.backend.service.painel.shared.PainelActionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelExecutionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelNativeCollectionCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.shared.PainelSignalReflectionService;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.guard.InstitutionalMaterialActionGuardService;
import com.tcc.pjb.backend.service.profile.PerfilCapabilityMatrixService;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalPanelBrandingService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class DelegadoPainelServiceInstitucionalTest {

    private final PerfilDashboardContextFactory contextFactory = mock(PerfilDashboardContextFactory.class);
    private final PainelServiceCommons commons = mock(PainelServiceCommons.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final InqueritoPolicialDigitalRepository inqueritoRepository = mock(InqueritoPolicialDigitalRepository.class);
    private final UnidadeInstituicaoRepository unidadeRepository = mock(UnidadeInstituicaoRepository.class);
    private final LotacaoInstituicaoRepository lotacaoRepository = mock(LotacaoInstituicaoRepository.class);
    private final InstitutionalActorRoutingService routingService = mock(InstitutionalActorRoutingService.class);
    private final InstitutionalMaterialActionGuardService materialGuard = mock(InstitutionalMaterialActionGuardService.class);

    private final DelegadoPainelService service = new DelegadoPainelService(
            contextFactory,
            commons,
            processoRepository,
            workItemRepository,
            inqueritoRepository,
            unidadeRepository,
            lotacaoRepository,
            mock(com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService.class),
            mock(PerfilCapabilityMatrixService.class),
            mock(PessoaLocalizacaoIntelligenceSummaryService.class),
            mock(InstitutionalActorTopologyMeshService.class),
            routingService,
            mock(InstitutionalPanelBrandingService.class),
            mock(InqueritoMultimidiaWorkspaceService.class),
            mock(PoliceInvestigationSystemLandscapeService.class),
            mock(PjbPoliceNativeToolbeltService.class),
            mock(PoliceTransactionalAdapterMeshService.class),
            mock(PoliceSovereignOperationalWorkbenchService.class),
            mock(PjbPoliceNativeExecutionService.class),
            mock(PoliceTraceableExecutionLedgerService.class),
            mock(QualifiedDocumentSignatureEnvelopeService.class),
            mock(PainelSharedExperienceService.class),
            mock(PainelSignalReflectionService.class),
            mock(PainelNativeCollectionCompositionService.class),
            mock(PainelActionSurfaceCompositionService.class),
            mock(PainelExecutionSurfaceCompositionService.class),
            materialGuard
    );

    @Test
    void registrarDiligenciaDelegaciaLotadaCriaWorkItemParaMinisterioPublico() {
        Usuario delegado = usuario(5L);
        UnidadeInstituicao delegacia = delegacia(10L);
        Processo processo = Processo.builder().id(20L).numeroProcesso("000020").faseAtual(FaseProcessual.CONHECIMENTO).build();
        InqueritoPolicialDigital inquerito = inquerito(30L, delegacia, processo);
        when(contextFactory.build()).thenReturn(contexto(delegado));
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(delegacia));
        when(lotacaoRepository.findAtivasByUsuario(delegado)).thenReturn(List.of(lotacao(delegacia)));
        when(inqueritoRepository.findById(30L)).thenReturn(Optional.of(inquerito));
        when(routingService.ministerioPublico(20L, "DILIGENCIA_REQUISITADA")).thenReturn(route());
        when(workItemRepository.save(any())).thenAnswer(invocation -> {
            WorkItem item = invocation.getArgument(0);
            item.setId(900L);
            return item;
        });

        Map<String, Object> out = service.registrarDiligencia(new DelegadoDiligenciaRequest(
                20L, 30L, 10L, "Ouvir testemunha presencial", "CPP art. 6", "ALTA"));

        ArgumentCaptor<WorkItem> captor = ArgumentCaptor.forClass(WorkItem.class);
        verify(workItemRepository).save(captor.capture());
        WorkItem saved = captor.getValue();
        assertThat(out).containsEntry("status", "CRIADO")
                .containsEntry("workItemId", 900L)
                .containsEntry("processoId", 20L)
                .containsEntry("inqueritoId", 30L)
                .containsEntry("unidadeApuracaoId", 10L);
        assertThat(saved.getProcesso()).isSameAs(processo);
        assertThat(saved.getAssignedRole()).isEqualTo(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        assertThat(saved.getUf()).isEqualTo("CE");
        assertThat(saved.getComarca()).isEqualTo("Fortaleza");
        assertThat(saved.getDescricao()).contains("unidadeApuracaoId=10", "inqueritoId=30", "descricao=Ouvir testemunha presencial");
        verify(materialGuard).requireAllowedForProcessAction(processo, InstitutionalMaterialActionGuardService.MaterialAction.DELEGADO_DILIGENCIA);
    }

    @Test
    void registrarDiligenciaInqueritoDeOutraDelegaciaNaoCriaWorkItem() {
        Usuario delegado = usuario(5L);
        UnidadeInstituicao delegaciaRequisitada = delegacia(10L);
        UnidadeInstituicao outraDelegacia = delegacia(11L);
        Processo processo = Processo.builder().id(20L).numeroProcesso("000020").faseAtual(FaseProcessual.CONHECIMENTO).build();
        InqueritoPolicialDigital inquerito = inquerito(30L, outraDelegacia, processo);
        when(contextFactory.build()).thenReturn(contexto(delegado));
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(delegaciaRequisitada));
        when(lotacaoRepository.findAtivasByUsuario(delegado)).thenReturn(List.of(lotacao(delegaciaRequisitada)));
        when(inqueritoRepository.findById(30L)).thenReturn(Optional.of(inquerito));

        assertThrows(IllegalStateException.class, () -> service.registrarDiligencia(new DelegadoDiligenciaRequest(
                20L, 30L, 10L, "Ouvir testemunha presencial", "CPP art. 6", "ALTA")));

        verify(workItemRepository, never()).save(any());
        verify(materialGuard, never()).requireAllowedForProcessAction(any(), any());
    }

    private Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Delegado " + id);
        usuario.setTipoUsuario(TipoUsuario.DELEGADO_POLICIA);
        return usuario;
    }

    private PerfilDashboardContext contexto(Usuario usuario) {
        return new PerfilDashboardContext(
                usuario,
                null,
                LocalDateTime.now(),
                "DELEGADO",
                "Delegado",
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                List.of(),
                null
        );
    }

    private UnidadeInstituicao delegacia(Long id) {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        ReflectionTestUtils.setField(unidade, "id", id);
        unidade.setNome("Delegacia de Fortaleza");
        unidade.setTipo(TipoUnidadeInstitucional.DELEGACIA);
        unidade.setStatusUnidade(StatusUnidadeInstitucional.ATIVA);
        unidade.setUf("CE");
        unidade.setComarca("Fortaleza");
        return unidade;
    }

    private LotacaoInstituicao lotacao(UnidadeInstituicao unidade) {
        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setUnidade(unidade);
        return lotacao;
    }

    private InqueritoPolicialDigital inquerito(Long id, UnidadeInstituicao unidade, Processo processo) {
        InqueritoPolicialDigital inquerito = new InqueritoPolicialDigital();
        ReflectionTestUtils.setField(inquerito, "id", id);
        inquerito.setNumeroProcedimento("IPD-" + id);
        inquerito.setTipo("INQUERITO_POLICIAL");
        inquerito.setStatus("INSTAURADO");
        inquerito.setFaseAtual("INVESTIGACAO");
        inquerito.setNaturezaFato("Roubo majorado");
        inquerito.setResumoFatos("Resumo mínimo dos fatos");
        inquerito.setUnidadeApuracao(unidade);
        inquerito.setProcessoVinculado(processo);
        return inquerito;
    }

    private InstitutionalActorRoutingService.InstitutionalRoute route() {
        return new InstitutionalActorRoutingService.InstitutionalRoute(
                "MP_DILIGENCIA",
                "MP:DILIGENCIA:20",
                TipoUsuario.MEMBRO_MINISTERIO_PUBLICO,
                "MP",
                "MP:DILIGENCIA:20",
                "Rota penal",
                Map.of()
        );
    }
}
