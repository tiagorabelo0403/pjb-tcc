package com.tcc.pjb.backend.core.processo.painel.application;

import com.tcc.pjb.backend.core.processo.analytics.application.ProcessoAnalyticsNacionalApplicationService;
import com.tcc.pjb.backend.core.processo.analytics.domain.ProcessoAnalyticsNacionalAggregate;
import com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelFonteOficialAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelFonteOficialItem;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelPrevidenciarioFonte;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelPrevidenciarioTrilhoAggregate;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoOperacaoTransversalApplicationService;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoOperacaoTransversalAggregate;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.service.financeiro.previdenciario.CnisAnalyzer;
import com.tcc.pjb.backend.service.financeiro.previdenciario.CnisResultado;
import com.tcc.pjb.backend.service.financeiro.previdenciario.CnisVinculo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPainelPrevidenciarioTrilhoApplicationService {

    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    private final ProcessoTimelineApplicationService processoTimelineApplicationService;
    private final ProcessoAnalyticsNacionalApplicationService processoAnalyticsNacionalApplicationService;
    private final ProcessoOperacaoTransversalApplicationService processoOperacaoTransversalApplicationService;
    private final ProcessoExecucaoApplicationService processoExecucaoApplicationService;
    private final ProcessoPainelFonteOficialApplicationService processoPainelFonteOficialApplicationService;
    private final CnisAnalyzer cnisAnalyzer;

    public ProcessoPainelPrevidenciarioTrilhoApplicationService(ProcessoUnificadoApplicationService processoUnificadoApplicationService,
                                                                ProcessoTimelineApplicationService processoTimelineApplicationService,
                                                                ProcessoAnalyticsNacionalApplicationService processoAnalyticsNacionalApplicationService,
                                                                ProcessoOperacaoTransversalApplicationService processoOperacaoTransversalApplicationService,
                                                                ProcessoExecucaoApplicationService processoExecucaoApplicationService,
                                                                ProcessoPainelFonteOficialApplicationService processoPainelFonteOficialApplicationService,
                                                                CnisAnalyzer cnisAnalyzer) {
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
        this.processoTimelineApplicationService = Objects.requireNonNull(processoTimelineApplicationService);
        this.processoAnalyticsNacionalApplicationService = Objects.requireNonNull(processoAnalyticsNacionalApplicationService);
        this.processoOperacaoTransversalApplicationService = Objects.requireNonNull(processoOperacaoTransversalApplicationService);
        this.processoExecucaoApplicationService = Objects.requireNonNull(processoExecucaoApplicationService);
        this.processoPainelFonteOficialApplicationService = Objects.requireNonNull(processoPainelFonteOficialApplicationService);
        this.cnisAnalyzer = Objects.requireNonNull(cnisAnalyzer);
    }

    public ProcessoPainelPrevidenciarioTrilhoAggregate detalhar(Long processoId) {
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        ProcessoTimelineAggregate timeline = processoTimelineApplicationService.detalhar(processoId);
        ProcessoAnalyticsNacionalAggregate analytics = processoAnalyticsNacionalApplicationService.detalhar(processoId);
        ProcessoOperacaoTransversalAggregate operacao = processoOperacaoTransversalApplicationService.detalhar(processoId);
        ProcessoExecucaoAggregate execucao = processoExecucaoApplicationService.detalhar(processoId);
        ProcessoPainelFonteOficialAggregate fontesOficiais = processoPainelFonteOficialApplicationService.detalhar(processoId);
        boolean aplicavel = isPrevidenciario(unificado);

        if (!aplicavel) {
            return new ProcessoPainelPrevidenciarioTrilhoAggregate(
                    processoId,
                    unificado.identity().numeroProcesso(),
                    false,
                    "NAO_APLICAVEL",
                    "CNIS não aplicável ao ramo atual",
                    "NAO_APLICAVEL",
                    "NAO_APLICAVEL",
                    List.of(),
                    List.of("RAMO_NAO_PREVIDENCIARIO"),
                    List.of(),
                    Instant.now()
            );
        }

        CnisResultado cnis = cnisAnalyzer.analisar(List.of(CnisVinculo.builder()
                .empregador("INSS")
                .mesesSemContribuicao(timeline.totalPendencias() > 0 || analytics.riscoSlaGlobal() > 75d ? 4 : 0)
                .build()));

        ProcessoPainelFonteOficialItem baseFonte = fontesOficiais.itens().stream()
                .filter(item -> "TRILHO_INSS_CNIS".equals(item.widgetCode()))
                .findFirst()
                .orElse(null);
        String fallback = baseFonte == null ? "ULTIMO_ESTADO_CACHE" : baseFonte.fallbackMode();
        ArrayList<ProcessoPainelPrevidenciarioFonte> fontes = new ArrayList<>();
        fontes.add(new ProcessoPainelPrevidenciarioFonte("CNIS", "Trilho CNIS", operacao.coberturaGlobal() >= 70d ? "PRONTO" : "PARCIAL", operacao.coberturaGlobal() >= 70d, fallback, cnis.getRecomendacao()));
        fontes.add(new ProcessoPainelPrevidenciarioFonte("SABI", "Trilho SABI", requiresPericia(unificado) && timeline.totalPendencias() > 0 ? "ATRASADO" : "PRONTO", !requiresPericia(unificado) || timeline.totalPendencias() == 0, fallback, requiresPericia(unificado) ? "Controle pericial vinculado" : "Sem incapacidade médica dominante"));
        fontes.add(new ProcessoPainelPrevidenciarioFonte("PLENUS", "Trilho PLENUS", execucao.totalTrilhas() > 0 ? "ATIVO" : "AGUARDANDO", execucao.totalTrilhas() > 0, fallback, execucao.totalTrilhas() > 0 ? "Ciclo de pagamento habilitado" : "Pagamento depende de fase executiva"));

        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        alertas.addAll(timeline.alertas());
        alertas.addAll(analytics.alertas());
        if (cnis.isPossuiLacunas()) {
            alertas.add("CNIS_COM_LACUNAS_RELEVANTES");
        }
        String filaPericiaStatus = requiresPericia(unificado)
                ? timeline.totalPendencias() > 0 ? "COM_ATRASO" : "PRONTA"
                : "NAO_CRITICA";
        String pagamentoStatus = execucao.totalTrilhas() > 0 ? "EM_ESTEIRA_RPV_PRECATORIO" : "AGUARDANDO_FASE_EXECUTIVA";
        String statusGeral = operacao.coberturaGlobal() >= 75d && !cnis.isPossuiLacunas() ? "PRONTO" : "PARCIAL";
        LinkedHashSet<String> passos = new LinkedHashSet<>();
        passos.addAll(unificado.proximoMelhorAto());
        passos.addAll(execucao.proximoMelhorPasso());
        passos.addAll(timeline.proximoCiclo());
        return new ProcessoPainelPrevidenciarioTrilhoAggregate(
                processoId,
                unificado.identity().numeroProcesso(),
                true,
                statusGeral,
                cnis.getRecomendacao(),
                filaPericiaStatus,
                pagamentoStatus,
                List.copyOf(fontes),
                alertas.stream().limit(8).toList(),
                passos.stream().limit(8).toList(),
                Instant.now()
        );
    }

    private boolean isPrevidenciario(ProcessoUnificadoAggregate unificado) {
        String ramo = normalize(unificado.competencia().ramoDireito());
        if (ramo.contains("PREVID")) {
            return true;
        }
        String assunto = normalize(unificado.identity().assunto());
        return assunto.contains("INSS") || assunto.contains("BPC") || assunto.contains("LOAS") || assunto.contains("BENEF");
    }

    private boolean requiresPericia(ProcessoUnificadoAggregate unificado) {
        String assunto = normalize(unificado.identity().assunto());
        String classe = normalize(unificado.identity().classeProcessual());
        return assunto.contains("INCAPAC") || assunto.contains("PERIC") || classe.contains("AUXILIO") || classe.contains("APOSENT");
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
