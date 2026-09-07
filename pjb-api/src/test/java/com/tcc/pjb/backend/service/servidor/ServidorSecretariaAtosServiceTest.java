package com.tcc.pjb.backend.service.servidor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.forum.ForumOfficialReturnOperationalService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.processo.ProcessoSlaJudicialService;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ServidorSecretariaAtosServiceTest {

    private final PerfilDashboardContextFactory contextFactory = mock(PerfilDashboardContextFactory.class);
    private final PainelServiceCommons commons = mock(PainelServiceCommons.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
    private final ProcessoLifecycleMachine lifecycleMachine = mock(ProcessoLifecycleMachine.class);
    private final ProcessoSlaJudicialService processoSlaJudicialService = mock(ProcessoSlaJudicialService.class);
    private final InstitutionalActorRoutingService institutionalActorRoutingService = mock(InstitutionalActorRoutingService.class);
    private final ForumOfficialReturnOperationalService forumOfficialReturnOperationalService = mock(ForumOfficialReturnOperationalService.class);
    private final OfficialDocumentTemplateService officialDocumentTemplateService = mock(OfficialDocumentTemplateService.class);

    private final ServidorSecretariaAtosService service = new ServidorSecretariaAtosService(
            contextFactory,
            commons,
            processoRepository,
            workItemRepository,
            authorizationService,
            lifecycleMachine,
            processoSlaJudicialService,
            institutionalActorRoutingService,
            forumOfficialReturnOperationalService,
            officialDocumentTemplateService
    );

    private void stubServidorContext() {
        Usuario servidor = new Usuario();
        servidor.setId(1L);
        PerfilDashboardContext ctx = new PerfilDashboardContext(
                servidor, null, LocalDateTime.now(), null, null,
                List.of(), List.of(), null, null, null, null, List.of(), null);
        when(contextFactory.build()).thenReturn(ctx);
    }

    private OfficialDocumentTemplateRenderResponse renderResponse(TemplateDocumentoOficial template) {
        return new OfficialDocumentTemplateRenderResponse(
                1L, "0001", template, "titulo", List.of(), List.of(),
                "conteudo", "hash", 1L, null, null, true, true, List.of(),
                Map.of(), Map.of());
    }

    @Test
    void realizarJuntadaCriaWorkItemConcluidoEAplicaLifecycle() {
        stubServidorContext();
        Processo processo = new Processo();
        processo.setId(40L);
        processo.setNumeroProcesso("0004444-00.2026.8.06.0001");
        when(processoRepository.findById(40L)).thenReturn(Optional.of(processo));
        var route = new InstitutionalActorRoutingService.InstitutionalRoute("FILA_GABINETE", "INBOX_GABINETE", null, "JUNTADA", null, "motivo", Map.of());
        when(institutionalActorRoutingService.gabineteReview(40L, "CIENCIA_JUNTADA")).thenReturn(route);
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(i -> {
            WorkItem item = i.getArgument(0);
            item.setId(500L);
            return item;
        });

        Map<String, Object> resultado = service.realizarJuntada(40L, "PETICAO", "descricao do documento", "BALCAO_VIRTUAL");

        assertThat(resultado.get("status")).isEqualTo("JUNTADA_REALIZADA");
        assertThat(resultado.get("tipo")).isEqualTo("PETICAO");
        verify(lifecycleMachine).apply(processo, ProcessoLifecycleAction.REALIZAR_JUNTADA);
        verify(commons).publishUserHistory(any(Usuario.class), org.mockito.ArgumentMatchers.eq("SERVIDOR"), org.mockito.ArgumentMatchers.eq("JUNTADA_REALIZADA"), any(), org.mockito.ArgumentMatchers.eq(processo), org.mockito.ArgumentMatchers.eq(40L));
    }

    @Test
    void conclusaoParaDespachoUsaSlaERotaDeGabinete() {
        stubServidorContext();
        Processo processo = new Processo();
        processo.setId(41L);
        processo.setNumeroProcesso("0004445-00.2026.8.06.0001");
        when(processoRepository.findById(41L)).thenReturn(Optional.of(processo));
        var sla = mock(ProcessoSlaJudicialService.ProcessoSlaSnapshot.class);
        java.time.Instant dueAt = java.time.Instant.now().plus(5, java.time.temporal.ChronoUnit.DAYS);
        when(sla.dueAtInitialConclusion()).thenReturn(dueAt);
        when(sla.prazoDespachoInicialDiasUteis()).thenReturn(5);
        when(processoSlaJudicialService.snapshot(processo)).thenReturn(sla);
        var route = new InstitutionalActorRoutingService.InstitutionalRoute("FILA_DESPACHO", "INBOX_DESPACHO", null, "DESPACHO", null, "motivo", Map.of());
        when(institutionalActorRoutingService.gabineteDecision(41L, "DESPACHO_INICIAL")).thenReturn(route);
        when(workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(41L, "CONCLUSAO:DESPACHO_INICIAL:41", WorkItemStatus.CANCELADO))
                .thenReturn(Optional.empty());
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(i -> {
            WorkItem item = i.getArgument(0);
            item.setId(501L);
            return item;
        });

        Map<String, Object> resultado = service.conclusaoParaDespacho(41L, "peticao inicial admitida");

        assertThat(resultado.get("status")).isEqualTo("CONCLUSO_PARA_DESPACHO");
        assertThat(resultado.get("slaDespachoDiasUteis")).isEqualTo(5);
        verify(lifecycleMachine).apply(processo, ProcessoLifecycleAction.CONCLUIR_PARA_DESPACHO);
    }

    @Test
    void expedeMandadoDeCitacaoRoteandoParaOOficialEGerandoDocumentoFormal() {
        stubServidorContext();
        Processo processo = new Processo();
        processo.setId(50L);
        processo.setNumeroProcesso("0001111-22.2026.8.06.0001");
        processo.setParteReuNome("João da Silva");
        when(processoRepository.findById(50L)).thenReturn(Optional.of(processo));

        WorkItem officialItem = WorkItem.builder().build();
        officialItem.setId(900L);
        when(workItemRepository.findById(900L)).thenReturn(Optional.of(officialItem));
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(forumOfficialReturnOperationalService.reativarPorExpedicaoAutomatica(any(Processo.class), any(), any(), any()))
                .thenReturn(Map.of("reativacao", Map.of("workItemId", 900L)));

        when(officialDocumentTemplateService.renderizar(any())).thenReturn(renderResponse(TemplateDocumentoOficial.MANDADO));

        Map<String, Object> resultado = service.expedirMandadoCitacao(50L, 77L, "Rua das Flores, 123", "Cuidado com cão bravo");

        assertThat(resultado.get("status")).isEqualTo("MANDADO_CITACAO_EXPEDIDO");
        assertThat(resultado.get("processoId")).isEqualTo(50L);
        assertThat(resultado.get("workItemId")).isEqualTo(900L);
        assertThat(resultado.get("enderecoCitacao")).isEqualTo("Rua das Flores, 123");
        assertThat(officialItem.getTitulo()).contains("Mandado de Citação").contains("0001111-22.2026.8.06.0001");
        assertThat(officialItem.getDescricao()).contains("Rua das Flores, 123").contains("Cuidado com cão bravo");
        verify(workItemRepository).save(officialItem);
    }

    @Test
    void rejeitaMandadoParaProcessoInexistenteSemChamarReativacaoOuDocumento() {
        stubServidorContext();
        when(processoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.expedirMandadoCitacao(999L, null, "Endereco X", null))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void expedicaoIntimacaoGeraDocumentoFormalENaoReativaOficialQuandoDestinatarioComum() {
        stubServidorContext();
        Processo processo = new Processo();
        processo.setId(60L);
        processo.setNumeroProcesso("0006666-00.2026.8.06.0001");
        when(processoRepository.findById(60L)).thenReturn(Optional.of(processo));
        var sla = mock(ProcessoSlaJudicialService.ProcessoSlaSnapshot.class);
        java.time.Instant dueAt = java.time.Instant.now().plus(15, java.time.temporal.ChronoUnit.DAYS);
        when(sla.dueAtInitialCommunication()).thenReturn(dueAt);
        when(sla.prazoCitacaoDiasUteis()).thenReturn(15);
        when(processoSlaJudicialService.snapshot(processo)).thenReturn(sla);
        when(workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(i -> i.getArgument(0));
        when(officialDocumentTemplateService.renderizar(any())).thenReturn(renderResponse(TemplateDocumentoOficial.INTIMACAO_FORMAL));

        Map<String, Object> resultado = service.expedicaoIntimacao(60L, "Advogado Fulano", "conteudo", null, null, false, null, null, null, null);

        assertThat(resultado.get("status")).isEqualTo("INTIMACAO_EXPEDIDA");
        assertThat(resultado.get("destinadaAoOficial")).isEqualTo(false);
        assertThat(resultado).doesNotContainKey("reativacaoOficial");
        verify(forumOfficialReturnOperationalService, org.mockito.Mockito.never()).reativarPorExpedicaoAutomatica(any(), any(), any(), any());
        verify(lifecycleMachine).apply(processo, ProcessoLifecycleAction.EXPEDIR_INTIMACAO);
    }
}
