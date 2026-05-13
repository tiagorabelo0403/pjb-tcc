package com.tcc.pjb.backend.core.processo.migracao;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.processo.integracao.application.ProcessoIntegracaoApplicationService;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoAggregate;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoCanal;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoEvento;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoIdentity;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineEvento;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineIdentity;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelinePendencia;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoMigracaoApplicationServiceTest {

    @Mock
    private ProcessoRepository processoRepository;
    @Mock
    private ProcessoIntegracaoApplicationService processoIntegracaoApplicationService;
    @Mock
    private ProcessoTimelineApplicationService processoTimelineApplicationService;
    @Mock
    private ProcessoUnificadoApplicationService processoUnificadoApplicationService;

    @Test
    void deveSinalizarReadyForCutoverQuandoNaoHaBloqueios() {
        Processo processo = Processo.builder()
                .id(801L)
                .numeroProcesso("0000801-99.2026.8.06.0001")
                .tribunalCodigoRoteado("TJCE")
                .unidadeJudiciariaCodigo("1VCRIM")
                .ramoDireito(RamoDireito.PENAL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .faseAtual(FaseProcessual.CONHECIMENTO)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .connectorSystem("PJE")
                .build();
        when(processoRepository.findById(801L)).thenReturn(Optional.of(processo));
        when(processoIntegracaoApplicationService.detalhar(801L)).thenReturn(new ProcessoIntegracaoAggregate(
                new ProcessoIntegracaoIdentity(801L, "0000801-99.2026.8.06.0001", "TJCE", "1VCRIM", "PENAL", "COMUM_ORDINARIO", "PJE", List.of("PENAL")),
                "PJE",
                "READY",
                "READY",
                List.of(new ProcessoIntegracaoCanal("CANAL", "Canal", "PJE", true, true, "BEARER", false, true, true, List.of(), List.of(), java.util.Map.of())),
                List.of(new ProcessoIntegracaoEvento("ROTEAMENTO", "Roteamento", "ROUTING", "READY", Instant.now(), "801", List.of())),
                List.of(),
                List.of(),
                Instant.now()
        ));
        when(processoTimelineApplicationService.detalhar(801L)).thenReturn(new ProcessoTimelineAggregate(
                new ProcessoTimelineIdentity(801L, "0000801-99.2026.8.06.0001", "PENAL", "COMUM_ORDINARIO", "CONHECIMENTO", "EM_ANDAMENTO", "TJCE", "1VCRIM", List.of("PENAL")),
                2,
                0,
                0,
                List.of("PROCESSO"),
                List.of(new ProcessoTimelineEvento("CRIACAO", "Criação", "PROCESSO", 10, Instant.now(), true, false, "PROTOCOLO", List.of(), List.of())),
                List.of(),
                List.of("PROXIMO_ATO"),
                List.of(),
                Instant.now()
        ));
        when(processoUnificadoApplicationService.detalhar(801L)).thenReturn(new ProcessoUnificadoAggregate(
                new com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity(801L, "0000801-99.2026.8.06.0001", "0000801-99.2026.8.06.0001", "TJCE", "CE", "Fortaleza", "1VCRIM", "ACAO PENAL", "CRIME", "MP", "ACUSADO", List.of("PENAL")),
                new ProcessoUnificadoCompetencia("ESTADUAL", "PRIMEIRO_GRAU", "PENAL", "COMUM_ORDINARIO", "CONHECIMENTO", "EM_ANDAMENTO", "TJCE", "Tribunal", "Juízo", "1VCRIM", "DISTRIBUICAO", "TRIAGEM", "FORO", "NAO", "SORTEIO", "PENAL", "PADRAO", "NAO", "ENVELOPE", "LOW", "SECRETARIA", false, false, 24, List.of(), List.of("fundamento"), List.of("check"), new LinkedHashMap<>()),
                new ProcessoUnificadoDiagnostico(true, 0, 0, 3, 0, 0, 0, List.of(), List.of("fundamento"), Instant.now()),
                List.of(),
                List.of(),
                List.of("PROXIMO_ATO"),
                Instant.now()
        ));

        ProcessoMigracaoApplicationService service = new ProcessoMigracaoApplicationService(
                processoRepository,
                processoIntegracaoApplicationService,
                processoTimelineApplicationService,
                processoUnificadoApplicationService
        );

        var aggregate = service.detalhar(801L);

        assertThat(aggregate.readiness()).isEqualTo("READY_FOR_CUTOVER");
        assertThat(aggregate.canCutOver()).isTrue();
        assertThat(aggregate.mirrors()).hasSize(2);
    }
}
