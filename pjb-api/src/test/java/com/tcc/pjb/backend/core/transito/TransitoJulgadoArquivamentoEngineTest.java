package com.tcc.pjb.backend.core.transito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleDecision;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.AcaoProcessualServidor;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransitoJulgadoArquivamentoEngineTest {

    private ProcessoRepository processoRepository;
    private WorkItemRepository workItemRepository;
    private PainelServiceCommons commons;
    private PerfilDashboardContextFactory contextFactory;
    private PjbAuthorizationService authorizationService;
    private ProcessoLifecycleMachine lifecycleMachine;
    private PostJudgmentOperationalResolver operationalResolver;
    private TransitoJulgadoTerminalWorkflowSupport terminalWorkflowSupport;
    private ExecutionMeshStateService executionMeshStateService;
    private TransitoJulgadoArquivamentoEngine engine;

    @BeforeEach
    void setUp() {
        processoRepository = mock(ProcessoRepository.class);
        workItemRepository = mock(WorkItemRepository.class);
        commons = mock(PainelServiceCommons.class);
        contextFactory = mock(PerfilDashboardContextFactory.class);
        authorizationService = mock(PjbAuthorizationService.class);
        lifecycleMachine = mock(ProcessoLifecycleMachine.class);
        operationalResolver = mock(PostJudgmentOperationalResolver.class);
        ExecutionIncidentResolver executionIncidentResolver = mock(ExecutionIncidentResolver.class);
        ExecutionEnforcementResolver executionEnforcementResolver = mock(ExecutionEnforcementResolver.class);
        TransitoJulgadoPatrimonialWorkflowSupport patrimonialWorkflowSupport = mock(TransitoJulgadoPatrimonialWorkflowSupport.class);
        TransitoJulgadoExpropriationWorkflowSupport expropriationWorkflowSupport = mock(TransitoJulgadoExpropriationWorkflowSupport.class);
        terminalWorkflowSupport = mock(TransitoJulgadoTerminalWorkflowSupport.class);
        TransitoJulgadoExecutionDiagnosticSupport executionDiagnosticSupport = mock(TransitoJulgadoExecutionDiagnosticSupport.class);
        TransitoJulgadoNarrativeSupport narrativeSupport = mock(TransitoJulgadoNarrativeSupport.class);
        executionMeshStateService = mock(ExecutionMeshStateService.class);

        engine = new TransitoJulgadoArquivamentoEngine(
                processoRepository,
                workItemRepository,
                commons,
                contextFactory,
                authorizationService,
                lifecycleMachine,
                operationalResolver,
                executionIncidentResolver,
                executionEnforcementResolver,
                patrimonialWorkflowSupport,
                expropriationWorkflowSupport,
                terminalWorkflowSupport,
                executionDiagnosticSupport,
                narrativeSupport,
                executionMeshStateService
        );
    }

    private Processo processo() {
        return Processo.builder()
                .id(10L)
                .numeroProcesso("0001234-56.2026.8.06.0001")
                .uf("CE")
                .comarca("Fortaleza")
                .faseAtual(FaseProcessual.CUMPRIMENTO_SENTENCA)
                .statusProcesso(StatusProcesso.CUMPRIMENTO_SENTENCA)
                .unidadeJudiciariaCodigo("UNIDADE-1")
                .build();
    }

    private Usuario servidor() {
        return Usuario.builder().id(1L).tipoUsuario(TipoUsuario.SERVIDOR_FORUM).build();
    }

    private Usuario magistrado() {
        return Usuario.builder().id(2L).tipoUsuario(TipoUsuario.JUIZ).build();
    }

    private void stubHappyPathColaboradores(Processo processo) {
        ProcessoLifecycleDecision decision = new ProcessoLifecycleDecision(
                ProcessoLifecycleAction.ARQUIVAR,
                FaseProcessual.CUMPRIMENTO_SENTENCA,
                StatusProcesso.CUMPRIMENTO_SENTENCA,
                FaseProcessual.CUMPRIMENTO_SENTENCA,
                StatusProcesso.ARQUIVADO,
                true,
                null,
                null,
                null,
                null
        );
        when(lifecycleMachine.preview(eq(processo), eq(ProcessoLifecycleAction.ARQUIVAR))).thenReturn(decision);
        when(lifecycleMachine.apply(eq(processo), eq(ProcessoLifecycleAction.ARQUIVAR))).thenReturn(decision);

        PostJudgmentOperationalProfile profile = new PostJudgmentOperationalProfile(
                "ARQUIVAMENTO", "FILA-ARQ", "INBOX-ARQ", TipoUsuario.SERVIDOR_FORUM,
                1, false, 0L, null, "Art. 313 CPC", "FORMAL", "ENCERRADA", "ARQUIVO",
                "MESA-ARQ", "DEFINITIVA", "SATISFEITO", null, null, null, null);
        when(operationalResolver.resolve(eq(processo), eq(ProcessoLifecycleAction.ARQUIVAR), anyString(), anyDouble()))
                .thenReturn(profile);

        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> {
            WorkItem item = invocation.getArgument(0);
            item.setId(99L);
            return item;
        });

        when(executionMeshStateService.currentTerminalReference(eq(processo))).thenReturn(Map.of());

        TerminalArchiveLinkProfile archiveLinkProfile = new TerminalArchiveLinkProfile(
                "ARQUIVAR", "SATISFEITO", "ELEGIVEL", "VINCULADO", "FILA-ARQ", "INBOX-ARQ",
                "MESA-ARQ", "DEFINITIVA", "RESTRITO", "DESARQUIVAMENTO_CONTROLADO", TipoUsuario.SERVIDOR_FORUM,
                1, false, 0L, null, "Art. 313 CPC", null, null, null, null);
        when(terminalWorkflowSupport.resolveArchiveLinkProfile(eq(processo), eq("ARQUIVAR"), anyString(), anyString(), anyDouble(), anyDouble()))
                .thenReturn(archiveLinkProfile);
    }

    @Test
    void servidorComFuncaoArquivarAtivaArquivaComSucesso() {
        Processo processo = processo();
        Usuario servidor = servidor();
        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));
        when(contextFactory.build()).thenReturn(contextoCom(servidor));
        stubHappyPathColaboradores(processo);

        Map<String, Object> resultado = engine.determinarBaixaArquivamento(10L, "Cumprimento satisfeito integralmente");

        assertThat(resultado.get("status")).isEqualTo("PROCESSO_ARQUIVADO");
        verify(authorizationService).requireFuncaoServidorCapability(eq(processo), eq(AcaoProcessualServidor.ARQUIVAR));
        verify(processoRepository).save(processo);
    }

    @Test
    void servidorSemFuncaoArquivarAtivaLancaAccessDenied() {
        Processo processo = processo();
        Usuario servidor = servidor();
        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));
        when(contextFactory.build()).thenReturn(contextoCom(servidor));
        doThrow(new AccessDeniedPjbException("Acesso negado à ação processual do servidor"))
                .when(authorizationService).requireFuncaoServidorCapability(eq(processo), eq(AcaoProcessualServidor.ARQUIVAR));

        assertThrows(AccessDeniedPjbException.class,
                () -> engine.determinarBaixaArquivamento(10L, "Cumprimento satisfeito integralmente"));

        verify(lifecycleMachine, never()).preview(any(), any());
        verify(processoRepository, never()).save(any());
    }

    @Test
    void magistradoSemFuncaoServidorArquivaSemSerBloqueadoPelaNovaChecagem() {
        Processo processo = processo();
        Usuario magistrado = magistrado();
        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));
        when(contextFactory.build()).thenReturn(contextoCom(magistrado));
        stubHappyPathColaboradores(processo);

        Map<String, Object> resultado = engine.determinarBaixaArquivamento(10L, "Cumprimento satisfeito integralmente");

        assertThat(resultado.get("status")).isEqualTo("PROCESSO_ARQUIVADO");
        verify(authorizationService, never()).requireFuncaoServidorCapability(any(), any());
        verify(processoRepository).save(processo);
    }

    private PerfilDashboardContext contextoCom(Usuario usuario) {
        return new PerfilDashboardContext(usuario, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
