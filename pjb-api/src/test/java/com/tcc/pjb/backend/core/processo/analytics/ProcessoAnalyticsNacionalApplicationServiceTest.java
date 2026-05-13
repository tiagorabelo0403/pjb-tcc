package com.tcc.pjb.backend.core.processo.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.analytics.application.ProcessoAnalyticsNacionalApplicationService;
import com.tcc.pjb.backend.core.processo.busca.application.ProcessoBuscaAnalyticsApplicationService;
import com.tcc.pjb.backend.core.processo.busca.domain.ProcessoAnalyticsAggregate;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class ProcessoAnalyticsNacionalApplicationServiceTest {

    @Test
    void deveGerarIndicadoresNacionaisDerivadosDoProcessoBase() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        ProcessoBuscaAnalyticsApplicationService busca = mock(ProcessoBuscaAnalyticsApplicationService.class);
        Processo base = Processo.builder().id(30L).numeroProcesso("0030").tribunalCodigoRoteado("TJCE").ramoDireito(RamoDireito.CIVIL).rito(RitoProcessual.COMUM_ORDINARIO).faseAtual(FaseProcessual.CONHECIMENTO).statusProcesso(StatusProcesso.DISTRIBUIDO).dataCriacao(LocalDateTime.now().minusDays(90)).build();
        when(processoRepository.findById(30L)).thenReturn(Optional.of(base));
        when(busca.analytics("CIVIL", "TJCE", null, null)).thenReturn(new ProcessoAnalyticsAggregate(Map.of("ramo", "CIVIL"), 10L, 8L, 33d, 12d, 6d, 4d, List.of(), List.of(), Instant.now()));
        when(processoRepository.findAll(PageRequest.of(0, 600, Sort.by(Sort.Direction.DESC, "dataUltimaMovimentacao", "id")))).thenReturn(new PageImpl<>(List.of(base)));
        ProcessoAnalyticsNacionalApplicationService service = new ProcessoAnalyticsNacionalApplicationService(processoRepository, busca);

        var aggregate = service.detalhar(30L);

        assertThat(aggregate.baseline().totalProcessos()).isEqualTo(10L);
        assertThat(aggregate.recorte()).containsEntry("processoId", "30");
        assertThat(aggregate.unidadesCriticas()).isNotEmpty();
    }
}
