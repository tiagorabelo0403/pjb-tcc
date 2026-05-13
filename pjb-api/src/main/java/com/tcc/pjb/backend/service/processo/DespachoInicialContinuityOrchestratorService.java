package com.tcc.pjb.backend.service.processo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;

@Service
public class DespachoInicialContinuityOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(DespachoInicialContinuityOrchestratorService.class);

    private final WorkItemRepository workItemRepository;
    private final ProcessoSlaJudicialService processoSlaJudicialService;
    private final AuditLedgerService auditLedgerService;

    public DespachoInicialContinuityOrchestratorService(WorkItemRepository workItemRepository,
                                                        ProcessoSlaJudicialService processoSlaJudicialService,
                                                        AuditLedgerService auditLedgerService) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.processoSlaJudicialService = Objects.requireNonNull(processoSlaJudicialService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public InitialDispatchFollowUpResponse onDespachoInicialAssinado(Processo processo,
                                                                     Usuario magistrado,
                                                                     String conteudo,
                                                                     String fundamentacao) {
        if (processo == null || processo.getId() == null) {
            return new InitialDispatchFollowUpResponse("SEM_PROCESSO", null, 0, List.of(), List.of());
        }
        ProcessoSlaJudicialService.ProcessoSlaSnapshot sla = processoSlaJudicialService.snapshot(processo);
        int resolvedCount = closeOpenInitialQueueItems(processo.getId());
        MarchaInicialProfile profile = classify(processo, conteudo, fundamentacao, sla);
        WorkItem nextItem = upsertFollowUpWorkItem(processo, magistrado, profile, sla);
        appendAudit(processo, magistrado, profile, sla, nextItem, resolvedCount);
        return new InitialDispatchFollowUpResponse(
                profile.code(),
                nextItem != null ? nextItem.getId() : null,
                resolvedCount,
                profile.reasons(),
                sla.evidencias()
        );
    }

    private int closeOpenInitialQueueItems(Long processoId) {
        List<WorkItem> items = workItemRepository.findAllByProcesso(processoId);
        int changed = 0;
        for (WorkItem item : items) {
            if (item == null || item.getStatus() == WorkItemStatus.CONCLUIDO || item.getStatus() == WorkItemStatus.CANCELADO) {
                continue;
            }
            if (isInitialQueueItem(item)) {
                item.setStatus(WorkItemStatus.CONCLUIDO);
                changed++;
            }
        }
        if (changed > 0) {
            workItemRepository.saveAll(items);
        }
        return changed;
    }

    private boolean isInitialQueueItem(WorkItem item) {
        String templateCode = normalize(item.getTemplateCode());
        String queueCode = normalize(item.getQueueCode());
        return startsWith(templateCode, "AJUIZAMENTO:REVISAO_SECRETARIA:")
                || startsWith(templateCode, "AJUIZAMENTO:CONCLUSAO_INICIAL:")
                || startsWith(templateCode, "CONCLUSAO:DESPACHO_INICIAL:")
                || equalsAny(queueCode, "SECRETARIA_AJUIZAMENTO_INICIAL", "GABINETE_AJUIZAMENTO_INICIAL", "SECRETARIA_CONCLUSO");
    }

    private MarchaInicialProfile classify(Processo processo,
                                          String conteudo,
                                          String fundamentacao,
                                          ProcessoSlaJudicialService.ProcessoSlaSnapshot sla) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        MarchaInicialAxis axis;
        if (requiresRestrictedCommunication(processo)) {
            axis = MarchaInicialAxis.COMUNICACAO_RESTRITA;
            reasons.add("Sigilo reforçado exige trilha controlada de comunicação inicial e conferência de credenciais.");
        } else if (missingDefendantQualification(processo)) {
            axis = MarchaInicialAxis.SANEAMENTO_POLO_PASSIVO;
            reasons.add("Polo passivo sem qualificação mínima para citação segura.");
        } else if (requiresUrgentIntimation(processo, conteudo, fundamentacao)) {
            axis = MarchaInicialAxis.INTIMACAO_PRIORITARIA;
            reasons.add("Caso com traço urgente ou mandamental recomenda intimação prioritária e não mera fila ordinária.");
        } else {
            axis = MarchaInicialAxis.CITACAO_INICIAL;
            reasons.add("Despacho inicial apto a desencadear preparo de citação do polo passivo com SLA do tribunal.");
        }
        reasons.add("Prazo de despacho inicial parametrizado em " + sla.prazoDespachoInicialDiasUteis() + " dia(s) útil(eis).");
        reasons.add("Prazo de citação parametrizado em " + sla.prazoCitacaoDiasUteis() + " dia(s) útil(eis).");
        return new MarchaInicialProfile(axis, List.copyOf(reasons));
    }

    private WorkItem upsertFollowUpWorkItem(Processo processo,
                                            Usuario magistrado,
                                            MarchaInicialProfile profile,
                                            ProcessoSlaJudicialService.ProcessoSlaSnapshot sla) {
        String templateCode = "DESPACHO_INICIAL:FOLLOWUP:" + profile.code() + ":" + processo.getId();
        WorkItem item = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processo.getId(), templateCode, WorkItemStatus.CANCELADO)
                .orElseGet(() -> WorkItem.builder().processo(processo).templateCode(templateCode).build());
        item.setFaseOrigem(processo.getFaseAtual());
        item.setType(profile.workItemType());
        item.setTitulo(profile.title(processo));
        item.setDescricao(profile.description(processo, sla));
        item.setQueueCode(profile.queueCode());
        item.setInboxKey(profile.inboxKey());
        item.setAssignedRole(TipoUsuario.SERVIDOR_FORUM);
        item.setStatus(WorkItemStatus.PENDENTE);
        item.setPrioridade(profile.priority());
        item.setBlocking(profile.blocking());
        item.setUf(firstNonBlank(processo.getUf(), magistrado != null ? magistrado.getUf() : null));
        item.setComarca(firstNonBlank(processo.getComarca(), magistrado != null ? magistrado.getComarca() : null));
        item.setBaseLegal(profile.legalBasis());
        item.setDueAt(profile.resolveDueAt(sla));
        return workItemRepository.save(item);
    }

    private void appendAudit(Processo processo,
                             Usuario magistrado,
                             MarchaInicialProfile profile,
                             ProcessoSlaJudicialService.ProcessoSlaSnapshot sla,
                             WorkItem nextItem,
                             int resolvedCount) {
        try {
            LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
            payload.put("profile", profile.code());
            payload.put("resolvedInitialItems", resolvedCount);
            payload.put("nextQueue", profile.queueCode());
            payload.put("nextInbox", profile.inboxKey());
            payload.put("nextWorkItemId", nextItem != null ? nextItem.getId() : null);
            payload.put("prioridade", profile.priority());
            payload.put("blocking", profile.blocking());
            payload.put("reasons", profile.reasons());
            payload.put("sla", sla.evidencias());
            payload.put("magistradoId", magistrado != null ? magistrado.getId() : null);
            auditLedgerService.appendSafely(
                    "DESPACHO_INICIAL_CONTINUIDADE",
                    "DESPACHO_INICIAL",
                    String.valueOf(processo.getId()),
                    profile.code() + ":" + processo.getId(),
                    payload.toString()
            );
        } catch (Exception ex) {
            log.warn("Falha nao bloqueante ao auditar continuidade do despacho inicial. processoId={} erro={}", processo.getId(), ex.getMessage());
        }
    }

    private boolean requiresRestrictedCommunication(Processo processo) {
        NivelSigilo nivelSigilo = processo.getNivelSigilo();
        return nivelSigilo != null && nivelSigilo.exigeCredencial() && nivelSigilo.nivel() >= NivelSigilo.SIGILO_N4.nivel();
    }

    private boolean missingDefendantQualification(Processo processo) {
        return isBlank(processo.getParteReuNome()) && isBlank(processo.getParteReuCpf());
    }

    private boolean requiresUrgentIntimation(Processo processo, String conteudo, String fundamentacao) {
        String corpus = String.join(" ", List.of(
                safe(processo.getClasseProcessual()),
                safe(processo.getAssunto()),
                safe(processo.getObjetoProcessual()),
                safe(conteudo),
                safe(fundamentacao),
                processo.getRito() != null ? processo.getRito().name() : ""
        )).toUpperCase(Locale.ROOT);
        if (processo.getRito() != null && (processo.getRito().isEspecialConstitucional() || processo.getRito().isPenal())) {
            return true;
        }
        return containsAny(corpus,
                "MANDADO_DE_SEGURANCA",
                "HABEAS_CORPUS",
                "MEDIDA PROTETIVA",
                "MARIA_DA_PENHA",
                "TUTELA_URGENTE",
                "LIMINAR",
                "AUTORIDADE COATORA");
    }

    private static boolean containsAny(String corpus, String... needles) {
        if (corpus == null || corpus.isBlank() || needles == null) {
            return false;
        }
        for (String needle : needles) {
            String normalizedNeedle = normalize(needle);
            if (normalizedNeedle != null && corpus.contains(normalizedNeedle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWith(String value, String prefix) {
        String normalizedValue = normalize(value);
        String normalizedPrefix = normalize(prefix);
        return normalizedValue != null && normalizedPrefix != null && normalizedValue.startsWith(normalizedPrefix);
    }

    private static boolean equalsAny(String value, String... candidates) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null || candidates == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (normalizedValue.equals(normalize(candidate))) {
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
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public record InitialDispatchFollowUpResponse(
            String profileCode,
            Long nextWorkItemId,
            int resolvedInitialItems,
            List<String> reasons,
            List<String> slaEvidence
    ) {
        public InitialDispatchFollowUpResponse {
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
            slaEvidence = slaEvidence == null ? List.of() : List.copyOf(slaEvidence);
        }

        public Map<String, Object> asMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("profileCode", profileCode);
            out.put("nextWorkItemId", nextWorkItemId);
            out.put("resolvedInitialItems", resolvedInitialItems);
            out.put("reasons", reasons);
            out.put("slaEvidence", slaEvidence);
            return out;
        }
    }

    private record MarchaInicialProfile(MarchaInicialAxis axis, List<String> reasons) {
        String code() {
            return axis.name();
        }

        WorkItemType workItemType() {
            return axis.workItemType();
        }

        String queueCode() {
            return axis.queueCode();
        }

        String inboxKey() {
            return axis.inboxKey();
        }

        int priority() {
            return axis.priority();
        }

        boolean blocking() {
            return axis.blocking();
        }

        String legalBasis() {
            return axis.legalBasis();
        }

        String title(Processo processo) {
            String numero = firstNonBlank(processo != null ? processo.getNumeroProcesso() : null,
                    processo != null ? processo.getNumeroUnificado() : null,
                    processo != null && processo.getId() != null ? "PJB-" + processo.getId() : "PROCESSO");
            return axis.titlePrefix() + " — " + numero;
        }

        String description(Processo processo, ProcessoSlaJudicialService.ProcessoSlaSnapshot sla) {
            List<String> lines = new ArrayList<>();
            lines.add(axis.primaryInstruction());
            if (processo != null) {
                appendLine(lines, "Classe", processo.getClasseProcessual());
                appendLine(lines, "Assunto", processo.getAssunto());
                appendLine(lines, "Rito", processo.getRito() != null ? processo.getRito().name() : null);
                appendLine(lines, "Unidade", firstNonBlank(processo.getUnidadeJudiciariaCodigo(), processo.getVara()));
                appendLine(lines, "Tribunal", firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal()));
                appendLine(lines, "Réu", firstNonBlank(processo.getParteReuNome(), processo.getParteReuCpf()));
            }
            if (sla != null) {
                lines.add("SLA despacho inicial: " + sla.prazoDespachoInicialDiasUteis() + " dia(s) útil(eis).");
                lines.add("SLA comunicação inicial: " + sla.prazoCitacaoDiasUteis() + " dia(s) útil(eis).");
            }
            if (reasons != null && !reasons.isEmpty()) {
                lines.add("Racional: " + String.join(" | ", reasons));
            }
            lines.add(axis.operationalChecklist());
            return String.join("\n", lines);
        }

        Instant resolveDueAt(ProcessoSlaJudicialService.ProcessoSlaSnapshot sla) {
            return axis.resolveDueAt(sla);
        }

        private static void appendLine(List<String> lines, String label, String value) {
            String normalized = firstNonBlank(value);
            if (normalized != null) {
                lines.add(label + ": " + normalized);
            }
        }
    }

    private enum MarchaInicialAxis {
        CITACAO_INICIAL(
                WorkItemType.CITACAO,
                "SECRETARIA_COMUNICACOES_INICIAIS",
                "SECRETARIA_INTIMACOES",
                1,
                true,
                "Preparar citação inicial do polo passivo",
                "Secretaria deve preparar a comunicação inaugural do réu com base no despacho assinado e na rota institucional disponível.",
                "Conferir endereço/conta institucional, revisar modalidade de citação, validar prazo de resposta e deixar a expedição pronta."
        ),
        INTIMACAO_PRIORITARIA(
                WorkItemType.INTIMACAO,
                "SECRETARIA_COMUNICACOES_PRIORITARIAS",
                "SECRETARIA_INTIMACOES",
                1,
                true,
                "Preparar intimação prioritária pós-despacho",
                "Caso mandamental ou urgente exige intimação prioritária com baixa latência operacional.",
                "Conferir regime de urgência, destinatário institucional ou autoridade coatora e priorizar o cumprimento em fila dedicada."
        ),
        SANEAMENTO_POLO_PASSIVO(
                WorkItemType.DILIGENCIA,
                "SECRETARIA_SANEAMENTO_CITACAO",
                "SECRETARIA_CITACAO_QUALIFICACAO",
                1,
                true,
                "Sanear qualificação do polo passivo",
                "Antes da comunicação inicial, a secretaria deve completar a identificação do polo passivo e o endereço útil de cumprimento.",
                "Conferir CPF/CNPJ, nome completo, endereço, vínculo institucional e eventual necessidade de cadastro complementar."
        ),
        COMUNICACAO_RESTRITA(
                WorkItemType.INTIMACAO,
                "SECRETARIA_SIGILO_COMUNICACAO",
                "SECRETARIA_SIGILO",
                1,
                true,
                "Preparar comunicação inicial em trilha sigilosa",
                "O regime de sigilo reforçado exige preparação da comunicação em fila restrita e conferência de credenciais antes da expedição.",
                "Validar credenciais, canal seguro, destinatário habilitado e preservar a compartimentação do conteúdo sensível."
        );

        private final WorkItemType workItemType;
        private final String queueCode;
        private final String inboxKey;
        private final int priority;
        private final boolean blocking;
        private final String titlePrefix;
        private final String primaryInstruction;
        private final String operationalChecklist;

        MarchaInicialAxis(WorkItemType workItemType,
                          String queueCode,
                          String inboxKey,
                          int priority,
                          boolean blocking,
                          String titlePrefix,
                          String primaryInstruction,
                          String operationalChecklist) {
            this.workItemType = workItemType;
            this.queueCode = queueCode;
            this.inboxKey = inboxKey;
            this.priority = priority;
            this.blocking = blocking;
            this.titlePrefix = titlePrefix;
            this.primaryInstruction = primaryInstruction;
            this.operationalChecklist = operationalChecklist;
        }

        WorkItemType workItemType() {
            return workItemType;
        }

        String queueCode() {
            return queueCode;
        }

        String inboxKey() {
            return inboxKey;
        }

        int priority() {
            return priority;
        }

        boolean blocking() {
            return blocking;
        }

        String titlePrefix() {
            return titlePrefix;
        }

        String primaryInstruction() {
            return primaryInstruction;
        }

        String operationalChecklist() {
            return operationalChecklist;
        }

        String legalBasis() {
            return switch (this) {
                case CITACAO_INICIAL -> "CPC art. 238 e malha nacional de comunicação judicial do PJB.";
                case INTIMACAO_PRIORITARIA -> "Tutela urgente, rito mandamental ou comunicação prioritária conforme o caso concreto.";
                case SANEAMENTO_POLO_PASSIVO -> "Regularidade da formação do polo passivo e validade futura da comunicação judicial.";
                case COMUNICACAO_RESTRITA -> "Sigilo processual reforçado e necessidade de canal controlado de comunicação judicial.";
            };
        }

        Instant resolveDueAt(ProcessoSlaJudicialService.ProcessoSlaSnapshot sla) {
            return switch (this) {
                case CITACAO_INICIAL, SANEAMENTO_POLO_PASSIVO -> sla.dueAtInitialCommunication();
                case INTIMACAO_PRIORITARIA, COMUNICACAO_RESTRITA -> sla.dueAtRestrictedCommunication();
            };
        }
    }
}
