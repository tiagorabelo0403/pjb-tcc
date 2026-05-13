package com.tcc.pjb.backend.core.processo.painel.application;

import com.tcc.pjb.backend.core.processo.analytics.application.ProcessoAnalyticsNacionalApplicationService;
import com.tcc.pjb.backend.core.processo.analytics.domain.ProcessoAnalyticsNacionalAggregate;
import com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelConectorSaude;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelContextualAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelContextualWidget;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoOperacaoTransversalApplicationService;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoOperacaoTransversalAggregate;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorObservabilityReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorObservabilitySystemReport;
import com.tcc.pjb.backend.judicial.connectors.application.JudicialConnectorHubService;
import com.tcc.pjb.backend.judicial.connectors.domain.JudicialConnectorHubReport;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPainelContextualApplicationService {

    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    private final ProcessoTimelineApplicationService processoTimelineApplicationService;
    private final ProcessoAnalyticsNacionalApplicationService processoAnalyticsNacionalApplicationService;
    private final ProcessoOperacaoTransversalApplicationService processoOperacaoTransversalApplicationService;
    private final ProcessoExecucaoApplicationService processoExecucaoApplicationService;
    private final JudicialConnectorHubService judicialConnectorHubService;

    public ProcessoPainelContextualApplicationService(ProcessoUnificadoApplicationService processoUnificadoApplicationService,
                                                      ProcessoTimelineApplicationService processoTimelineApplicationService,
                                                      ProcessoAnalyticsNacionalApplicationService processoAnalyticsNacionalApplicationService,
                                                      ProcessoOperacaoTransversalApplicationService processoOperacaoTransversalApplicationService,
                                                      ProcessoExecucaoApplicationService processoExecucaoApplicationService,
                                                      JudicialConnectorHubService judicialConnectorHubService) {
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
        this.processoTimelineApplicationService = Objects.requireNonNull(processoTimelineApplicationService);
        this.processoAnalyticsNacionalApplicationService = Objects.requireNonNull(processoAnalyticsNacionalApplicationService);
        this.processoOperacaoTransversalApplicationService = Objects.requireNonNull(processoOperacaoTransversalApplicationService);
        this.processoExecucaoApplicationService = Objects.requireNonNull(processoExecucaoApplicationService);
        this.judicialConnectorHubService = Objects.requireNonNull(judicialConnectorHubService);
    }

    public ProcessoPainelContextualAggregate detalhar(Long processoId, String profileCode) {
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        ProcessoTimelineAggregate timeline = processoTimelineApplicationService.detalhar(processoId);
        ProcessoAnalyticsNacionalAggregate analytics = processoAnalyticsNacionalApplicationService.detalhar(processoId);
        ProcessoOperacaoTransversalAggregate operacao = processoOperacaoTransversalApplicationService.detalhar(processoId);
        ProcessoExecucaoAggregate execucao = processoExecucaoApplicationService.detalhar(processoId);
        String ramo = normalizeAxis(unificado.competencia().ramoDireito());
        PainelDescriptor painel = resolvePanel(ramo, unificado.competencia().ritoProcessual());
        JudicialConnectorHubReport hubReport = hasText(unificado.competencia().tribunalCodigo())
                ? judicialConnectorHubService.tribunalReport(unificado.competencia().tribunalCodigo(), Duration.ofHours(24))
                : judicialConnectorHubService.nationalReport(Duration.ofHours(24));

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("painel_contextual_por_ramo_e_rito");
        fundamentos.add("telemetria_guiada_por_contexto");
        if (profileCode != null && !profileCode.isBlank()) {
            fundamentos.add("perfil=" + profileCode.trim().toUpperCase(Locale.ROOT));
        }
        fundamentos.addAll(unificado.competencia().fundamentos());
        fundamentos.addAll(timeline.alertas());
        fundamentos.addAll(analytics.alertas());
        fundamentos.addAll(operacao.alertas());

        return new ProcessoPainelContextualAggregate(
                processoId,
                unificado.identity().numeroProcesso(),
                ramo,
                painel.code(),
                painel.title(),
                profileCode,
                buildWidgets(painel, unificado, timeline, analytics, operacao, execucao),
                buildConnectors(hubReport),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private List<ProcessoPainelContextualWidget> buildWidgets(PainelDescriptor painel,
                                                              ProcessoUnificadoAggregate unificado,
                                                              ProcessoTimelineAggregate timeline,
                                                              ProcessoAnalyticsNacionalAggregate analytics,
                                                              ProcessoOperacaoTransversalAggregate operacao,
                                                              ProcessoExecucaoAggregate execucao) {
        ArrayList<ProcessoPainelContextualWidget> out = new ArrayList<>();
        out.add(widget(
                "TIMELINE_VIVA",
                "Linha do tempo viva",
                "timeline",
                timeline.totalBloqueantes() > 0 ? "ALTO" : "ESTAVEL",
                timeline.totalBloqueantes() > 0 ? "#DC2626" : "#2563EB",
                timeline.totalPendencias() + " pendência(s)",
                "Mostra o que aconteceu, o que trava o fluxo e o próximo melhor ato.",
                mergeInsights(timeline.alertas(), timeline.proximoCiclo()),
                "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/timeline"
        ));
        out.add(widget(
                "ROTA_TATICA",
                "Rota tática",
                "tactical",
                timeline.totalBloqueantes() > 0 || analytics.riscoSlaGlobal() >= 70d ? "ALTO" : "ESTAVEL",
                timeline.totalBloqueantes() > 0 || analytics.riscoSlaGlobal() >= 70d ? "#DC2626" : "#2563EB",
                timeline.totalBloqueantes() + " trava(s) / " + percent(analytics.riscoSlaGlobal()),
                "Prioriza o próximo movimento operacional e o gate que ainda impede corte seguro.",
                mergeInsights(unificado.proximoMelhorAto(), timeline.proximoCiclo()),
                "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/painel-contextual/rota-tatica"
        ));
        out.add(widget(
                "OPERACAO_PESADA",
                "Operação pesada",
                "telemetry",
                operacao.readiness(),
                operacao.coberturaGlobal() >= 80d ? "#059669" : "#D97706",
                percent(operacao.coberturaGlobal()),
                "Idempotência, retry, replay controlado e readiness transversal do processo.",
                mergeInsights(operacao.alertas(), operacao.proximasAcoes()),
                "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/painel-contextual/telemetria-conectores"
        ));
        switch (painel.code()) {
            case "PAINEL_PENAL" -> penalWidgets(out, unificado, timeline, analytics, execucao);
            case "PAINEL_TRABALHISTA" -> trabalhistaWidgets(out, unificado, timeline, analytics, execucao);
            case "PAINEL_ELEITORAL" -> eleitoralWidgets(out, unificado, timeline, analytics);
            case "PAINEL_FAZENDA_PUBLICA" -> fazendaWidgets(out, unificado, timeline, analytics, execucao);
            case "PAINEL_PREVIDENCIARIO" -> previdenciarioWidgets(out, unificado, timeline, analytics, execucao);
            case "PAINEL_FAMILIA" -> familiaWidgets(out, unificado, timeline, analytics, execucao);
            case "PAINEL_JUIZADO" -> juizadoWidgets(out, unificado, timeline, analytics);
            default -> civelWidgets(out, unificado, timeline, analytics, execucao);
        }
        return List.copyOf(out);
    }

    private void penalWidgets(List<ProcessoPainelContextualWidget> out,
                              ProcessoUnificadoAggregate unificado,
                              ProcessoTimelineAggregate timeline,
                              ProcessoAnalyticsNacionalAggregate analytics,
                              ProcessoExecucaoAggregate execucao) {
        out.add(widget("RADAR_REUS_PRESOS", "Radar de réus presos", "critical-clock", execucao.totalOperacoesCustodia() > 0 ? "CRITICO" : "ATENCAO", "#DC2626", execucao.totalOperacoesCustodia() + " custódia(s)", "Controle de liberdade, excesso de prazo e audiência de custódia.", mergeInsights(List.of("Mandados: " + execucao.totalMandados()), timeline.alertas()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/fatias/penal-custodia"));
        out.add(widget("PRESCRICAO_ATIVA", "Calculadora de prescrição ativa", "prescription", analytics.riscoSlaGlobal() > 70d ? "ALTO" : "ESTAVEL", analytics.riscoSlaGlobal() > 70d ? "#D97706" : "#2563EB", percent(analytics.riscoSlaGlobal()), "Recalibra marcos penais e evidencia prazos críticos após cada movimentação.", mergeInsights(analytics.alertas(), timeline.proximoCiclo()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/analytics-nacional"));
        if (normalizeAxis(unificado.competencia().ramoDireito()).contains("MILITAR")) {
            out.add(widget("VISAO_MILITAR", "Visão militar", "military", "CONTROLADO", "#7C3AED", safe(unificado.competencia().grauJurisdicao()), "Painel de conselhos de justiça e requisição de militares por comando.", List.of("Ramo militar detectado pelo motor de competência."), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/dsl"));
        }
    }

    private void civelWidgets(List<ProcessoPainelContextualWidget> out,
                              ProcessoUnificadoAggregate unificado,
                              ProcessoTimelineAggregate timeline,
                              ProcessoAnalyticsNacionalAggregate analytics,
                              ProcessoExecucaoAggregate execucao) {
        out.add(widget("COCKPIT_CONSTRICAO", "Cockpit de constrição de bens", "asset-lock", execucao.totalTrilhas() > 0 ? "ATIVO" : "ESTAVEL", "#2563EB", execucao.totalMandados() + " ordem(ns)", "Unifica satisfação, constrição e frustração executiva do cível comum.", mergeInsights(execucao.alertas(), execucao.proximoMelhorPasso()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/fatias/civel-primeiro-grau"));
        out.add(widget("DEMANDAS_REPETITIVAS", "Filtro de demandas repetitivas", "mass-litigation", analytics.taxaCongestionamento() > 50d ? "ATENCAO" : "ESTAVEL", analytics.taxaCongestionamento() > 50d ? "#D97706" : "#059669", percent(analytics.taxaCongestionamento()), "Evidencia temas repetitivos e filas congestionadas aguardando tese uniformizada.", mergeInsights(analytics.alertas(), List.of("Gargalos de fila: " + analytics.gargalosFila().size())), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/analytics-nacional"));
        out.add(widget("PAUTA_DINAMICA", "Pauta dinâmica de audiências", "calendar", timeline.totalPendencias() > 0 ? "OPERACIONAL" : "LIVRE", "#7C3AED", timeline.totalPendencias() + " marco(s)", "Mostra conciliação, instrução e saneamento em contexto de massa.", mergeInsights(timeline.proximoCiclo(), timeline.alertas()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/timeline"));
    }

    private void trabalhistaWidgets(List<ProcessoPainelContextualWidget> out,
                                    ProcessoUnificadoAggregate unificado,
                                    ProcessoTimelineAggregate timeline,
                                    ProcessoAnalyticsNacionalAggregate analytics,
                                    ProcessoExecucaoAggregate execucao) {
        out.add(widget("RADAR_EXECUCAO_FRUSTRADA", "Radar de execução frustrada", "execution", execucao.totalBloqueantes() > 0 ? "CRITICO" : "ATIVO", execucao.totalBloqueantes() > 0 ? "#DC2626" : "#2563EB", execucao.totalBloqueantes() + " bloqueante(s)", "Identifica execução sem satisfação e prepara escalonamento para medidas patrimoniais.", mergeInsights(execucao.alertas(), execucao.proximoMelhorPasso()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/fatias/execucao-fiscal-fazendaria"));
        out.add(widget("BNDT_ATIVA", "Integração BNDT ativa", "connector", analytics.riscoSlaGlobal() > 60d ? "ATENCAO" : "ESTAVEL", "#2563EB", percent(100d - analytics.riscoSlaGlobal()), "Entrada trabalhista preparada para checagem de devedor e last mile executiva.", mergeInsights(analytics.alertas(), timeline.proximoCiclo()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/painel-contextual/trabalhista/bndt"));
        out.add(widget("DEPOSITOS_JUDICIAIS", "Controle de depósitos judiciais", "money", timeline.totalBloqueantes() > 0 ? "ATENCAO" : "ESTAVEL", "#059669", percent(analytics.riscoSlaGlobal()), "Rastreia alvarás, contas vinculadas e liberação de valores com trilha explicável.", mergeInsights(timeline.alertas(), analytics.alertas()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/painel-contextual/telemetria-conectores"));
    }

    private void eleitoralWidgets(List<ProcessoPainelContextualWidget> out,
                                  ProcessoUnificadoAggregate unificado,
                                  ProcessoTimelineAggregate timeline,
                                  ProcessoAnalyticsNacionalAggregate analytics) {
        out.add(widget("CRONOMETRO_HORAS", "Cronômetro em horas", "hours", timeline.totalBloqueantes() > 0 ? "CRITICO" : "ATIVO", "#DC2626", unificado.competencia().prazoTriagemHoras() + "h", "Fluxo eleitoral prioriza prazos por hora em anos de crise e campanha.", mergeInsights(timeline.alertas(), timeline.proximoCiclo()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/timeline"));
        out.add(widget("RADAR_INELEGIBILIDADE", "Radar de inelegibilidade", "integrity", analytics.mapaUrgencia() > 0 ? "ATENCAO" : "ESTAVEL", "#7C3AED", analytics.mapaUrgencia() + " alerta(s)", "Cruza urgência, histórico e integridade de candidatura no eixo eleitoral.", mergeInsights(analytics.alertas(), List.of("Risco global: " + percent(analytics.riscoSlaGlobal()))), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/analytics-nacional"));
        out.add(widget("PRESTACAO_CONTAS", "Esteira de prestação de contas", "ledger", timeline.totalPendencias() > 0 ? "OPERACIONAL" : "ESTAVEL", "#2563EB", timeline.totalPendencias() + " inconsistência(s)", "Monitora anomalias, pendências documentais e travas normativas eleitorais.", mergeInsights(timeline.alertas(), analytics.alertas()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/pre-gravacao"));
    }

    private void fazendaWidgets(List<ProcessoPainelContextualWidget> out,
                                ProcessoUnificadoAggregate unificado,
                                ProcessoTimelineAggregate timeline,
                                ProcessoAnalyticsNacionalAggregate analytics,
                                ProcessoExecucaoAggregate execucao) {
        out.add(widget("GESTOR_LOTES_FISCAIS", "Gestor de lotes fiscais", "batch", analytics.gargalosFila().size() > 0 ? "ATIVO" : "ESTAVEL", "#2563EB", analytics.gargalosFila().size() + " fila(s)", "Operação em lote para prescrição intercorrente, valor ínfimo e fechamento massivo.", mergeInsights(analytics.alertas(), List.of("Unidades críticas: " + analytics.unidadesCriticas().size())), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/fatias/execucao-fiscal-fazendaria"));
        out.add(widget("RADAR_GARANTIAS", "Radar de garantias", "guarantee", execucao.totalTrilhas() > 0 ? "ATIVO" : "ATENCAO", "#D97706", execucao.totalTrilhas() + " trilha(s)", "Controla garantias, apólices e vínculo entre execução e embargos.", mergeInsights(execucao.alertas(), execucao.proximoMelhorPasso()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/fatias/execucao-fiscal-fazendaria"));
        out.add(widget("MONITOR_EMBARGOS", "Monitor de embargos", "shield", timeline.totalBloqueantes() > 0 ? "CRITICO" : "CONTROLADO", timeline.totalBloqueantes() > 0 ? "#DC2626" : "#059669", timeline.totalBloqueantes() + " trava(s)", "Liga embargos, efeito suspensivo e blindagem do fluxo executivo fazendário.", mergeInsights(timeline.alertas(), timeline.proximoCiclo()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/timeline"));
    }

    private void previdenciarioWidgets(List<ProcessoPainelContextualWidget> out,
                                       ProcessoUnificadoAggregate unificado,
                                       ProcessoTimelineAggregate timeline,
                                       ProcessoAnalyticsNacionalAggregate analytics,
                                       ProcessoExecucaoAggregate execucao) {
        out.add(widget("TRILHO_INSS_CNIS", "Trilho INSS/CNIS", "social-security", analytics.riscoSlaGlobal() > 60d ? "ATENCAO" : "ESTAVEL", "#2563EB", percent(100d - analytics.riscoSlaGlobal()), "Entrada preparada para extratos previdenciários, prova e reprocessamento seguro.", mergeInsights(analytics.alertas(), timeline.proximoCiclo()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/painel-contextual/previdenciario/trilho"));
        out.add(widget("FILA_PERICIAS", "Fila de perícias médicas", "medical", timeline.totalPendencias() > 0 ? "ATENCAO" : "ESTAVEL", "#D97706", timeline.totalPendencias() + " pendência(s)", "Controla agenda pericial, atraso de laudos e janela de resposta.", mergeInsights(timeline.alertas(), analytics.alertas()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/timeline"));
        out.add(widget("ESTEIRA_RPV_PRECATORIO", "Esteira RPV/Precatório", "payment", execucao.totalTrilhas() > 0 ? "ATIVO" : "ESTAVEL", "#059669", execucao.totalTrilhas() + " trilha(s)", "Fecha o ciclo de pagamento público com trilha executiva e governança de prazo.", mergeInsights(execucao.alertas(), execucao.proximoMelhorPasso()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/fatias/execucao-fiscal-fazendaria"));
    }

    private void familiaWidgets(List<ProcessoPainelContextualWidget> out,
                                ProcessoUnificadoAggregate unificado,
                                ProcessoTimelineAggregate timeline,
                                ProcessoAnalyticsNacionalAggregate analytics,
                                ProcessoExecucaoAggregate execucao) {
        out.add(widget("MEDIDAS_PROTETIVAS", "Medidas protetivas ativas", "shield", execucao.totalTrilhas() > 0 ? "ATIVO" : "ESTAVEL", "#DC2626", execucao.totalTrilhas() + " medida(s)", "Controla medidas protetivas, afastamento e monitoramento eletrônico.", mergeInsights(execucao.alertas(), execucao.proximoMelhorPasso()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/fatias/civel-primeiro-grau"));
        out.add(widget("ALIMENTOS_URGENTES", "Régua de alimentos", "family", timeline.totalPendencias() > 0 ? "ATENCAO" : "ESTAVEL", "#D97706", timeline.totalPendencias() + " pendência(s)", "Unifica alimentos provisionais, definitivos e prisão civil por inadimplência.", mergeInsights(timeline.alertas(), timeline.proximoCiclo()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/timeline"));
        out.add(widget("GUARDA_VISITAS", "Painel de guarda e visitas", "children", analytics.riscoSlaGlobal() > 50d ? "ATENCAO" : "ESTAVEL", "#7C3AED", percent(analytics.riscoSlaGlobal()), "Controla acordos de guarda, visitas e descumprimento parental.", mergeInsights(analytics.alertas(), timeline.alertas()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/analytics-nacional"));
    }

    private void juizadoWidgets(List<ProcessoPainelContextualWidget> out,
                                ProcessoUnificadoAggregate unificado,
                                ProcessoTimelineAggregate timeline,
                                ProcessoAnalyticsNacionalAggregate analytics) {
        out.add(widget("CONCILIACAO_OBRIGATORIA", "Pauta de conciliação", "handshake", timeline.totalPendencias() > 0 ? "ATENCAO" : "ESTAVEL", "#059669", timeline.totalPendencias() + " pauta(s)", "Rito simplificado exige tentativa de conciliação prévia à instrução.", mergeInsights(timeline.alertas(), timeline.proximoCiclo()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/timeline"));
        out.add(widget("TETO_JEC", "Controle de teto econômico", "ceiling", analytics.taxaCongestionamento() > 40d ? "ATENCAO" : "ESTAVEL", "#2563EB", percent(analytics.taxaCongestionamento()), "Monitora conformidade com limite de alçada do rito sumaríssimo.", mergeInsights(analytics.alertas(), List.of()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/analytics-nacional"));
        out.add(widget("TURMA_RECURSAL", "Rota turma recursal", "route", timeline.totalBloqueantes() > 0 ? "ALTO" : "ESTAVEL", timeline.totalBloqueantes() > 0 ? "#DC2626" : "#2563EB", timeline.totalBloqueantes() + " trava(s)", "Mapeia o caminho recursal específico do juizado especial.", mergeInsights(timeline.alertas(), timeline.proximoCiclo()), "/api/v1/processual/unificado/" + unificado.identity().processoId() + "/painel-contextual/rota-tatica"));
    }

    private List<ProcessoPainelConectorSaude> buildConnectors(JudicialConnectorHubReport hubReport) {
        JudicialConnectorCommandCenterReport operational = hubReport == null ? null : hubReport.operational();
        JudicialConnectorObservabilityReport observability = operational == null ? null : operational.observability();
        if (observability == null || observability.systems() == null) {
            return List.of();
        }
        return observability.systems().stream()
                .limit(6)
                .map(this::connector)
                .toList();
    }

    private ProcessoPainelConectorSaude connector(JudicialConnectorObservabilitySystemReport item) {
        return new ProcessoPainelConectorSaude(
                item.system() == null ? "OUTRO" : item.system().name(),
                item.observabilityStatus(),
                colorForStatus(item.observabilityStatus()),
                item.successRate(),
                item.submissionReady(),
                item.syncReady(),
                item.latestEventAt(),
                item.blockers(),
                item.warnings()
        );
    }

    private ProcessoPainelContextualWidget widget(String code,
                                                  String title,
                                                  String kind,
                                                  String status,
                                                  String accentColor,
                                                  String headline,
                                                  String subtitle,
                                                  List<String> insights,
                                                  String navigationPath) {
        return new ProcessoPainelContextualWidget(code, title, kind, status, accentColor, headline, subtitle, insights, navigationPath);
    }

    private List<String> mergeInsights(List<String> primary, List<String> secondary) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (primary != null) merged.addAll(primary);
        if (secondary != null) merged.addAll(secondary);
        return merged.stream().filter(Objects::nonNull).map(String::trim).filter(text -> !text.isBlank()).limit(6).toList();
    }

    private String percent(double value) {
        return Math.round(Math.max(0d, value)) + "%";
    }

    private String colorForStatus(String value) {
        String normalized = normalizeAxis(value);
        if (normalized.contains("BLOCK") || normalized.contains("CRIT")) return "#DC2626";
        if (normalized.contains("DEGRAD") || normalized.contains("ATENCAO")) return "#D97706";
        return "#059669";
    }

    private String normalizeAxis(String value) {
        if (value == null) return "NAO_INFORMADO";
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? "NAO_INFORMADO" : normalized;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/D" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private PainelDescriptor resolvePanel(String ramo, String rito) {
        var canonicalGroup = RitoProcessual.tryParse(rito).map(RitoProcessual::getGrupoPrincipal);
        if (canonicalGroup.isPresent()) {
            PainelDescriptor fromRito = switch (canonicalGroup.get()) {
                case PENAL, EXECUCAO_PENAL, MILITAR -> new PainelDescriptor("PAINEL_PENAL", "Painel Penal Contextual");
                case TRABALHISTA -> new PainelDescriptor("PAINEL_TRABALHISTA", "Painel Trabalhista Contextual");
                case ELEITORAL -> new PainelDescriptor("PAINEL_ELEITORAL", "Painel Eleitoral Contextual");
                case EXECUCAO_FISCAL -> new PainelDescriptor("PAINEL_FAZENDA_PUBLICA", "Painel Fazenda Pública e Tributário");
                case PREVIDENCIARIO -> new PainelDescriptor("PAINEL_PREVIDENCIARIO", "Painel Previdenciário Contextual");
                case FAMILIA -> new PainelDescriptor("PAINEL_FAMILIA", "Painel Família e Sucessões");
                case JUIZADO -> new PainelDescriptor("PAINEL_JUIZADO", "Painel Juizado Especial");
                default -> null;
            };
            if (fromRito != null) return fromRito;
        }
        String axis = normalizeAxis(ramo) + '|' + normalizeAxis(rito);
        if (axis.contains("PENAL") || axis.contains("CUSTOD")) return new PainelDescriptor("PAINEL_PENAL", "Painel Penal Contextual");
        if (axis.contains("TRABALH")) return new PainelDescriptor("PAINEL_TRABALHISTA", "Painel Trabalhista Contextual");
        if (axis.contains("ELEITOR")) return new PainelDescriptor("PAINEL_ELEITORAL", "Painel Eleitoral Contextual");
        if (axis.contains("FAZENDA") || axis.contains("TRIBUT") || axis.contains("EXECUCAO_FISCAL")) return new PainelDescriptor("PAINEL_FAZENDA_PUBLICA", "Painel Fazenda Pública e Tributário");
        if (axis.contains("PREVIDEN") || axis.contains("JEF")) return new PainelDescriptor("PAINEL_PREVIDENCIARIO", "Painel Previdenciário Contextual");
        return new PainelDescriptor("PAINEL_CIVEL", "Painel Cível Contextual");
    }

    private record PainelDescriptor(String code, String title) {
    }
}
