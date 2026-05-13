package com.tcc.pjb.backend.service.processo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleDecision;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.dto.event.ProcessoAjuizadoEvent;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.competencia.MapaCompetenciaDinamicoEngine;
import com.tcc.pjb.backend.service.distribuicao.ProcessoInitialDistributionSnapshotService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import com.tcc.pjb.backend.service.territorial.TerritorialProcessualService;

@Component
public class ProcessoPostAjuizamentoOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(ProcessoPostAjuizamentoOrchestratorService.class);

    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final ProcessoLifecycleMachine lifecycleMachine;
    private final MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine;
    private final TetoProcessualService tetoProcessualService;
    private final ProcessoInitialDistributionSnapshotService processoInitialDistributionSnapshotService;
    private final TerritorialProcessualService territorialProcessualService;
    private final ProcessoSlaJudicialService processoSlaJudicialService;
    private final AuditLedgerService auditLedgerService;

    public ProcessoPostAjuizamentoOrchestratorService(ProcessoRepository processoRepository,
                                                      WorkItemRepository workItemRepository,
                                                      ProcessoLifecycleMachine lifecycleMachine,
                                                      MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine,
                                                      ProcessoInitialDistributionSnapshotService processoInitialDistributionSnapshotService,
                                                      TetoProcessualService tetoProcessualService,
                                                      TerritorialProcessualService territorialProcessualService,
                                                      ProcessoSlaJudicialService processoSlaJudicialService,
                                                      AuditLedgerService auditLedgerService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.lifecycleMachine = Objects.requireNonNull(lifecycleMachine);
        this.mapaCompetenciaDinamicoEngine = Objects.requireNonNull(mapaCompetenciaDinamicoEngine);
        this.processoInitialDistributionSnapshotService = Objects.requireNonNull(processoInitialDistributionSnapshotService);
        this.tetoProcessualService = Objects.requireNonNull(tetoProcessualService);
        this.territorialProcessualService = Objects.requireNonNull(territorialProcessualService);
        this.processoSlaJudicialService = Objects.requireNonNull(processoSlaJudicialService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @PjbTransactionalBudget(operation = "ajuizamento.post-commit.persist", maxMillis = 2500, critical = true)
    public void onProcessoAjuizado(ProcessoAjuizadoEvent event) {
        if (event == null || event.getProcessoId() == null) {
            return;
        }
        Processo processo = processoRepository.findById(event.getProcessoId()).orElse(null);
        if (processo == null) {
            return;
        }

        DynamicCompetenceDistributionResponse distribuicao = ensureDistributionSnapshot(processo);
        TetoProcessualService.DiagnosticoTetoProcessual teto = tetoProcessualService.diagnosticar(processo);
        TerritorialProcessualService.DiagnosticoTerritorialProcessual territorial = territorialProcessualService.diagnosticar(processo);
        ProcessoLifecycleDecision distribuicaoDecision = ensureDistributedLifecycle(processo);
        ReviewDisposition disposition = classify(processo, teto, territorial, distribuicao);
        ProcessoSlaJudicialService.ProcessoSlaSnapshot sla = processoSlaJudicialService.snapshot(processo);

        if (disposition.requiresSecretariatReview()) {
            upsertSecretariatReview(processo, teto, territorial, distribuicao, disposition, sla);
        } else {
            upsertInitialConclusion(processo, teto, territorial, distribuicao, disposition, sla);
        }

        try {
            auditLedgerService.appendSafely(
                    disposition.requiresSecretariatReview() ? "PROCESSO_POS_AJUIZAMENTO_SECRETARIA" : "PROCESSO_POS_AJUIZAMENTO_GABINETE",
                    "PROCESSO_POS_AJUIZAMENTO",
                    String.valueOf(processo.getId()),
                    disposition.auditKey(processo),
                    disposition.summary(processo, teto, territorial, distribuicao, distribuicaoDecision)
            );
        } catch (Exception ex) {
            log.warn("Falha nao bloqueante ao auditar pos-ajuizamento. processoId={} erro={}", processo.getId(), ex.getMessage());
        }
    }

    private DynamicCompetenceDistributionResponse ensureDistributionSnapshot(Processo processo) {
        if (processo == null) {
            return null;
        }
        boolean missingSnapshot = isBlank(processo.getUnidadeJudiciariaCodigo()) || isBlank(processo.getTribunalCodigoRoteado());
        boolean staleSnapshot = missingSnapshot
                || isBlank(processo.getPreProtocoloStatus())
                || isBlank(processo.getCompetenciaTerritorialModo())
                || isBlank(processo.getPreventionMode())
                || isBlank(processo.getLinkageMode());
        DynamicCompetenceDistributionResponse distribuicao = null;
        try {
            if (missingSnapshot) {
                distribuicao = mapaCompetenciaDinamicoEngine.registrarDistribuicaoInicial(processo).orElse(null);
            }
            if (staleSnapshot) {
                processoInitialDistributionSnapshotService.consolidar(processo);
            }
            return distribuicao;
        } catch (Exception ex) {
            log.warn("Falha nao bloqueante ao reforcar distribuicao inicial. processoId={} erro={}", processo.getId(), ex.getMessage());
            return distribuicao;
        }
    }

    private ProcessoLifecycleDecision ensureDistributedLifecycle(Processo processo) {
        if (processo == null) {
            return null;
        }
        if (isDistributedStatus(processo)) {
            return lifecycleMachine.preview(processo, ProcessoLifecycleAction.DISTRIBUIR);
        }
        ProcessoLifecycleDecision decision = lifecycleMachine.apply(processo, ProcessoLifecycleAction.DISTRIBUIR);
        processoRepository.save(processo);
        return decision;
    }

    private void upsertSecretariatReview(Processo processo,
                                         TetoProcessualService.DiagnosticoTetoProcessual teto,
                                         TerritorialProcessualService.DiagnosticoTerritorialProcessual territorial,
                                         DynamicCompetenceDistributionResponse distribuicao,
                                         ReviewDisposition disposition,
                                         ProcessoSlaJudicialService.ProcessoSlaSnapshot sla) {
        String templateCode = "AJUIZAMENTO:REVISAO_SECRETARIA:" + processo.getId();
        WorkItem item = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processo.getId(), templateCode, WorkItemStatus.CANCELADO)
                .orElseGet(() -> WorkItem.builder().processo(processo).templateCode(templateCode).build());
        item.setFaseOrigem(processo.getFaseAtual() != null ? processo.getFaseAtual() : FaseProcessual.CONHECIMENTO);
        item.setType(WorkItemType.AJUIZAMENTO);
        item.setTitulo("Revisão inicial do ajuizamento — " + numeroProcesso(processo));
        item.setDescricao(buildSecretariatDescription(processo, teto, territorial, distribuicao, disposition));
        item.setQueueCode("SECRETARIA_AJUIZAMENTO_INICIAL");
        item.setInboxKey("SECRETARIA_AJUIZAMENTO_INICIAL");
        item.setAssignedRole(TipoUsuario.SERVIDOR_FORUM);
        item.setStatus(WorkItemStatus.PENDENTE);
        item.setPrioridade(disposition.priority());
        item.setBlocking(disposition.blocking());
        item.setUf(processo.getUf());
        item.setComarca(processo.getComarca());
        item.setDueAt(sla != null ? sla.dueAtSecretariatReview(disposition.blocking()) : Instant.now().plus(disposition.blocking() ? 6 : 12, ChronoUnit.HOURS));
        item.setBaseLegal(firstNonBlank(teto != null ? teto.fundamentoLegal() : null, "Revisão cartorária inicial do ajuizamento e saneamento de autuação."));
        workItemRepository.save(item);
    }

    private void upsertInitialConclusion(Processo processo,
                                         TetoProcessualService.DiagnosticoTetoProcessual teto,
                                         TerritorialProcessualService.DiagnosticoTerritorialProcessual territorial,
                                         DynamicCompetenceDistributionResponse distribuicao,
                                         ReviewDisposition disposition,
                                         ProcessoSlaJudicialService.ProcessoSlaSnapshot sla) {
        ProcessoLifecycleDecision decision = lifecycleMachine.apply(processo, ProcessoLifecycleAction.CONCLUIR_PARA_DESPACHO);
        processoRepository.save(processo);
        String templateCode = "AJUIZAMENTO:CONCLUSAO_INICIAL:" + processo.getId();
        WorkItem item = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processo.getId(), templateCode, WorkItemStatus.CANCELADO)
                .orElseGet(() -> WorkItem.builder().processo(processo).templateCode(templateCode).build());
        item.setFaseOrigem(decision != null ? decision.faseDestino() : processo.getFaseAtual());
        item.setType(WorkItemType.DESPACHO);
        item.setTitulo("Conclusão inicial do ajuizamento — " + numeroProcesso(processo));
        item.setDescricao(buildMagistratureDescription(processo, teto, territorial, distribuicao, decision));
        item.setQueueCode("GABINETE_AJUIZAMENTO_INICIAL");
        item.setInboxKey("GABINETE_DESPACHO_INICIAL");
        item.setAssignedRole(TipoUsuario.JUIZ);
        item.setStatus(WorkItemStatus.PENDENTE);
        item.setPrioridade(disposition.priority());
        item.setBlocking(false);
        item.setUf(processo.getUf());
        item.setComarca(processo.getComarca());
        item.setDueAt(sla != null ? sla.dueAtInitialConclusion() : Instant.now().plus(24, ChronoUnit.HOURS));
        item.setBaseLegal(firstNonBlank(teto != null ? teto.fundamentoLegal() : null, "Conclusão inicial ao magistrado após autuação, distribuição e análise de aderência."));
        workItemRepository.save(item);
    }

    private ReviewDisposition classify(Processo processo,
                                       TetoProcessualService.DiagnosticoTetoProcessual teto,
                                       TerritorialProcessualService.DiagnosticoTerritorialProcessual territorial,
                                       DynamicCompetenceDistributionResponse distribuicao) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        boolean blocking = false;

        if (teto != null && teto.bloqueante()) {
            blocking = true;
            reasons.add(firstNonBlank(teto.fundamentoLegal(), "Erro de teto bloqueante identificado para o rito ou competência econômica."));
        } else if (teto != null && teto.alerta()) {
            reasons.add(firstNonBlank(teto.sugestaoOperacional(), "Valor da causa próximo ao teto exige conferência antes do impulso útil seguinte."));
        }
        if (territorial != null && territorial.bloqueante()) {
            blocking = true;
            reasons.add(firstNonBlank(territorial.fundamentoLegal(), "Inconsistência territorial bloqueante identificada no fechamento da competência."));
        } else if (territorial != null && territorial.alerta()) {
            reasons.add(firstNonBlank(territorial.sugestaoOperacional(), "Há risco territorial que recomenda revisão humana antes da marcha ordinária."));
        }
        if (distribuicao != null && !distribuicao.distribuicaoAutomatica()) {
            reasons.add("Distribuição marcada para revisão humana antes da conclusão ao gabinete.");
        }
        if (!isBlank(processo.getPreProtocoloStatus()) && !containsAny(processo.getPreProtocoloStatus(), "APTA", "READY", "AUTOMATICA")) {
            reasons.add("Status pré-protocolo ou de distribuição demanda saneamento inicial da autuação.");
        }
        if (isBlank(processo.getUnidadeJudiciariaCodigo()) || isBlank(processo.getTribunalCodigoRoteado())) {
            blocking = true;
            reasons.add("Snapshot de distribuição ainda não consolidado no processo.");
        }
        if (isBlank(processo.getParteReuNome()) && isBlank(processo.getParteReuCpf())) {
            reasons.add("Réu sem qualificação mínima consolidada para futura citação/intimação.");
        }
        if (!isBlank(processo.getConnectorSubmissionStatus()) && containsAny(processo.getConnectorSubmissionStatus(), "PENDENTE", "ERRO", "MANUAL")) {
            reasons.add("Trilha do conector judicial exige conferência humana antes da marcha ordinária.");
        }
        return new ReviewDisposition(List.copyOf(reasons), blocking);
    }

    private boolean isDistributedStatus(Processo processo) {
        return processo != null
                && processo.getStatusProcesso() != null
                && EnumSet.of(
                        StatusProcesso.DISTRIBUIDO,
                        StatusProcesso.EM_ANDAMENTO,
                        StatusProcesso.CITACAO_REALIZADA
                ).contains(processo.getStatusProcesso());
    }

    private String buildSecretariatDescription(Processo processo,
                                               TetoProcessualService.DiagnosticoTetoProcessual teto,
                                               TerritorialProcessualService.DiagnosticoTerritorialProcessual territorial,
                                               DynamicCompetenceDistributionResponse distribuicao,
                                               ReviewDisposition disposition) {
        List<String> lines = new ArrayList<>();
        lines.add("Revisar a autuação inicial antes da conclusão ao gabinete.");
        add(lines, "Rito", processo.getRito() != null ? processo.getRito().name() : null);
        add(lines, "Classe", processo.getClasseProcessual());
        add(lines, "Assunto", processo.getAssunto());
        add(lines, "Valor da causa", formatMoney(processo.getValorCausa()));
        add(lines, "Tribunal roteado", processo.getTribunalCodigoRoteado());
        add(lines, "Unidade judiciária", processo.getUnidadeJudiciariaCodigo());
        if (distribuicao != null) {
            add(lines, "Score de distribuição", BigDecimal.valueOf(distribuicao.scoreFinal()).toPlainString());
            if (!distribuicao.alertas().isEmpty()) {
                lines.add("Alertas de distribuição: " + String.join(" | ", distribuicao.alertas()));
            }
            if (!distribuicao.fatoresRevisaoHumana().isEmpty()) {
                lines.add("Fatores de revisão humana: " + String.join(" | ", distribuicao.fatoresRevisaoHumana()));
            }
        }
        if (teto != null && (teto.alerta() || teto.bloqueante())) {
            lines.add("Diagnóstico de teto: " + firstNonBlank(teto.sugestaoOperacional(), teto.fundamentoLegal()));
        }
        if (territorial != null && (territorial.alerta() || territorial.bloqueante())) {
            lines.add("Diagnóstico territorial: " + firstNonBlank(territorial.sugestaoOperacional(), territorial.fundamentoLegal()));
            if (!territorial.reviewChecklist().isEmpty()) {
                lines.add("Checklist territorial: " + String.join(" | ", territorial.reviewChecklist()));
            }
        }
        if (!disposition.reasons().isEmpty()) {
            lines.add("Motivos da revisão: " + String.join(" | ", disposition.reasons()));
        }
        lines.add("Após o saneamento cartorário, promover conclusão inicial ao magistrado para despacho inaugural e definição de citação/intimação.");
        return String.join("\n", lines);
    }

    private String buildMagistratureDescription(Processo processo,
                                                TetoProcessualService.DiagnosticoTetoProcessual teto,
                                                TerritorialProcessualService.DiagnosticoTerritorialProcessual territorial,
                                                DynamicCompetenceDistributionResponse distribuicao,
                                                ProcessoLifecycleDecision decision) {
        List<String> lines = new ArrayList<>();
        lines.add("Processo distribuído e apto à análise inicial do magistrado.");
        add(lines, "Rito", processo.getRito() != null ? processo.getRito().name() : null);
        add(lines, "Classe", processo.getClasseProcessual());
        add(lines, "Assunto", processo.getAssunto());
        add(lines, "Valor da causa", formatMoney(processo.getValorCausa()));
        add(lines, "Comarca", processo.getComarca());
        add(lines, "UF", processo.getUf());
        add(lines, "Unidade judiciária", processo.getUnidadeJudiciariaCodigo());
        if (decision != null) {
            add(lines, "Transição de lifecycle", decision.transitionKey());
        }
        if (teto != null && teto.alerta()) {
            lines.add("Alerta econômico: " + firstNonBlank(teto.sugestaoOperacional(), teto.fundamentoLegal()));
        }
        if (territorial != null && territorial.alerta()) {
            lines.add("Alerta territorial: " + firstNonBlank(territorial.sugestaoOperacional(), territorial.fundamentoLegal()));
        }
        if (distribuicao != null && !distribuicao.alertas().isEmpty()) {
            lines.add("Alertas de distribuição: " + String.join(" | ", distribuicao.alertas()));
        }
        lines.add("Próximo passo esperado: despacho inaugural, saneamento inicial e definição do regime de citação/intimação do polo passivo.");
        return String.join("\n", lines);
    }

    private static String numeroProcesso(Processo processo) {
        return firstNonBlank(processo != null ? processo.getNumeroProcesso() : null,
                processo != null ? processo.getNumeroUnificado() : null,
                processo != null && processo.getId() != null ? "PJB-" + processo.getId() : "PROCESSO");
    }

    private static void add(List<String> lines, String label, String value) {
        String normalized = normalize(value);
        if (normalized != null) {
            lines.add(label + ": " + normalized);
        }
    }

    private static String formatMoney(BigDecimal value) {
        return value == null ? null : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static boolean containsAny(String value, String... needles) {
        String normalized = normalizeUpper(value);
        if (normalized == null || needles == null) {
            return false;
        }
        for (String needle : needles) {
            if (normalizeUpper(needle) != null && normalized.contains(normalizeUpper(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeUpper(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return normalize(value) == null;
    }

    private record ReviewDisposition(List<String> reasons, boolean blocking) {
        private ReviewDisposition {
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
        }

        boolean requiresSecretariatReview() {
            return blocking || !reasons.isEmpty();
        }

        int priority() {
            if (blocking) {
                return 1;
            }
            return reasons.size() >= 2 ? 1 : 2;
        }

        String auditKey(Processo processo) {
            return (requiresSecretariatReview() ? "SECRETARIA:" : "GABINETE:") + (processo != null ? processo.getId() : "SEM_PROCESSO");
        }

        String summary(Processo processo,
                       TetoProcessualService.DiagnosticoTetoProcessual teto,
                       TerritorialProcessualService.DiagnosticoTerritorialProcessual territorial,
                       DynamicCompetenceDistributionResponse distribuicao,
                       ProcessoLifecycleDecision decision) {
            List<String> out = new ArrayList<>();
            out.add(requiresSecretariatReview() ? "revisao_secretaria" : "conclusao_gabinete");
            out.add("processo=" + (processo != null ? processo.getId() : null));
            if (teto != null) {
                out.add("teto=" + (teto.bloqueante() ? "bloqueante" : teto.alerta() ? "alerta" : "ok"));
            }
            if (distribuicao != null) {
                out.add("distribuicaoAutomatica=" + distribuicao.distribuicaoAutomatica());
            }
            if (territorial != null) {
                out.add("territorial=" + (territorial.bloqueante() ? "bloqueante" : territorial.alerta() ? "alerta" : "ok"));
            }
            if (decision != null) {
                out.add("transition=" + decision.transitionKey());
            }
            if (!reasons.isEmpty()) {
                out.add("reasons=" + String.join(", ", reasons));
            }
            return String.join(" | ", out);
        }
    }
}
