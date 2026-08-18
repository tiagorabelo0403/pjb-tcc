package com.tcc.pjb.backend.service.advogado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoProdutividadeEscritorioResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedProcessOperationService;
import com.tcc.pjb.backend.modules.custas.application.CustasApplicationService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceContextualSearchService;
import com.tcc.pjb.backend.service.processual.honorarios.HonorariosSucumbenciaCalculatorService;
import com.tcc.pjb.backend.service.processual.legitimidade.OabValidationService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

class AdvogadoCockpitServiceProdutividadeTest {

    private final PerfilDashboardContextFactory contextFactory = mock(PerfilDashboardContextFactory.class);
    private final PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);

    private final AdvogadoCockpitService service = new AdvogadoCockpitService(
            contextFactory,
            mock(PainelServiceCommons.class),
            processoRepository,
            mock(WorkItemRepository.class),
            authorizationService,
            mock(OfficeGovernedProcessOperationService.class),
            new HonorariosSucumbenciaCalculatorService(),
            mock(CustasApplicationService.class),
            mock(OabValidationService.class),
            mock(JurisprudenceContextualSearchService.class));

    private Processo processo(StatusProcesso status, RitoProcessual rito, LocalDateTime distribuicao, LocalDateTime ultimaMovimentacao) {
        Processo p = new Processo();
        p.setStatusProcesso(status);
        p.setRito(rito);
        p.setDataDistribuicao(distribuicao);
        p.setDataUltimaMovimentacao(ultimaMovimentacao);
        return p;
    }

    @Test
    void consolidaCarteiraPorStatusRitoEDuracaoMediaDosEncerrados() {
        Usuario advogado = new Usuario();
        advogado.setId(1L);
        advogado.setCpf("11122233344");
        PerfilDashboardContext ctx = new PerfilDashboardContext(
                advogado, null, LocalDateTime.now(), null, null,
                List.of(), List.of(), null, null, null, null, List.of(), null);
        when(contextFactory.build()).thenReturn(ctx);

        Processo ativo = processo(StatusProcesso.EM_ANDAMENTO, RitoProcessual.COMUM_ORDINARIO, LocalDateTime.now().minusDays(30), null);
        Processo encerrado1 = processo(StatusProcesso.ARQUIVADO, RitoProcessual.COMUM_ORDINARIO,
                LocalDateTime.now().minusDays(100), LocalDateTime.now().minusDays(20));
        Processo encerrado2 = processo(StatusProcesso.TRANSITO_EM_JULGADO, RitoProcessual.JUIZADO_ESPECIAL_CIVEL,
                LocalDateTime.now().minusDays(60), LocalDateTime.now().minusDays(20));
        Page<Processo> pagina = new PageImpl<>(List.of(ativo, encerrado1, encerrado2));
        when(processoRepository.findByAdvogadoCpf(eq("11122233344"), any())).thenReturn(pagina);

        AdvogadoProdutividadeEscritorioResponse resultado = service.consultarProdutividadeEscritorio();

        assertThat(resultado.totalProcessos()).isEqualTo(3);
        assertThat(resultado.processosAtivos()).isEqualTo(1);
        assertThat(resultado.processosEncerrados()).isEqualTo(2);
        assertThat(resultado.distribuicaoPorStatus()).containsEntry("EM_ANDAMENTO", 1L);
        assertThat(resultado.distribuicaoPorStatus()).containsEntry("ARQUIVADO", 1L);
        assertThat(resultado.distribuicaoPorRito()).containsEntry("COMUM_ORDINARIO", 2L);
        assertThat(resultado.duracaoMediaDiasProcessosEncerrados()).isEqualTo(60.0);
    }

    @Test
    void retornaDuracaoMediaNulaQuandoNenhumProcessoEncerradoTemDatasValidas() {
        Usuario advogado = new Usuario();
        advogado.setId(2L);
        advogado.setCpf("55566677788");
        PerfilDashboardContext ctx = new PerfilDashboardContext(
                advogado, null, LocalDateTime.now(), null, null,
                List.of(), List.of(), null, null, null, null, List.of(), null);
        when(contextFactory.build()).thenReturn(ctx);
        when(processoRepository.findByAdvogadoCpf(eq("55566677788"), any())).thenReturn(new PageImpl<>(List.of()));

        AdvogadoProdutividadeEscritorioResponse resultado = service.consultarProdutividadeEscritorio();

        assertThat(resultado.totalProcessos()).isZero();
        assertThat(resultado.duracaoMediaDiasProcessosEncerrados()).isNull();
    }
}
