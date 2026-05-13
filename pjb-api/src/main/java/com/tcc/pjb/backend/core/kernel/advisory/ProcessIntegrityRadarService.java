package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import com.tcc.pjb.backend.service.rito.dto.RitoPlanDto;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;

@Service
public class ProcessIntegrityRadarService {

    public ProcessIntegrityRadarReport analyzeRequest(LaianePeticaoAssistRequest request,
                                                      CanonicalContext canonical,
                                                      String ritoName,
                                                      LegalCoherenceReport coherence,
                                                      ProtocolDryRunReport dryRun,
                                                      DynamicCompetenceDistributionResponse competencia,
                                                      TetoProcessualService.DiagnosticoTetoProcessual teto) {
        Objects.requireNonNull(request, "request");
        List<ProcessIntegrityRadarReport.Finding> findings = new ArrayList<>();
        LinkedHashSet<String> nextActions = new LinkedHashSet<>();
        LinkedHashSet<String> watchpoints = new LinkedHashSet<>();
        double score = 0.90d;

        if (coherence != null && coherence.blocking()) {
            findings.add(finding("COHERENCE_BLOCKING", "NULLITY", "Incoerência jurídica bloqueante", "CRITICAL", true, "A peça ainda contém inconsistências graves entre pedido, prova, rito ou competência.", coherence.strengths()));
            nextActions.addAll(coherence.strategicRecommendations());
            score -= 0.22d;
        }
        if (dryRun != null && !dryRun.apto()) {
            findings.add(finding("DRY_RUN_FAILURE", "PROTOCOL", "Ensaio de protocolo com falhas", hasCritical(dryRun) ? "CRITICAL" : "HIGH", hasCritical(dryRun), "O ensaio de protocolo encontrou falhas que podem gerar rejeição, emenda ou redistribuição.", dryRun.nextActions()));
            nextActions.addAll(dryRun.nextActions());
            score -= hasCritical(dryRun) ? 0.18d : 0.10d;
        }
        if (competencia == null) {
            findings.add(finding("COMPETENCE_MISSING", "COMPETENCE", "Competência não resolvida", "CRITICAL", true, "Não foi possível fechar com segurança tribunal, unidade ou destino judicial.", List.of()));
            nextActions.add("Materializar os sinais territoriais e materiais necessários para fechamento competencial.");
            score -= 0.18d;
        } else if (!competencia.distribuicaoAutomatica()) {
            findings.add(finding("COMPETENCE_REVIEW", "COMPETENCE", "Competência depende de revisão humana", "HIGH", false, firstNonBlank(competencia.motivacao(), "A distribuição automática não está liberada para o contexto atual."), merge(competencia.alertas(), competencia.fatoresRevisaoHumana())));
            nextActions.add("Submeter a competência para revisão humana antes do protocolo real.");
            score -= 0.08d;
        }
        if (teto != null && teto.bloqueante()) {
            findings.add(finding("PROCEDURAL_VALUE_LIMIT", "COMPETENCE", "Valor da causa incompatível com o rito/competência", "CRITICAL", true, firstNonBlank(teto.sugestaoOperacional(), "O valor da causa extrapola o teto operacional da trilha escolhida."), List.of(firstNonBlank(teto.fundamentoLegal(), teto.codigoDiagnostico()))));
            nextActions.add("Revisar valor da causa, renúncia econômica ou trilha procedimental adequada.");
            score -= 0.16d;
        }
        if (blank(request.getCpfCnpjAutor()) || blank(request.getCpfCnpjReu())) {
            findings.add(finding("PARTIES_IDENTIFICATION_GAP", "NULLITY", "Qualificação incompleta de partes", "HIGH", false, "Ausência de identificadores mínimos pode gerar exigência ou vulnerabilidade formal do ajuizamento.", List.of()));
            nextActions.add("Completar a qualificação das partes com CPF/CNPJ ou identificador equivalente antes do protocolo.");
            score -= 0.07d;
        }
        if (Boolean.TRUE.equals(request.getRequerLiminar()) && blank(request.getTextoFatosResumido())) {
            findings.add(finding("URGENT_RELIEF_WEAKNESS", "EVIDENCE", "Tutela urgente sem narrativa resumida adequada", "HIGH", false, "Pedidos urgentes sem narrativa fática minimamente estruturada tendem a perder robustez na análise inicial.", List.of()));
            nextActions.add("Consolidar narrativa resumida focada em dano atual, risco de ineficácia e probabilidade do direito.");
            score -= 0.06d;
        }
        if (canonical == null || blank(canonical.classeTpuCodigo())) {
            findings.add(finding("TPU_GAP", "PROTOCOL", "Classe TPU não consolidada", "HIGH", false, "A ausência de classe TPU reduz a segurança do schema documental e do workflow inicial.", List.of(ritoName)));
            score -= 0.06d;
        } else {
            watchpoints.add("Classe TPU consolidada em " + canonical.classeTpuCodigo());
        }
        if (!blank(ritoName)) {
            watchpoints.add("Rito consolidado em trilha nominal: " + ritoName);
        }

        String status = findings.stream().anyMatch(ProcessIntegrityRadarReport.Finding::blocking)
                ? "BLOCKING_RISK"
                : findings.isEmpty() ? "STABLE" : "WATCHLIST";
        if (nextActions.isEmpty()) {
            nextActions.add("Sem risco material bloqueante adicional identificado na trilha atual.");
        }
        return new ProcessIntegrityRadarReport(
                status,
                round(clamp(score)),
                findings.stream().anyMatch(ProcessIntegrityRadarReport.Finding::blocking),
                List.copyOf(findings),
                List.copyOf(nextActions),
                List.copyOf(watchpoints),
                PayloadMaps.ofEntries(
                        "lane", "PETITION_ASSIST",
                        "ritoName", ritoName,
                        "classeTpu", canonical != null ? canonical.classeTpuCodigo() : null,
                        "competenceResolved", competencia != null,
                        "dryRunStatus", dryRun != null ? dryRun.status() : null
                )
        );
    }

    public ProcessIntegrityRadarReport analyzeProcess(Processo processo,
                                                      String ritoName,
                                                      RitoPlanDto ritoPlan,
                                                      LegalCoherenceReport coherence,
                                                      ProtocolDryRunReport dryRun,
                                                      List<String> riskSignals) {
        Objects.requireNonNull(processo, "processo");
        List<ProcessIntegrityRadarReport.Finding> findings = new ArrayList<>();
        LinkedHashSet<String> nextActions = new LinkedHashSet<>();
        LinkedHashSet<String> watchpoints = new LinkedHashSet<>();
        double score = 0.88d;

        if (processo.getFaseAtual() == FaseProcessual.RECURSAL) {
            findings.add(finding("RECURSAL_STRICTNESS", "RECURSAL", "Fase recursal demanda estrita regularidade", "MEDIUM", false, "A fase recursal exige tempestividade, preparo, dialeticidade e delimitação precisa da insurgência.", List.of(ritoName)));
            nextActions.add("Revalidar tempestividade, preparo e aderência da peça recursal ao capítulo impugnado.");
            score -= 0.07d;
        }
        if (ritoPlan != null && ritoPlan.getBlockingOpen() != null && !ritoPlan.getBlockingOpen().isEmpty()) {
            findings.add(finding("WORKFLOW_BLOCKING_OPEN", "DEADLINE", "Work items bloqueantes em aberto", "HIGH", false, "O workflow materializa pendências que podem comprometer prazo ou próxima transição de fase.", ritoPlan.getBlockingOpen().stream().map(item -> firstNonBlank(item.getTitulo(), item.getTemplateCode(), String.valueOf(item.getId()))).filter(Objects::nonNull).toList()));
            nextActions.add("Encerrar ou redistribuir com prioridade máxima os work items bloqueantes abertos.");
            score -= 0.10d;
        }
        if (coherence != null && coherence.blocking()) {
            findings.add(finding("PROCESS_COHERENCE_BLOCK", "NULLITY", "Twin detectou incoerência processual bloqueante", "CRITICAL", true, "O processo carrega incoerência material suficiente para travar ato relevante subsequente.", coherence.strategicRecommendations()));
            nextActions.addAll(coherence.strategicRecommendations());
            score -= 0.18d;
        }
        if (dryRun != null && !dryRun.apto()) {
            findings.add(finding("NEXT_STEP_NOT_READY", "PROTOCOL", "Próxima etapa não está pronta", hasCritical(dryRun) ? "CRITICAL" : "HIGH", hasCritical(dryRun), "O ensaio de continuidade processual identificou falhas antes do próximo ato relevante.", dryRun.nextActions()));
            nextActions.addAll(dryRun.nextActions());
            score -= hasCritical(dryRun) ? 0.14d : 0.08d;
        }
        if (processo.getDataUltimaMovimentacao() != null) {
            long stagnationDays = Duration.between(processo.getDataUltimaMovimentacao(), LocalDateTime.now()).toDays();
            if (stagnationDays >= 30) {
                findings.add(finding("PROCESS_STAGNATION", "DEADLINE", "Estagnação processual relevante", stagnationDays >= 90 ? "HIGH" : "MEDIUM", false, "O processo está sem movimentação relevante há período sensível para a governança da atuação.", List.of(Long.toString(stagnationDays))));
                nextActions.add("Revisar filas, pendências de gabinete/secretaria e próximos atos úteis para remover estagnação.");
                score -= stagnationDays >= 90 ? 0.08d : 0.04d;
            }
        }
        if (riskSignals != null) {
            for (String riskSignal : riskSignals) {
                if (!blank(riskSignal)) {
                    watchpoints.add(riskSignal.trim());
                }
            }
        }
        if (!blank(ritoName)) {
            watchpoints.add("Rito efetivo: " + ritoName);
        }
        String status = findings.stream().anyMatch(ProcessIntegrityRadarReport.Finding::blocking)
                ? "BLOCKING_RISK"
                : findings.isEmpty() ? "STABLE" : "WATCHLIST";
        if (nextActions.isEmpty()) {
            nextActions.add("Trilha sem defeito material bloqueante na leitura atual do twin.");
        }
        return new ProcessIntegrityRadarReport(
                status,
                round(clamp(score)),
                findings.stream().anyMatch(ProcessIntegrityRadarReport.Finding::blocking),
                List.copyOf(findings),
                List.copyOf(nextActions),
                List.copyOf(watchpoints),
                PayloadMaps.ofEntries(
                        "lane", "PROCESS_TWIN",
                        "processoId", processo.getId(),
                        "numeroUnificado", processo.getNumeroUnificado(),
                        "phase", processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null,
                        "ritoName", ritoName,
                        "today", LocalDate.now().toString()
                )
        );
    }

    private ProcessIntegrityRadarReport.Finding finding(String code, String domain, String title, String severity, boolean blocking, String message, List<String> evidence) {
        return new ProcessIntegrityRadarReport.Finding(code, domain, title, severity, blocking, message, evidence == null ? List.of() : List.copyOf(new LinkedHashSet<>(evidence)));
    }

    private boolean hasCritical(ProtocolDryRunReport dryRun) {
        return dryRun != null && dryRun.checks().stream().anyMatch(check -> !check.passed() && "CRITICAL".equals(check.severity()));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private List<String> merge(List<String> first, List<String> second) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (first != null) {
            first.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).forEach(out::add);
        }
        if (second != null) {
            second.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).forEach(out::add);
        }
        return List.copyOf(out);
    }

    private double clamp(double value) {
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
