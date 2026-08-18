package com.tcc.pjb.backend.service.advogado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoPainelFinanceiroResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedProcessOperationService;
import com.tcc.pjb.backend.modules.custas.application.CustasApplicationService;
import com.tcc.pjb.backend.modules.custas.domain.CustaConsultaResult;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceContextualSearchService;
import com.tcc.pjb.backend.service.processual.honorarios.HonorariosSucumbenciaCalculatorService;
import com.tcc.pjb.backend.service.processual.legitimidade.OabValidationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdvogadoCockpitServicePainelFinanceiroTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final CustasApplicationService custasApplicationService = mock(CustasApplicationService.class);

    private final AdvogadoCockpitService service = new AdvogadoCockpitService(
            mock(PerfilDashboardContextFactory.class),
            mock(PainelServiceCommons.class),
            processoRepository,
            mock(WorkItemRepository.class),
            mock(PjbAuthorizationService.class),
            mock(OfficeGovernedProcessOperationService.class),
            new HonorariosSucumbenciaCalculatorService(),
            custasApplicationService,
            mock(OabValidationService.class),
            mock(JurisprudenceContextualSearchService.class));

    @Test
    void consolidaTotaisDeCustasPendentesEPagasIgnorandoStatusDesconhecido() {
        Processo processo = new Processo();
        processo.setId(80L);
        when(processoRepository.findById(80L)).thenReturn(Optional.of(processo));
        when(custasApplicationService.listarPorProcesso(80L)).thenReturn(List.of(
                new CustaConsultaResult(1L, "CUSTAS_INICIAIS", new BigDecimal("100.00"), "PENDENTE", LocalDate.now().plusDays(10), null, null),
                new CustaConsultaResult(2L, "PREPARO_RECURSAL", new BigDecimal("50.00"), "PENDENTE", LocalDate.now().plusDays(20), null, null),
                new CustaConsultaResult(3L, "CUSTAS_INICIAIS", new BigDecimal("30.00"), "PAGO", null, null, new BigDecimal("30.00")),
                new CustaConsultaResult(4L, "MULTA_LITIGANCIA_MA_FE", new BigDecimal("999.00"), "CANCELADA", null, null, null)
        ));

        AdvogadoPainelFinanceiroResponse resultado = service.consultarPainelFinanceiro(80L);

        assertThat(resultado.processoId()).isEqualTo(80L);
        assertThat(resultado.quantidadeCustas()).isEqualTo(4);
        assertThat(resultado.quantidadeCustasPendentes()).isEqualTo(2);
        assertThat(resultado.quantidadeCustasPagas()).isEqualTo(1);
        assertThat(resultado.totalCustasPendentes()).isEqualByComparingTo("150.00");
        assertThat(resultado.totalCustasPagas()).isEqualByComparingTo("30.00");
        assertThat(resultado.custas()).hasSize(4);
    }

    @Test
    void retornaTotaisZeradosQuandoProcessoNaoTemCustas() {
        Processo processo = new Processo();
        processo.setId(81L);
        when(processoRepository.findById(81L)).thenReturn(Optional.of(processo));
        when(custasApplicationService.listarPorProcesso(81L)).thenReturn(List.of());

        AdvogadoPainelFinanceiroResponse resultado = service.consultarPainelFinanceiro(81L);

        assertThat(resultado.quantidadeCustas()).isZero();
        assertThat(resultado.totalCustasPendentes()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.totalCustasPagas()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
