package com.tcc.pjb.backend.service.servidor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.calendar.CalendarInstitutionalBridgeService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.forum.ForumOfficialReturnOperationalService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.painel.shared.PainelActionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelExecutionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelNativeCollectionCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.shared.PainelSignalReflectionService;
import com.tcc.pjb.backend.service.processo.ProcessoSlaJudicialService;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ServidorSecretariaOperacionalServiceMandadoCitacaoTest {

    private final PerfilDashboardContextFactory contextFactory = mock(PerfilDashboardContextFactory.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final ForumOfficialReturnOperationalService forumOfficialReturnOperationalService = mock(ForumOfficialReturnOperationalService.class);
    private final OfficialDocumentTemplateService officialDocumentTemplateService = mock(OfficialDocumentTemplateService.class);

    private final ServidorSecretariaOperacionalService service = new ServidorSecretariaOperacionalService(
            contextFactory,
            mock(PainelServiceCommons.class),
            processoRepository,
            workItemRepository,
            mock(PjbAuthorizationService.class),
            mock(ProcessoLifecycleMachine.class),
            mock(ProcessoSlaJudicialService.class),
            mock(InstitutionalActorRoutingService.class),
            forumOfficialReturnOperationalService,
            mock(CalendarInstitutionalBridgeService.class),
            officialDocumentTemplateService,
            mock(PainelSharedExperienceService.class),
            mock(PainelSignalReflectionService.class),
            mock(PainelNativeCollectionCompositionService.class),
            mock(PainelActionSurfaceCompositionService.class),
            mock(PainelExecutionSurfaceCompositionService.class));

    private void stubServidorContext() {
        Usuario servidor = new Usuario();
        servidor.setId(1L);
        PerfilDashboardContext ctx = new PerfilDashboardContext(
                servidor, null, LocalDateTime.now(), null, null,
                List.of(), List.of(), null, null, null, null, List.of(), null);
        when(contextFactory.build()).thenReturn(ctx);
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

        when(officialDocumentTemplateService.renderizar(any()))
                .thenReturn(new OfficialDocumentTemplateRenderResponse(
                        50L, "0001111-22.2026.8.06.0001", TemplateDocumentoOficial.MANDADO,
                        "Mandado de citação — 0001111-22.2026.8.06.0001",
                        List.of("qualificacaoPartes", "ordemJudicial", "prazoCumprimento"), List.of(),
                        "conteudo renderizado", "hash-abc", 123L, null, null, true, true, List.of(),
                        Map.of("assinado", true), Map.of("valido", true)));

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
}
