package com.tcc.pjb.backend.core.processo.busca.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ProcessoBuscaAnalyticsApplicationServiceTest {

    @Mock
    private ProcessoRepository processoRepository;

    private ProcessoBuscaAnalyticsApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ProcessoBuscaAnalyticsApplicationService(processoRepository);
    }

    @Test
    void shouldReturnSearchCardsAndFacets() {
        Processo processo = Processo.builder()
                .id(20L)
                .numeroProcesso("0002")
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .faseAtual(FaseProcessual.CONHECIMENTO)
                .statusProcesso(StatusProcesso.DISTRIBUIDO)
                .tribunalCodigoRoteado("TJCE")
                .unidadeJudiciariaCodigo("2VC")
                .build();
        when(processoRepository.searchQuick(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(processo), PageRequest.of(0, 25), 1));

        var aggregate = service.buscar(null, null, "0002", null, null, null, null, null, 0, 25);

        assertThat(aggregate.resultados()).hasSize(1);
        assertThat(aggregate.facets()).isNotEmpty();
    }

    @Test
    void shouldBuildAnalyticsFromRamoQuery() {
        when(processoRepository.agregadosPorRamo("CIVIL")).thenReturn(List.<Object[]>of(new Object[]{"CIVIL", 10L, 4L, 2L, 6L, 12.5d}));

        var aggregate = service.analytics("civil", null, null, null);

        assertThat(aggregate.totalAtivos()).isEqualTo(6L);
        assertThat(aggregate.taxaRecursal()).isEqualTo(20.0d);
        assertThat(aggregate.indicadores()).isNotEmpty();
    }
}
