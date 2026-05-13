package com.tcc.pjb.backend.service.conciliacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConciliadorMediadorEnhancedServiceTest {

    @Test
    void lavraTermoDeAcordoComDocumentoFormalAssinado() {
        PerfilDashboardContextFactory contextFactory = Mockito.mock(PerfilDashboardContextFactory.class);
        PainelServiceCommons commons = Mockito.mock(PainelServiceCommons.class);
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        WorkItemRepository workItemRepository = Mockito.mock(WorkItemRepository.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        ProcessoLifecycleMachine lifecycleMachine = Mockito.mock(ProcessoLifecycleMachine.class);
        InstitutionalActorRoutingService routingService = Mockito.mock(InstitutionalActorRoutingService.class);
        OfficialDocumentTemplateService officialDocumentTemplateService = Mockito.mock(OfficialDocumentTemplateService.class);

        Usuario usuario = new Usuario();
        usuario.setId(12L);
        usuario.setNome("Conciliador CEJUSC");
        usuario.setTipoUsuario(TipoUsuario.CONCILIADOR_CEJUSC);
        usuario.setComarca("Quixadá");
        PerfilDashboardContext context = new PerfilDashboardContext(usuario, null, LocalDateTime.now(), "CONCILIADOR_CEJUSC", "TRATAMENTO", List.of(), List.of(), null, null, null, null, List.of(), null);
        when(contextFactory.build()).thenReturn(context);

        Processo processo = new Processo();
        processo.setId(501L);
        processo.setNumeroProcesso("0001111-22.2026.8.06.0001");
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        when(processoRepository.findById(501L)).thenReturn(Optional.of(processo));
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> {
            WorkItem item = invocation.getArgument(0);
            item.setId(800L);
            return item;
        });
        when(routingService.gabineteDecision(501L, "HOMOLOGACAO_ACORDO")).thenReturn(new InstitutionalActorRoutingService.InstitutionalRoute("Q", "I", TipoUsuario.JUIZ, "HOMOLOGACAO_ACORDO", "TOPO", "desc", Map.of()));
        when(officialDocumentTemplateService.renderizar(any())).thenReturn(new OfficialDocumentTemplateRenderResponse(
                501L,
                processo.getNumeroProcesso(),
                TemplateDocumentoOficial.TERMO_ACORDO,
                "Termo de acordo",
                List.of(),
                List.of(),
                "CONTEUDO_ASSINADO",
                "ab".repeat(32),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                DocumentoCategoria.PUBLICO,
                NivelSigilo.PUBLICO,
                true,
                true,
                List.of(),
                Map.of("rubrica", "PJB-RUB-ACORDO", "envelopeId", "PJB-ENV-ACORDO"),
                Map.of("status", "VALIDO")
        ));

        ConciliadorMediadorEnhancedService service = new ConciliadorMediadorEnhancedService(
                contextFactory, commons, processoRepository, workItemRepository, authorizationService, lifecycleMachine, routingService, officialDocumentTemplateService
        );

        var response = service.lavrarTermoAcordo(501L, "Pagamento em 3 parcelas", List.of("Autor", "Réu"), 1500.00);

        assertThat(response).containsEntry("status", "TERMO_LAVRADO_AGUARDANDO_HOMOLOGACAO");
        assertThat(response).containsKey("documentoFormalAssinado");
        @SuppressWarnings("unchecked")
        Map<String, Object> documentoFormalAssinado = (Map<String, Object>) response.get("documentoFormalAssinado");
        assertThat(documentoFormalAssinado).containsEntry("template", "TERMO_ACORDO");
    }
}
