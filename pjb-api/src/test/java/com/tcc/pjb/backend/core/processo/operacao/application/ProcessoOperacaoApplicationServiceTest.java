package com.tcc.pjb.backend.core.processo.operacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.integracao.application.ProcessoIntegracaoApplicationService;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoAggregate;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoIdentity;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoApplicationService;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoAggregate;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoIdentity;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineIdentity;
import com.tcc.pjb.backend.model.dto.processo.ProcessoAcessoVisibilidadeResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.processo.ProcessoObservabilidadeAcessoService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoOperacaoApplicationServiceTest {

    @Mock
    private ProcessoRepository processoRepository;
    @Mock
    private ProcessoTimelineApplicationService processoTimelineApplicationService;
    @Mock
    private ProcessoIntegracaoApplicationService processoIntegracaoApplicationService;
    @Mock
    private ProcessoMigracaoApplicationService processoMigracaoApplicationService;
    @Mock
    private ProcessoObservabilidadeAcessoService processoObservabilidadeAcessoService;

    private ProcessoOperacaoApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ProcessoOperacaoApplicationService(
                processoRepository,
                processoTimelineApplicationService,
                processoIntegracaoApplicationService,
                processoMigracaoApplicationService,
                processoObservabilidadeAcessoService
        );
    }

    @Test
    void shouldBuildOperationalAggregateWithAlerts() {
        Processo processo = Processo.builder()
                .id(10L)
                .numeroProcesso("0001")
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .faseAtual(FaseProcessual.CONHECIMENTO)
                .statusProcesso(StatusProcesso.DISTRIBUIDO)
                .tribunalCodigoRoteado("TJCE")
                .unidadeJudiciariaCodigo("1VC")
                .build();
        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));
        when(processoTimelineApplicationService.detalhar(10L)).thenReturn(new ProcessoTimelineAggregate(
                new ProcessoTimelineIdentity(10L, "0001", "CIVIL", "COMUM_ORDINARIO", "CONHECIMENTO", "DISTRIBUIDO", "TJCE", "1VC", List.of()),
                5,
                3,
                1,
                List.of("PROCESSO"),
                List.of(),
                List.of(),
                List.of("DESOBSTRUIR"),
                List.of("bloqueio"),
                Instant.now()
        ));
        when(processoIntegracaoApplicationService.detalhar(10L)).thenReturn(new ProcessoIntegracaoAggregate(
                new ProcessoIntegracaoIdentity(10L, "0001", "TJCE", "1VC", "CIVIL", "COMUM_ORDINARIO", "PJE", List.of()),
                "PJE",
                "BLOCKED",
                "READY",
                List.of(),
                List.of(),
                List.of("REMOVER_BLOQUEIO"),
                List.of("Integração pendente"),
                Instant.now()
        ));
        when(processoMigracaoApplicationService.detalhar(10L)).thenReturn(new ProcessoMigracaoAggregate(
                new ProcessoMigracaoIdentity(10L, "0001", "TJCE", "PJE", List.of()),
                "PARCIAL",
                false,
                List.of(),
                List.of(),
                List.of("EVOLUIR_SHADOW"),
                List.of("Sem corte"),
                Instant.now()
        ));
        when(processoObservabilidadeAcessoService.resumir(processo)).thenReturn(new ProcessoAcessoVisibilidadeResponse(
                null,
                10L,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("sem leitura")
        ));

        var aggregate = service.detalhar(10L);

        assertThat(aggregate.readiness()).isEqualTo("NOT_READY");
        assertThat(aggregate.totalBloqueios()).isPositive();
        assertThat(aggregate.faixas()).hasSize(4);
        assertThat(aggregate.alertas()).isNotEmpty();
    }
}
