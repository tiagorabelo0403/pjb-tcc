package com.tcc.pjb.backend.core.processo.painel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.analytics.application.ProcessoAnalyticsNacionalApplicationService;
import com.tcc.pjb.backend.core.processo.analytics.domain.ProcessoAnalyticsNacionalAggregate;
import com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoIdentity;
import com.tcc.pjb.backend.core.processo.painel.application.ProcessoPainelContextualApplicationService;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelContextualAggregate;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoOperacaoTransversalApplicationService;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoOperacaoTransversalAggregate;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineIdentity;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorObservabilityReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorObservabilitySystemReport;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.judicial.connectors.application.JudicialConnectorHubService;
import com.tcc.pjb.backend.judicial.connectors.domain.JudicialConnectorHubReport;
import com.tcc.pjb.backend.judicial.connectors.domain.JudicialConnectorStructureReport;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoPainelContextualApplicationServiceTest {

    @Mock ProcessoUnificadoApplicationService unificadoService;
    @Mock ProcessoTimelineApplicationService timelineService;
    @Mock ProcessoAnalyticsNacionalApplicationService analyticsService;
    @Mock ProcessoOperacaoTransversalApplicationService operacaoService;
    @Mock ProcessoExecucaoApplicationService execucaoService;
    @Mock JudicialConnectorHubService hubService;

    @Test
    void deveResolverPainelPenalEExporConectores() {
        when(unificadoService.detalhar(7L)).thenReturn(new ProcessoUnificadoAggregate(
                new ProcessoUnificadoIdentity(7L, "0001", "0001", "TJCE", "CE", "Fortaleza", "1V", "AÇÃO PENAL", "assunto", "MP", "Réu", List.of("PENAL")),
                new ProcessoUnificadoCompetencia("ESTADUAL", "PRIMEIRO_GRAU", "PENAL", "COMUM", "INSTRUCAO", "ATIVO", "TJCE", "Tribunal", "Vara", "Unidade", "Fila", "Mesa", "TERRITORIAL", "PREVENCAO", "DISTRIBUICAO", "PENAL", "PADRAO", "LINK", "ENV", "MEDIO", "JUIZ", false, false, 24, List.of("fundamento_competencia"), List.of("fundamento_competencia"), List.of("check"), new LinkedHashMap<>()),
                new ProcessoUnificadoDiagnostico(true, 0, 0, 0, 0, 0, 0, List.of(), List.of("ok"), Instant.now()),
                List.of(),
                List.of(),
                List.of("AUDIENCIA_CUSTODIA"),
                Instant.now()
        ));
        when(timelineService.detalhar(7L)).thenReturn(new ProcessoTimelineAggregate(
                new ProcessoTimelineIdentity(7L, "0001", "PENAL", "COMUM", "INSTRUCAO", "ATIVO", "TJCE", "1V", List.of("PENAL")),
                2,
                3,
                1,
                List.of("PRAZO"),
                List.of(),
                List.of(),
                List.of("AUDIENCIA_CUSTODIA"),
                List.of("Prazo penal critico"),
                Instant.now()
        ));
        when(analyticsService.detalhar(7L)).thenReturn(new ProcessoAnalyticsNacionalAggregate(7L, Map.of(), null, 22d, 10d, 1L, 65d, List.of(), List.of(), List.of("Risco elevado"), Instant.now()));
        when(operacaoService.detalhar(7L)).thenReturn(new ProcessoOperacaoTransversalAggregate(7L, "0001", "READY", 88d, 21d, List.of(), List.of("retry_ok"), List.of("seguir"), Instant.now()));
        when(execucaoService.detalhar(7L)).thenReturn(new ProcessoExecucaoAggregate(new ProcessoExecucaoIdentity(7L, "0001", "TJCE", "PENAL", "COMUM", "INSTRUCAO", "ATIVO", List.of("PENAL")), true, 2, 1, 4, 2, List.of(), List.of("mandado_ativo"), List.of("cumprir_mandado"), Instant.now()));
        when(hubService.tribunalReport(org.mockito.ArgumentMatchers.eq("TJCE"), org.mockito.ArgumentMatchers.any())).thenReturn(new JudicialConnectorHubReport(
                Instant.now(),
                "TJCE",
                new JudicialConnectorStructureReport(Instant.now(), "com.tcc.pjb.backend.integration", List.of("legacy"), List.of(), List.of("entry"), Map.of()),
                new JudicialConnectorCommandCenterReport(
                        Instant.now(),
                        "TJCE",
                        null,
                        null,
                        null,
                        null,
                        new JudicialConnectorObservabilityReport(Instant.now(), "TJCE", Instant.now(), 1, 0, 0, List.of(new JudicialConnectorObservabilitySystemReport(Instant.now(), JudicialSystem.PJE, "TJCE", "HEALTHY", true, true, true, true, true, 10L, 99.0d, Instant.now(), List.of(), List.of(), Map.of())), List.of(), Map.of()),
                        null,
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                null,
                List.of(),
                Map.of()
        ));

        ProcessoPainelContextualApplicationService service = new ProcessoPainelContextualApplicationService(unificadoService, timelineService, analyticsService, operacaoService, execucaoService, hubService);
        ProcessoPainelContextualAggregate aggregate = service.detalhar(7L, "JUIZ_PENAL");

        assertThat(aggregate.painelCodigo()).isEqualTo("PAINEL_PENAL");
        assertThat(aggregate.widgets()).extracting("code").contains("RADAR_REUS_PRESOS", "PRESCRICAO_ATIVA");
        assertThat(aggregate.conectores()).hasSize(1);
    }
}
