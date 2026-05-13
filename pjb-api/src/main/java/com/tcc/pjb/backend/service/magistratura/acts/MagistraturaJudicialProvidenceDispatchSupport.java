package com.tcc.pjb.backend.service.magistratura.acts;

import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.deterministicKey;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.firstNonBlank;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.label;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.mergeDescription;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.resolveAgendaTrack;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.resolveAttendanceStatus;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.resolveConfirmationStatus;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.safeMap;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.stringValue;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.summarizeReasons;

import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCode;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialProvidenceDispatchResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalAssignmentService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatProcessContactEnvelopeResolver;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MagistraturaJudicialProvidenceDispatchSupport {

    private final WorkItemRepository workItemRepository;
    private final SecretariatQueueProjectionService queueProjectionService;
    private final SecretariatOperationalAssignmentService assignmentService;
    private final SecretariatProcessContactEnvelopeResolver contactEnvelopeResolver;

    public MagistraturaJudicialProvidenceDispatchSupport(WorkItemRepository workItemRepository,
                                                         SecretariatQueueProjectionService queueProjectionService,
                                                         SecretariatOperationalAssignmentService assignmentService,
                                                         SecretariatProcessContactEnvelopeResolver contactEnvelopeResolver) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.queueProjectionService = Objects.requireNonNull(queueProjectionService);
        this.assignmentService = Objects.requireNonNull(assignmentService);
        this.contactEnvelopeResolver = Objects.requireNonNull(contactEnvelopeResolver);
    }

    public List<MagistraturaJudicialProvidenceDispatchResponse> dispatch(Processo processo,
                                                                         Usuario usuario,
                                                                         MagistraturaJudicialActCode action,
                                                                         Map<String, Object> actPayload,
                                                                         SecretariatOperationalRoutingProfile profile,
                                                                         List<ProvidencePlan> plans) {
        List<MagistraturaJudicialProvidenceDispatchResponse> out = new ArrayList<>();
        HashSet<Long> projectedWorkItemIds = new HashSet<>();
        for (ProvidencePlan plan : plans) {
            ReusedWorkItem reused = resolveReusableWorkItem(actPayload, plan);
            WorkItem workItem = reused.workItem() != null
                    ? prepareReusedWorkItem(reused.workItem(), plan)
                    : createWorkItem(processo, usuario, action, plan);
            applySuggestedAssignment(workItem, processo, profile, plan.stageToken());
            if (workItem.getStatus() == null) {
                workItem.setStatus(WorkItemStatus.PENDENTE);
            }
            WorkItem persisted = workItemRepository.save(workItem);
            SecretariatOperationalAssignmentService.AssignmentSnapshot assignment = assignmentService.avaliar(processo, profile, plan.stageToken());
            Long persistedId = persisted.getId();
            boolean shouldProject = persistedId == null || projectedWorkItemIds.add(persistedId);
            if (shouldProject) {
                queueProjectionService.upsert(
                        persisted,
                        computeScore(plan, persisted),
                        computeTags(processo, action, plan),
                        buildQueueMetadata(processo, action, plan, assignment)
                );
            }
            out.add(new MagistraturaJudicialProvidenceDispatchResponse(
                    plan.code(),
                    persisted.getStatus().name(),
                    plan.stageToken(),
                    persisted.getId(),
                    reused.reused(),
                    plan.targetInboxKey(),
                    plan.targetQueueCode(),
                    plan.targetPanelRoute(),
                    persisted.getDueAt(),
                    persisted.getAssignedUser() == null ? null : persisted.getAssignedUser().getId(),
                    persisted.getAssignedUser() == null ? null : persisted.getAssignedUser().getNome(),
                    persisted.getAssignedUser() == null ? null : persisted.getAssignedUser().getEmail(),
                    plan.summary(),
                    contactEnvelopeResolver.participantSnapshots(processo),
                    List.copyOf(plan.reasons()),
                    enrichMetrics(plan.metrics(), processo, plan, assignment)
            ));
        }
        return List.copyOf(out);
    }

    private WorkItem createWorkItem(Processo processo,
                                    Usuario usuario,
                                    MagistraturaJudicialActCode action,
                                    ProvidencePlan plan) {
        return WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(deterministicKey(action, plan.code(), processo.getId(), usuario.getId(), plan.dueAt()))
                .type(plan.workItemType())
                .titulo(buildTitle(processo, action, plan.code()))
                .descricao(plan.summary())
                .queueCode(plan.targetQueueCode())
                .inboxKey(plan.targetInboxKey())
                .assignedRole(TipoUsuario.SERVIDOR_FORUM)
                .status(WorkItemStatus.PENDENTE)
                .prioridade(plan.priority())
                .blocking(plan.blocking())
                .dueAt(plan.dueAt())
                .uf(firstNonBlank(processo.getUf(), usuario.getUf()))
                .comarca(firstNonBlank(processo.getComarca(), usuario.getComarca()))
                .baseLegal(summarizeReasons(plan.reasons()))
                .build();
    }

    private ReusedWorkItem resolveReusableWorkItem(Map<String, Object> payload, ProvidencePlan plan) {
        if (payload == null) {
            return ReusedWorkItem.empty();
        }
        Object raw = payload.get("workItemId");
        if (!(raw instanceof Number number)) {
            return ReusedWorkItem.empty();
        }
        return workItemRepository.findById(number.longValue())
                .filter(workItem -> eligibleForReuse(workItem, plan.workItemType()))
                .map(workItem -> new ReusedWorkItem(workItem, true))
                .orElseGet(ReusedWorkItem::empty);
    }

    private WorkItem prepareReusedWorkItem(WorkItem workItem, ProvidencePlan plan) {
        workItem.setQueueCode(plan.targetQueueCode());
        workItem.setInboxKey(plan.targetInboxKey());
        workItem.setAssignedRole(TipoUsuario.SERVIDOR_FORUM);
        if (workItem.getStatus() == null || workItem.getStatus() == WorkItemStatus.CONCLUIDO || workItem.getStatus() == WorkItemStatus.CANCELADO) {
            workItem.setStatus(WorkItemStatus.PENDENTE);
        }
        workItem.setPrioridade(plan.priority());
        workItem.setDueAt(plan.dueAt());
        workItem.setDescricao(mergeDescription(workItem.getDescricao(), plan.summary()));
        workItem.setBaseLegal(mergeDescription(workItem.getBaseLegal(), summarizeReasons(plan.reasons())));
        return workItem;
    }

    private boolean eligibleForReuse(WorkItem workItem, com.tcc.pjb.backend.model.entity.enums.WorkItemType targetType) {
        if (workItem.getId() == null || workItem.getStatus() == WorkItemStatus.CANCELADO) {
            return false;
        }
        if (workItem.getType() == targetType) {
            return true;
        }
        return workItem.getStatus() == WorkItemStatus.PENDENTE || workItem.getStatus() == WorkItemStatus.EM_EXECUCAO;
    }

    private String buildTitle(Processo processo,
                              MagistraturaJudicialActCode action,
                              com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialProvidenceCode code) {
        return label(code) + " — " + firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero(), "PROCESSO") + " — " + action.name();
    }

    private int computeScore(ProvidencePlan plan, WorkItem workItem) {
        int base = switch (plan.code()) {
            case PREPARAR_AUDIENCIA -> 92;
            case EXPEDIR_ORDEM_CUMPRIMENTO, REDISTRIBUIR_OU_PREVENIR -> 90;
            case IMPULSIONAR_EXECUCAO -> 88;
            case PROVIDENCIAR_PUBLICACAO -> 84;
            case EXPEDIR_INTIMACOES, ABRIR_VISTA_TECNICA -> 82;
            case PROVIDENCIAR_PERICIA -> 80;
            case CONTROLAR_CALCULO_LIQUIDACAO -> 78;
            case REMETER_COLEGIADO_OU_PLENARIO -> 76;
            case SANEAR_PROCESSO, PROCESSAR_INCIDENTE_PROCESSUAL -> 72;
            case ORGANIZAR_CONCLUSAO -> 70;
            case CUMPRIR_DETERMINACAO_CARTORIO -> 68;
        };
        if (workItem.isBlocking()) {
            base += 4;
        }
        if (workItem.getDueAt() != null && workItem.getDueAt().isBefore(Instant.now().plus(Duration.ofHours(6)))) {
            base += 4;
        }
        if (plan.priority() == 1) {
            base += 2;
        }
        return Math.min(100, base);
    }

    private List<String> computeTags(Processo processo,
                                     MagistraturaJudicialActCode action,
                                     ProvidencePlan plan) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add("MAGISTRATURA");
        tags.add(plan.code().name());
        tags.add(action.name());
        tags.add(plan.stageToken());
        if (processo.getRamoDireito() != null) {
            tags.add(processo.getRamoDireito().name());
        }
        if (processo.getTribunalCodigoRoteado() != null) {
            tags.add(processo.getTribunalCodigoRoteado());
        }
        return List.copyOf(tags);
    }

    private void applySuggestedAssignment(WorkItem workItem,
                                          Processo processo,
                                          SecretariatOperationalRoutingProfile profile,
                                          String stageToken) {
        SecretariatOperationalAssignmentService.AssignmentSnapshot snapshot = assignmentService.avaliar(processo, profile, stageToken);
        if (snapshot != null && snapshot.primary() != null) {
            workItem.setAssignedUser(snapshot.primary());
            workItem.setAssignedRole(snapshot.primary().getTipoUsuario() == null ? TipoUsuario.SERVIDOR_FORUM : snapshot.primary().getTipoUsuario());
        }
    }

    private Map<String, Object> buildQueueMetadata(Processo processo,
                                                   MagistraturaJudicialActCode action,
                                                   ProvidencePlan plan,
                                                   SecretariatOperationalAssignmentService.AssignmentSnapshot assignment) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("automationAxis", "MAGISTRATURA_PROVIDENCIAS");
        metadata.put("processoNumero", firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()));
        metadata.put("processoId", processo.getId());
        metadata.put("tribunalCodigo", firstNonBlank(stringValue(plan.metrics().get("authorityTribunalAxis")), processo.getTribunalCodigoRoteado()));
        metadata.put("tribunalNome", processo.getTribunal());
        metadata.put("unidadeJudiciariaCodigo", firstNonBlank(stringValue(plan.metrics().get("routingUnitCode")), processo.getUnidadeJudiciariaCodigo()));
        metadata.put("tipoJustica", processo.getTipoJustica() == null ? null : processo.getTipoJustica().name());
        metadata.put("panelReferenceAt", firstNonBlank(stringValue(plan.metrics().get("hearingAt")), stringValue(plan.metrics().get("dueAt"))));
        metadata.put("panelDateBucket", stringValue(plan.metrics().get("hearingDateBucket")));
        metadata.put("action", action.name());
        metadata.put("providencia", plan.code().name());
        metadata.put("stage", plan.stageToken());
        metadata.put("panelRoute", plan.targetPanelRoute());
        metadata.put("summary", plan.summary());
        metadata.put("participants", contactEnvelopeResolver.participantSnapshots(processo));
        metadata.put("contactEnvelope", contactEnvelopeResolver.buildEnvelope(processo));
        metadata.put("assignment", assignmentSummary(assignment));
        metadata.put("organizationalPath", plan.metrics().get("organizationalPath"));
        metadata.put("routingSecretariatCode", firstNonBlank(stringValue(plan.metrics().get("organizationalPath")), stringValue(plan.metrics().get("routingStageBindingKey")), processo.getUnidadeJudiciariaCodigo()));
        metadata.put("routingSecretariatLabel", firstNonBlank(plan.institutionalOwner(), stringValue(plan.metrics().get("authorityUnitLabel")), stringValue(plan.metrics().get("routingVaraLabel"))));
        metadata.put("routingUnitCode", plan.metrics().get("routingUnitCode"));
        metadata.put("routingStageBindingKey", plan.metrics().get("routingStageBindingKey"));
        metadata.put("routingCellCode", plan.metrics().get("routingCellCode"));
        metadata.put("prioridadeOperacional", plan.priority());
        metadata.put("dependencias", plan.dependencies());
        metadata.put("retornoEsperado", plan.expectedReturn());
        metadata.put("eventoConclusao", plan.completionEvent());
        metadata.put("modoConfirmacao", plan.confirmationMode());
        metadata.put("unidadeAlvo", plan.institutionalOwner());
        metadata.put("authorityScope", plan.metrics().get("authorityScope"));
        metadata.put("authorityAxis", plan.metrics().get("authorityAxis"));
        metadata.put("judgmentAxis", plan.metrics().get("judgmentAxis"));
        metadata.put("authorityPanelRoute", plan.metrics().get("authorityPanelRoute"));
        metadata.put("authorityReturnRoute", plan.metrics().get("authorityReturnRoute"));
        metadata.put("authorityOrgao", plan.metrics().get("authorityOrgao"));
        metadata.put("routingVaraLabel", plan.metrics().get("routingVaraLabel"));
        metadata.put("routingOrgaoLabel", plan.metrics().get("routingOrgaoLabel"));
        metadata.put("routingComarcaLabel", plan.metrics().get("routingComarcaLabel"));
        metadata.put("authorityUnitCode", plan.metrics().get("authorityUnitCode"));
        metadata.put("authorityUnitLabel", plan.metrics().get("authorityUnitLabel"));
        metadata.put("authorityJusticeAxis", plan.metrics().get("authorityJusticeAxis"));
        metadata.put("authorityTribunalAxis", plan.metrics().get("authorityTribunalAxis"));
        metadata.put("authorityClass", plan.metrics().get("authorityClass"));
        metadata.put("authorityInstitutionalPanelCode", plan.metrics().get("authorityInstitutionalPanelCode"));
        metadata.put("authorityInstitutionalLandingPath", plan.metrics().get("authorityInstitutionalLandingPath"));
        metadata.put("hearingAt", plan.metrics().get("hearingAt"));
        metadata.put("dueAt", plan.metrics().get("dueAt"));
        metadata.put("eventTrack", resolveAgendaTrack(plan.code()));
        metadata.put("operationalConfirmationStatus", resolveConfirmationStatus(plan.code()));
        metadata.put("attendanceStatus", resolveAttendanceStatus(plan.code()));
        metadata.put("processReturnRoute", resolveProcessReturnRoute(plan));
        metadata.put("processReturnStatus", resolveProcessReturnStatus(plan));
        metadata.put("autoReturnReady", Boolean.FALSE);
        metadata.put("reuseOperationalMesh", Boolean.TRUE);
        return safeMap(metadata);
    }

    private String resolveProcessReturnRoute(ProvidencePlan plan) {
        return firstNonBlank(
                stringValue(plan.metrics().get("authorityReturnRoute")),
                stringValue(plan.metrics().get("originReturnRoute"))
        );
    }

    private String resolveProcessReturnStatus(ProvidencePlan plan) {
        return resolveProcessReturnRoute(plan) == null ? "NAO_APLICAVEL" : "AGUARDANDO_EVENTO";
    }

    private Map<String, Object> assignmentSummary(SecretariatOperationalAssignmentService.AssignmentSnapshot assignment) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (assignment == null) {
            return Map.of();
        }
        out.put("stage", assignment.stage());
        out.put("cellCode", assignment.cellCode());
        if (assignment.primary() != null) {
            out.put("primaryUserId", assignment.primary().getId());
            out.put("primaryUserName", assignment.primary().getNome());
            out.put("primaryUserEmail", assignment.primary().getEmail());
        }
        if (assignment.backup() != null) {
            out.put("backupUserId", assignment.backup().getId());
            out.put("backupUserName", assignment.backup().getNome());
            out.put("backupUserEmail", assignment.backup().getEmail());
        }
        return safeMap(out);
    }

    private Map<String, Object> enrichMetrics(Map<String, Object> metrics,
                                              Processo processo,
                                              ProvidencePlan plan,
                                              SecretariatOperationalAssignmentService.AssignmentSnapshot assignment) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(safeMap(metrics));
        out.put("panelRoute", plan.targetPanelRoute());
        out.put("stage", plan.stageToken());
        out.put("participantsCount", contactEnvelopeResolver.participantSnapshots(processo).size());
        Map<String, Object> contactEnvelope = contactEnvelopeResolver.buildEnvelope(processo);
        out.put("contactReadyCount", contactEnvelope.get("contactReadyCount"));
        out.put("contactMissingCount", contactEnvelope.get("contactMissingCount"));
        out.put("prioridadeOperacional", plan.priority());
        out.put("dependencias", plan.dependencies());
        out.put("retornoEsperado", plan.expectedReturn());
        out.put("eventoConclusao", plan.completionEvent());
        out.put("modoConfirmacao", plan.confirmationMode());
        out.put("responsavelInstitucional", plan.institutionalOwner());
        out.put("authorityScope", plan.metrics().get("authorityScope"));
        out.put("authorityAxis", plan.metrics().get("authorityAxis"));
        out.put("judgmentAxis", plan.metrics().get("judgmentAxis"));
        out.put("authorityPanelRoute", plan.metrics().get("authorityPanelRoute"));
        out.put("authorityReturnRoute", plan.metrics().get("authorityReturnRoute"));
        out.put("authorityOrgao", plan.metrics().get("authorityOrgao"));
        out.put("authorityUnitCode", plan.metrics().get("authorityUnitCode"));
        out.put("authorityUnitLabel", plan.metrics().get("authorityUnitLabel"));
        out.put("routingUnitCode", plan.metrics().get("routingUnitCode"));
        out.put("routingSnapshotRoute", plan.metrics().get("routingSnapshotRoute"));
        out.put("routingCellCode", plan.metrics().get("routingCellCode"));
        if (assignment != null && assignment.primary() != null) {
            out.put("responsavelNomeadoId", assignment.primary().getId());
            out.put("responsavelNomeado", assignment.primary().getNome());
            out.put("responsavelNomeadoEmail", assignment.primary().getEmail());
        }
        return safeMap(out);
    }
}
