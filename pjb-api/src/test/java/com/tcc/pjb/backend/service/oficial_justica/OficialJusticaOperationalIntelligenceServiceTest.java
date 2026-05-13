package com.tcc.pjb.backend.service.oficial_justica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoIntelligenceSummaryService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OficialJusticaOperationalIntelligenceServiceTest {

    @Test
    void summarizesOfficialInbox() {
        PerfilDashboardContextFactory contextFactory = Mockito.mock(PerfilDashboardContextFactory.class);
        PainelServiceCommons commons = Mockito.mock(PainelServiceCommons.class);
        WorkItemRepository workItemRepository = Mockito.mock(WorkItemRepository.class);
        PessoaLocalizacaoIntelligenceSummaryService intelligenceSummaryService = Mockito.mock(PessoaLocalizacaoIntelligenceSummaryService.class);
        OficialJusticaEnderecoTriageService enderecoTriageService = Mockito.mock(OficialJusticaEnderecoTriageService.class);
        OficialJusticaOperationalIntelligenceService service = new OficialJusticaOperationalIntelligenceService(contextFactory, commons, workItemRepository, intelligenceSummaryService, enderecoTriageService);
        Usuario usuario = new Usuario();
        usuario.setId(3L);
        usuario.setTipoUsuario(TipoUsuario.OFICIAL_JUSTICA);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        when(contextFactory.build()).thenReturn(new PerfilDashboardContext(usuario, null, java.time.LocalDateTime.now(), "OFICIAL", "Sr.", java.util.List.of(), java.util.List.of(), null, null, null, null, java.util.List.of(), null));
        Processo processo = new Processo();
        processo.setNumeroProcesso("0001-77");
        WorkItem item = WorkItem.builder().id(1L).processo(processo).titulo("Mandado de Citação").type(WorkItemType.CITACAO).dueAt(Instant.now().plusSeconds(3600)).comarca("Fortaleza").build();
        when(commons.inboxHibrido(usuario, 100)).thenReturn(java.util.List.of(item));
        when(workItemRepository.findFilasComMaisDe(25L)).thenReturn(java.util.List.of("SECRETARIA_CUMPRIMENTO"));
        when(intelligenceSummaryService.resumir(Mockito.eq(usuario), Mockito.any(), Mockito.eq(6))).thenReturn(new com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoGovernanceMetricas(null, null, null, 0, 0, 0, 0, 0, 0, 0d, "SEM_DADOS", false, java.util.List.of(), java.util.List.of()));
        when(enderecoTriageService.painelResumo()).thenReturn(java.util.Map.of("enabled", true));

        var response = service.analyze(2, 25L);

        assertEquals(1, response.totalMandadosPendentes());
        assertEquals(1, response.totalCitacoes());
    }
}
