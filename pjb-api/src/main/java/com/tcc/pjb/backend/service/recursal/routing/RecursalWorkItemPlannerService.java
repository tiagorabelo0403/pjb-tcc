package com.tcc.pjb.backend.service.recursal.routing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalFactType;
import com.tcc.pjb.backend.core.kernel.recursal.model.AppealFiledPayload;
import com.tcc.pjb.backend.core.kernel.recursal.model.AutuationPayload;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.kernel.recursal.plan.ProceedingUpsert;
import com.tcc.pjb.backend.core.kernel.recursal.plan.RecursalPlan;
import com.tcc.pjb.backend.core.kernel.recursal.plan.WorkItemDirective;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.casefile.CaseFile;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceeding;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.CaseFileRepository;
import com.tcc.pjb.backend.model.repository.CaseProceedingRepository;

@Service
public class RecursalWorkItemPlannerService {

    private final CaseFileRepository caseFileRepository;
    private final CaseProceedingRepository proceedingRepository;
    private final RecursalCabinetAllocator cabinetAllocator;
    private final RecursalRoutingProperties properties;

    public RecursalWorkItemPlannerService(CaseFileRepository caseFileRepository,
                                          CaseProceedingRepository proceedingRepository,
                                          RecursalCabinetAllocator cabinetAllocator,
                                          RecursalRoutingProperties properties) {
        this.caseFileRepository = caseFileRepository;
        this.proceedingRepository = proceedingRepository;
        this.cabinetAllocator = cabinetAllocator;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<WorkItemSpec> plan(Processo processo, CanonicalFact fact, RecursalPlan plan) {
        Objects.requireNonNull(processo, "processo é obrigatório");
        Objects.requireNonNull(fact, "fact é obrigatório");

        String originCourt = resolveOriginCourt(processo);
        RitoProcessual rito = processo.getRito();
        Long caseFileId = resolveCaseFileId(processo.getId());
        NivelSigilo secrecy = processo.getNivelSigilo() != null ? processo.getNivelSigilo() : NivelSigilo.PUBLICO;

        List<WorkItemSpec> out = new ArrayList<>();
        Set<String> titleDedup = new HashSet<>();

        if (plan != null && plan.workItems() != null && !plan.workItems().isEmpty()) {
            for (WorkItemDirective directive : plan.workItems()) {
                if (directive == null || directive.title() == null || directive.title().isBlank()) {
                    continue;
                }
                WorkItemSpec spec = convertDirective(directive, processo, fact, plan, originCourt, rito, secrecy);
                push(out, titleDedup, spec);
            }
        }

        if (fact.type() == RecursalFactType.APPEAL_FILED && fact.payload() instanceof AppealFiledPayload payload) {
            addAppealFiledTasks(out, titleDedup, processo, payload, plan, originCourt, rito, caseFileId, secrecy);
        } else if (fact.type() == RecursalFactType.AUTUATED_IN_TARGET && fact.payload() instanceof AutuationPayload payload) {
            addAutuationTasks(out, titleDedup, processo, payload, originCourt, caseFileId, secrecy);
        }

        return List.copyOf(out);
    }

    private void addAppealFiledTasks(List<WorkItemSpec> out,
                                     Set<String> titleDedup,
                                     Processo processo,
                                     AppealFiledPayload payload,
                                     RecursalPlan plan,
                                     String originCourt,
                                     RitoProcessual rito,
                                     Long caseFileId,
                                     NivelSigilo secrecy) {
        LegalAppealType appeal = payload.appealType();
        boolean incident = appeal == LegalAppealType.EMBARGOS_DECLARACAO
                || appeal == LegalAppealType.AGRAVO_INTERNO
                || appeal == LegalAppealType.AGRAVO_REGIMENTAL
                || appeal == LegalAppealType.CORREICAO_PARCIAL;
        boolean apartadoDependencia = appeal == LegalAppealType.EMBARGOS_EXECUCAO
                || appeal == LegalAppealType.EMBARGOS_EXECUCAO_FISCAL
                || appeal == LegalAppealType.EMBARGOS_TERCEIRO;
        boolean urgent = titleSuggestsUrgency(payload.notes()) || appeal == LegalAppealType.HABEAS_CORPUS;

        if (incident) {
            String queueCode = queue("1G", originCourt, "INCIDENTES", null);
            WorkItemSpec wi = spec(
                    queueCode,
                    "Incidente recursal (mesmos autos): " + safeAppeal(appeal),
                    payload.notes(),
                    TipoUsuario.JUIZ,
                    2,
                    false,
                    processo,
                    InstanceLevel.FIRST_INSTANCE,
                    secrecy,
                    urgent
            );
            push(out, titleDedup, wi);
            return;
        }

        if (apartadoDependencia) {
            String queueCode = queue("1G", originCourt, "APARTADOS_DEPENDENCIA", null);
            WorkItemSpec wi = spec(
                    queueCode,
                    "Autuação por dependência: " + safeAppeal(appeal),
                    payload.notes(),
                    TipoUsuario.SERVIDOR_FORUM,
                    2,
                    false,
                    processo,
                    InstanceLevel.FIRST_INSTANCE,
                    secrecy,
                    urgent
            );
            push(out, titleDedup, wi);
            return;
        }

        if (properties.isEmitOriginSecretaryWorkItemOnAppealFiled()) {
            String lane = properties.resolveOriginSecretaryLane(rito, appeal);
            String queueCode = queue("1G", originCourt, lane, null);
            WorkItemSpec wi = spec(
                    queueCode,
                    "Origem: contrarrazões/remessa do recurso (" + safeAppeal(appeal) + ")",
                    protocolDescription(payload.protocolNumber(), payload.notes()),
                    TipoUsuario.SERVIDOR_FORUM,
                    null,
                    true,
                    processo,
                    InstanceLevel.FIRST_INSTANCE,
                    secrecy,
                    urgent
            );
            push(out, titleDedup, wi);
        }

        if (properties.isEmitTargetTriageWorkItemOnAppealFiled()) {
            Target target = resolveTargetFromPlanOrPayload(plan, payload);
            String targetCourt = target.court != null && !target.court.isBlank() ? target.court : originCourt;
            String instTag = instanceTag(target.instance);
            String queueCode = queue(instTag, targetCourt, laneForAppealTarget(appeal, rito), null);
            WorkItemSpec dest = spec(
                    queueCode,
                    "Destino: triagem/distribuição recursal (" + safeAppeal(appeal) + ")",
                    "Shadow node previsto para " + instTag + " (" + targetCourt + "). " + safe(payload.notes()),
                    TipoUsuario.SERVIDOR_FORUM,
                    2,
                    false,
                    processo,
                    target.instance,
                    secrecy,
                    urgent
            );
            push(out, titleDedup, dest);
        }

        if (appeal == LegalAppealType.RESP || appeal == LegalAppealType.RE || appeal == LegalAppealType.AGRAVO_RESP_RE) {
            String queueCode = queue("2G", originCourt, "ADMISSIBILIDADE", null);
            WorkItemSpec admiss = spec(
                    queueCode,
                    "Origem: juízo de admissibilidade (REsp/RE)",
                    safe(payload.notes()),
                    TipoUsuario.DESEMBARGADOR,
                    2,
                    true,
                    processo,
                    InstanceLevel.SECOND_INSTANCE,
                    secrecy,
                    urgent
            );
            push(out, titleDedup, admiss);
        }

        if (requiresRestrictedDesk(secrecy)) {
            String queueCode = queue("1G", originCourt, "SIGILO_RECURSAL", null);
            WorkItemSpec secrecySpec = spec(
                    queueCode,
                    "Origem: validar sigilo e credenciais do fluxo recursal",
                    "Nível efetivo de sigilo: " + secrecy.name(),
                    TipoUsuario.SERVIDOR_FORUM,
                    2,
                    true,
                    processo,
                    InstanceLevel.FIRST_INSTANCE,
                    secrecy,
                    true
            );
            push(out, titleDedup, secrecySpec);
        }

        if (appeal == LegalAppealType.HABEAS_CORPUS || (rito != null && rito.isPenal())) {
            String queueCode = queue("2G", originCourt, "VISTA_MP", null);
            WorkItemSpec vistaMp = spec(
                    queueCode,
                    "Destino: vista ministerial recursal",
                    safe(payload.notes()),
                    TipoUsuario.MEMBRO_MINISTERIO_PUBLICO,
                    2,
                    false,
                    processo,
                    InstanceLevel.SECOND_INSTANCE,
                    secrecy,
                    urgent
            );
            push(out, titleDedup, vistaMp);
        }

        if (rito != null && rito.name().contains("JUIZADO")) {
            String queueCode = queue("2G", originCourt, "TURMA_RECURSAL", null);
            WorkItemSpec turma = spec(
                    queueCode,
                    "Destino: preparação para turma recursal",
                    safe(payload.notes()),
                    TipoUsuario.SERVIDOR_FORUM,
                    2,
                    false,
                    processo,
                    InstanceLevel.SECOND_INSTANCE,
                    secrecy,
                    urgent
            );
            push(out, titleDedup, turma);
        }

        if (rito != null && rito.isEleitoral()) {
            String queueCode = queue("2G", originCourt, "SECRETARIA_ELEITORAL_RECURSAL", null);
            WorkItemSpec eleitoral = spec(
                    queueCode,
                    "Destino: secretaria eleitoral recursal",
                    safe(payload.notes()),
                    TipoUsuario.SERVIDOR_FORUM,
                    2,
                    true,
                    processo,
                    InstanceLevel.SECOND_INSTANCE,
                    secrecy,
                    true
            );
            push(out, titleDedup, eleitoral);
        }
    }

    private void addAutuationTasks(List<WorkItemSpec> out,
                                   Set<String> titleDedup,
                                   Processo processo,
                                   AutuationPayload payload,
                                   String originCourt,
                                   Long caseFileId,
                                   NivelSigilo secrecy) {
        String court = payload.targetCourt() != null && !payload.targetCourt().isBlank() ? payload.targetCourt() : originCourt;
        String instTag = instanceTag(payload.targetInstance());
        String proceedingNumber = resolveDestinationProceedingNumber(payload, processo);
        boolean urgent = titleSuggestsUrgency(proceedingNumber);

        String distQueue = queue(instTag, court, "DISTRIBUICAO", null);
        WorkItemSpec dist = spec(
                distQueue,
                "Destino: distribuir/autuar e vincular prevenção",
                proceedingNumber != null ? ("Autos: " + proceedingNumber) : null,
                TipoUsuario.SERVIDOR_FORUM,
                2,
                false,
                processo,
                payload.targetInstance(),
                secrecy,
                urgent
        );
        push(out, titleDedup, dist);

        RecursalCabinetAllocator.CabinetSlot slot = cabinetAllocator.allocate(
                court,
                caseFileId != null ? caseFileId : processo.getId(),
                instTag,
                proceedingNumber
        );
        String cabinetQueue = queue(instTag, court, "GAB_PREV", "BAND_" + slot.chamberBand() + ":SLOT_" + slot.slot());
        TipoUsuario magistrateRole = resolveTargetMagistrateRole(payload.targetInstance());
        WorkItemSpec cab = spec(
                cabinetQueue,
                "Destino: gabinete prevento virtual (" + slot.descriptor() + ")",
                "Inbox determinístico para prevenção e câmara virtual.",
                magistrateRole,
                3,
                false,
                processo,
                payload.targetInstance(),
                secrecy,
                urgent
        );
        push(out, titleDedup, cab);

        WorkItemSpec assessor = spec(
                queue(instTag, court, "ASSESSORIA_GABINETE", null),
                "Destino: assessoria de gabinete recursal",
                proceedingNumber != null ? "Preparar análise inicial do feito recursal: " + proceedingNumber : "Preparar análise inicial do feito recursal.",
                resolveAssessorRole(payload.targetInstance()),
                3,
                false,
                processo,
                payload.targetInstance(),
                secrecy,
                urgent
        );
        push(out, titleDedup, assessor);

        if (requiresRestrictedDesk(secrecy)) {
            WorkItemSpec restricted = spec(
                    queue(instTag, court, "SIGILO_RECURSAL", null),
                    "Destino: saneamento de credenciais e sigilo do órgão ad quem",
                    "Sigilo efetivo: " + secrecy.name(),
                    TipoUsuario.SERVIDOR_FORUM,
                    2,
                    true,
                    processo,
                    payload.targetInstance(),
                    secrecy,
                    true
            );
            push(out, titleDedup, restricted);
        }
    }

    private String resolveDestinationProceedingNumber(AutuationPayload payload, Processo processo) {
        String targetNumber = payload.targetProceedingNumber();
        if (targetNumber != null && !targetNumber.isBlank()) {
            return targetNumber;
        }
        return processo != null ? processo.getNumeroUnificado() : null;
    }

    private WorkItemSpec convertDirective(WorkItemDirective directive,
                                          Processo processo,
                                          CanonicalFact fact,
                                          RecursalPlan plan,
                                          String originCourt,
                                          RitoProcessual rito,
                                          NivelSigilo secrecy) {
        String baseQueue = directive.queue();
        String title = directive.title() != null ? directive.title().trim() : "";
        String description = directive.description();

        if (baseQueue != null && baseQueue.trim().toUpperCase(Locale.ROOT).startsWith("REC:")) {
            String queueCode = normalize(baseQueue);
            return spec(
                    queueCode,
                    title,
                    description,
                    inferRole(queueCode, title, null),
                    null,
                    false,
                    processo,
                    resolveInstance(queueCode),
                    secrecy,
                    titleSuggestsUrgency(title),
                    directive.dueDate()
            );
        }

        String normalizedToken = normalize(baseQueue);
        if (normalizedToken.contains("INCIDENT")) {
            String queueCode = queue("1G", originCourt, "INCIDENTES", null);
            return spec(queueCode, title, description, TipoUsuario.JUIZ, 2, false, processo, InstanceLevel.FIRST_INSTANCE, secrecy, titleSuggestsUrgency(title), directive.dueDate());
        }

        if (fact.type() == RecursalFactType.APPEAL_FILED && fact.payload() instanceof AppealFiledPayload payload) {
            Target target = resolveTargetFromPlanOrPayload(plan, payload);
            String targetCourt = target.court != null && !target.court.isBlank() ? target.court : originCourt;
            String queueCode = queue(instanceTag(target.instance), targetCourt, laneForAppealTarget(payload.appealType(), rito), null);
            return spec(queueCode, title, description, TipoUsuario.SERVIDOR_FORUM, null, false, processo, target.instance, secrecy, titleSuggestsUrgency(title), directive.dueDate());
        }

        String queueCode = queue("1G", originCourt, "ACOMPANHAR", null);
        return spec(queueCode, title, description, TipoUsuario.SERVIDOR_FORUM, null, false, processo, InstanceLevel.FIRST_INSTANCE, secrecy, titleSuggestsUrgency(title), directive.dueDate());
    }

    private WorkItemSpec spec(String queueCode,
                              String title,
                              String description,
                              TipoUsuario explicitRole,
                              Integer explicitPriority,
                              boolean blocking,
                              Processo processo,
                              InstanceLevel targetInstance,
                              NivelSigilo secrecy,
                              boolean urgent) {
        return spec(queueCode, title, description, explicitRole, explicitPriority, blocking, processo, targetInstance, secrecy, urgent, null);
    }

    private WorkItemSpec spec(String queueCode,
                              String title,
                              String description,
                              TipoUsuario explicitRole,
                              Integer explicitPriority,
                              boolean blocking,
                              Processo processo,
                              InstanceLevel targetInstance,
                              NivelSigilo secrecy,
                              boolean urgent,
                              LocalDate dueDateOverride) {
        String normalizedQueue = normalize(queueCode);
        String lane = laneFromQueue(normalizedQueue);
        TipoUsuario assignedRole = explicitRole != null ? explicitRole : inferRole(normalizedQueue, title, targetInstance);
        boolean upperCourt = targetInstance == InstanceLevel.SUPERIOR || targetInstance == InstanceLevel.EXTRAORDINARY;
        Integer priority = properties.resolvePriority(lane, explicitPriority, urgent, blocking, requiresRestrictedDesk(secrecy), upperCourt);
        LocalDate dueDate = dueDateOverride != null ? dueDateOverride : LocalDate.now().plusDays(properties.resolveDueDays(lane, urgent, blocking, upperCourt));
        String inboxKey = buildInboxKey(normalizedQueue, title, targetInstance);
        return new WorkItemSpec(normalizedQueue, inboxKey, title, description, dueDate, assignedRole, priority, blocking);
    }

    private static void push(List<WorkItemSpec> out, Set<String> titleDedup, WorkItemSpec spec) {
        if (spec == null || spec.title() == null || spec.title().isBlank()) {
            return;
        }
        String key = spec.routingFingerprint();
        if (titleDedup.add(key)) {
            out.add(spec);
        }
    }

    private String buildInboxKey(String queueCode, String title, InstanceLevel targetInstance) {
        if (queueCode == null || queueCode.isBlank()) {
            return null;
        }
        if (queueCode.contains(":GAB_PREV:")) {
            return queueCode.replace("REC:", "REC:INBOX:");
        }
        String[] parts = queueCode.split(":");
        if (parts.length >= 4) {
            return "REC:INBOX:" + parts[1] + ":" + parts[2] + ":" + parts[3];
        }
        InstanceLevel effectiveInstance = targetInstance != null ? targetInstance : InstanceLevel.FIRST_INSTANCE;
        return "REC:INBOX:" + instanceTag(effectiveInstance) + ":" + normalize(title);
    }

    private static TipoUsuario inferRole(String queueCode, String title, InstanceLevel instance) {
        String value = normalize(queueCode) + " " + normalize(title);
        if (value.contains("MINIST") || value.contains(":STF:") || value.contains(":STJ:")) {
            return TipoUsuario.MINISTRO;
        }
        if (value.contains("ASSESSORIA")) {
            if (instance == InstanceLevel.EXTRAORDINARY || instance == InstanceLevel.SUPERIOR) {
                return TipoUsuario.ASSESSOR_MINISTRO;
            }
            if (instance == InstanceLevel.SECOND_INSTANCE) {
                return TipoUsuario.ASSESSOR_DESEMBARGADOR;
            }
            return TipoUsuario.ASSESSOR_JUDICIAL;
        }
        if (value.contains("DESEMB") || value.contains(":2G:") || value.contains("TURMA") || value.contains("CAMARA")) {
            return TipoUsuario.DESEMBARGADOR;
        }
        if (value.contains("JUIZ") || value.contains(":1G:")) {
            return TipoUsuario.JUIZ;
        }
        if (value.contains("MINISTERIO_PUBLICO") || value.contains("PROMOTOR") || value.contains("PROCURADOR")) {
            return TipoUsuario.MEMBRO_MINISTERIO_PUBLICO;
        }
        if (value.contains("DEFENSOR") || value.contains("DPU") || value.contains("DPE")) {
            return TipoUsuario.DEFENSOR_PUBLICO;
        }
        if (value.contains("ADVOG")) {
            return TipoUsuario.ADVOGADO;
        }
        return TipoUsuario.SERVIDOR_FORUM;
    }

    private static TipoUsuario resolveTargetMagistrateRole(InstanceLevel instance) {
        if (instance == null) {
            return TipoUsuario.DESEMBARGADOR;
        }
        return switch (instance) {
            case SUPERIOR, EXTRAORDINARY -> TipoUsuario.MINISTRO;
            case SECOND_INSTANCE -> TipoUsuario.DESEMBARGADOR;
            default -> TipoUsuario.JUIZ;
        };
    }

    private static TipoUsuario resolveAssessorRole(InstanceLevel instance) {
        if (instance == null) {
            return TipoUsuario.ASSESSOR_JUDICIAL;
        }
        return switch (instance) {
            case SUPERIOR, EXTRAORDINARY -> TipoUsuario.ASSESSOR_MINISTRO;
            case SECOND_INSTANCE -> TipoUsuario.ASSESSOR_DESEMBARGADOR;
            default -> TipoUsuario.ASSESSOR_JUDICIAL;
        };
    }

    private static String laneForAppealTarget(LegalAppealType appeal, RitoProcessual rito) {
        if (appeal == LegalAppealType.RESP || appeal == LegalAppealType.RE || appeal == LegalAppealType.AGRAVO_RESP_RE) {
            return "TRIAGEM_SUPERIOR";
        }
        if (appeal == LegalAppealType.HABEAS_CORPUS || (rito != null && rito.isPenal())) {
            return "TRIAGEM_PENAL";
        }
        if (rito != null && rito.isEleitoral()) {
            return "TRIAGEM_ELEITORAL";
        }
        if (rito != null && rito.isMilitar()) {
            return "TRIAGEM_MILITAR";
        }
        return "TRIAGEM";
    }

    private static String protocolDescription(String protocolNumber, String notes) {
        if (protocolNumber == null || protocolNumber.isBlank()) {
            return notes;
        }
        String safeNotes = notes == null ? "" : notes;
        return ("Protocolo: " + protocolNumber + ". " + safeNotes).trim();
    }

    private static boolean requiresRestrictedDesk(NivelSigilo secrecy) {
        return secrecy != null && secrecy.exigeCredencial();
    }

    private static boolean titleSuggestsUrgency(String value) {
        String normalized = normalize(value);
        return normalized.contains("URGENTE") || normalized.contains("LIMINAR") || normalized.contains("PLANTAO") || normalized.contains("HC");
    }

    private static String queue(String instTag, String court, String lane, String suffix) {
        String i = normalize(instTag);
        String c = normalize(court);
        String l = normalize(lane);
        String s = normalize(suffix);
        String base = "REC:" + (i.isBlank() ? "1G" : i) + ":" + (c.isBlank() ? "UNKNOWN" : c) + ":" + (l.isBlank() ? "ACOMPANHAR" : l);
        return s.isBlank() ? base : (base + ":" + s);
    }

    private static String laneFromQueue(String queueCode) {
        String[] parts = queueCode == null ? new String[0] : queueCode.split(":");
        return parts.length > 3 ? parts[3] : "ACOMPANHAR";
    }

    private static Target resolveTargetFromPlanOrPayload(RecursalPlan plan, AppealFiledPayload payload) {
        if (plan != null && plan.proceedings() != null) {
            for (ProceedingUpsert proceeding : plan.proceedings()) {
                if (proceeding == null || !proceeding.shadow() || proceeding.instanceLevel() == null) {
                    continue;
                }
                return new Target(proceeding.instanceLevel(), proceeding.court());
            }
        }
        InstanceLevel hint = payload.targetInstanceHint();
        InstanceLevel resolved = hint != null ? hint : resolveByAppeal(payload.appealType());
        return new Target(resolved, payload.targetCourtHint());
    }

    private static InstanceLevel resolveByAppeal(LegalAppealType appeal) {
        if (appeal == null) {
            return InstanceLevel.FIRST_INSTANCE;
        }
        return switch (appeal) {
            case APELACAO,
                    APELACAO_PENAL,
                    AGRAVO_INSTRUMENTO,
                    AGRAVO_INTERNO,
                    EMBARGOS_DECLARACAO,
                    EMBARGOS_INFRINGENTES,
                    RECURSO_INOMINADO,
                    PEDIDO_UNIFORMIZACAO,
                    RESE,
                    HABEAS_CORPUS -> InstanceLevel.SECOND_INSTANCE;
            case RESP,
                    AGRAVO_RESP_RE -> InstanceLevel.SUPERIOR;
            case RE -> InstanceLevel.EXTRAORDINARY;
            default -> InstanceLevel.FIRST_INSTANCE;
        };
    }

    private static String instanceTag(InstanceLevel instance) {
        if (instance == null) {
            return "1G";
        }
        return switch (instance) {
            case FIRST_INSTANCE -> "1G";
            case SECOND_INSTANCE -> "2G";
            case SUPERIOR -> "STJ";
            case EXTRAORDINARY -> "STF";
        };
    }

    private static InstanceLevel resolveInstance(String queueCode) {
        String[] parts = queueCode == null ? new String[0] : queueCode.split(":");
        if (parts.length < 2) {
            return InstanceLevel.FIRST_INSTANCE;
        }
        return switch (parts[1]) {
            case "2G" -> InstanceLevel.SECOND_INSTANCE;
            case "STJ" -> InstanceLevel.SUPERIOR;
            case "STF" -> InstanceLevel.EXTRAORDINARY;
            default -> InstanceLevel.FIRST_INSTANCE;
        };
    }

    private Long resolveCaseFileId(Long processoId) {
        CaseFile caseFile = caseFileRepository.findByRootProcessoId(processoId).orElse(null);
        if (caseFile != null) {
            return caseFile.getId();
        }
        CaseProceeding linked = proceedingRepository.findFirstByLinkedProcessoId(processoId).orElse(null);
        return linked != null ? linked.getCaseFileId() : null;
    }

    private static String resolveOriginCourt(Processo processo) {
        Jurisdicao jurisdicao = processo.getJurisdicao();
        if (jurisdicao != null && jurisdicao.getSigla() != null && !jurisdicao.getSigla().isBlank()) {
            return jurisdicao.getSigla().trim();
        }
        String uf = resolveUf(processo);
        return uf.isBlank() ? "UNKNOWN" : ("COURT_" + uf);
    }

    private static String resolveUf(Processo processo) {
        Jurisdicao jurisdicao = processo.getJurisdicao();
        if (jurisdicao != null && jurisdicao.getEstado() != null && !jurisdicao.getEstado().isBlank()) {
            return jurisdicao.getEstado().trim().toUpperCase(Locale.ROOT);
        }
        return "";
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String safeAppeal(LegalAppealType appeal) {
        return appeal == null ? "OUTRO" : appeal.name();
    }

    private record Target(InstanceLevel instance, String court) {
    }
}
