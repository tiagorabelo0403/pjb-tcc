package com.tcc.pjb.backend.core.processo.painel.application;

import com.tcc.pjb.backend.core.processo.analytics.application.ProcessoAnalyticsNacionalApplicationService;
import com.tcc.pjb.backend.core.processo.analytics.domain.ProcessoAnalyticsNacionalAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelBndtAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelFonteOficialAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelFonteOficialItem;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoOperacaoTransversalApplicationService;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoOperacaoTransversalAggregate;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPainelBndtApplicationService {

    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    private final ProcessoTimelineApplicationService processoTimelineApplicationService;
    private final ProcessoAnalyticsNacionalApplicationService processoAnalyticsNacionalApplicationService;
    private final ProcessoOperacaoTransversalApplicationService processoOperacaoTransversalApplicationService;
    private final ProcessoPainelFonteOficialApplicationService processoPainelFonteOficialApplicationService;

    public ProcessoPainelBndtApplicationService(ProcessoUnificadoApplicationService processoUnificadoApplicationService,
                                                ProcessoTimelineApplicationService processoTimelineApplicationService,
                                                ProcessoAnalyticsNacionalApplicationService processoAnalyticsNacionalApplicationService,
                                                ProcessoOperacaoTransversalApplicationService processoOperacaoTransversalApplicationService,
                                                ProcessoPainelFonteOficialApplicationService processoPainelFonteOficialApplicationService) {
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
        this.processoTimelineApplicationService = Objects.requireNonNull(processoTimelineApplicationService);
        this.processoAnalyticsNacionalApplicationService = Objects.requireNonNull(processoAnalyticsNacionalApplicationService);
        this.processoOperacaoTransversalApplicationService = Objects.requireNonNull(processoOperacaoTransversalApplicationService);
        this.processoPainelFonteOficialApplicationService = Objects.requireNonNull(processoPainelFonteOficialApplicationService);
    }

    public ProcessoPainelBndtAggregate detalhar(Long processoId) {
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        ProcessoTimelineAggregate timeline = processoTimelineApplicationService.detalhar(processoId);
        ProcessoAnalyticsNacionalAggregate analytics = processoAnalyticsNacionalApplicationService.detalhar(processoId);
        ProcessoOperacaoTransversalAggregate operacao = processoOperacaoTransversalApplicationService.detalhar(processoId);
        ProcessoPainelFonteOficialAggregate fontes = processoPainelFonteOficialApplicationService.detalhar(processoId);
        boolean aplicavel = normalize(unificado.competencia().ramoDireito()).contains("TRABALH");
        ProcessoPainelFonteOficialItem fonte = fontes.itens().stream().filter(item -> "BNDT_ATIVA".equals(item.widgetCode())).findFirst().orElse(null);

        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        LinkedHashSet<String> passos = new LinkedHashSet<>();
        String status;
        boolean realtime;
        if (!aplicavel) {
            status = "NAO_APLICAVEL";
            realtime = false;
            alertas.add("RAMO_NAO_TRABALHISTA_PARA_BNDT");
        } else {
            realtime = operacao.coberturaGlobal() >= 70d && analytics.riscoSlaGlobal() < 80d;
            if (timeline.totalBloqueantes() > 0) {
                status = "BLOQUEADO";
                alertas.add("TRILHA_EXECUTIVA_COM_BLOQUEIOS_ANTES_DA_CONSULTA_BNDT");
            } else if (realtime) {
                status = "PRONTA";
            } else {
                status = "PARCIAL";
                alertas.add("BNDT_REQUER_ENDURECIMENTO_OPERACIONAL_E_REPLAY_CONTROLADO");
            }
            if (analytics.taxaRetrabalho() > 20d) {
                alertas.add("TAXA_RETRABALHO_ELEVADA_NA_EXECUCAO_TRABALHISTA");
            }
            passos.addAll(unificado.proximoMelhorAto());
            passos.addAll(timeline.proximoCiclo());
            if (passos.isEmpty()) {
                passos.add("SINCRONIZAR_EXECUCAO_TRABALHISTA_E_CONSULTA_BNDT");
            }
        }
        return new ProcessoPainelBndtAggregate(
                processoId,
                unificado.identity().numeroProcesso(),
                aplicavel,
                status,
                realtime,
                fonte == null ? "BNDT" : String.join(", ", fonte.officialSources()),
                fonte == null ? "REPLAY_CONTROLADO" : fonte.fallbackMode(),
                List.copyOf(alertas),
                passos.stream().limit(6).toList(),
                Instant.now()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
