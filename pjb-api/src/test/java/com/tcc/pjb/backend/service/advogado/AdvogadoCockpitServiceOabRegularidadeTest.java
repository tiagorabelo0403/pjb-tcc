package com.tcc.pjb.backend.service.advogado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.integration.oab.OabValidationResult;
import com.tcc.pjb.backend.integration.oab.OabValidationStatus;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoOabRegularidadeResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedProcessOperationService;
import com.tcc.pjb.backend.modules.custas.application.CustasApplicationService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.processual.honorarios.HonorariosSucumbenciaCalculatorService;
import com.tcc.pjb.backend.service.processual.legitimidade.OabValidationService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdvogadoCockpitServiceOabRegularidadeTest {

    private final PerfilDashboardContextFactory contextFactory = mock(PerfilDashboardContextFactory.class);
    private final PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
    private final OabValidationService oabValidationService = mock(OabValidationService.class);

    private final AdvogadoCockpitService service = new AdvogadoCockpitService(
            contextFactory,
            mock(PainelServiceCommons.class),
            mock(ProcessoRepository.class),
            mock(WorkItemRepository.class),
            authorizationService,
            mock(OfficeGovernedProcessOperationService.class),
            new HonorariosSucumbenciaCalculatorService(),
            mock(CustasApplicationService.class),
            oabValidationService,
            mock(com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceContextualSearchService.class));

    @Test
    void retornaRegularidadeOabDoAdvogadoAutenticadoAposCheckarPapel() {
        Usuario advogado = new Usuario();
        advogado.setId(5L);
        PerfilDashboardContext ctx = new PerfilDashboardContext(
                advogado, null, LocalDateTime.now(), null, null,
                List.of(), List.of(), null, null, null, null, List.of(), null);
        when(contextFactory.build()).thenReturn(ctx);
        when(oabValidationService.consultarRegularidade(advogado))
                .thenReturn(OabValidationResult.apto("oab-cna"));

        AdvogadoOabRegularidadeResponse response = service.consultarRegularidadeOab();

        assertThat(response.status()).isEqualTo(OabValidationStatus.APTO.name());
        assertThat(response.reasonCode()).isEqualTo("OAB_APTA");
        assertThat(response.source()).isEqualTo("oab-cna");
    }
}
