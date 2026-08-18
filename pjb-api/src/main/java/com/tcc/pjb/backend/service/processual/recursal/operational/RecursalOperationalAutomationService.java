package com.tcc.pjb.backend.service.processual.recursal.operational;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.magistratura.acts.MagistraturaAuthorityProjection;
import com.tcc.pjb.backend.service.magistratura.acts.MagistraturaOperationalUnitContext;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalAssignmentService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatProcessContactEnvelopeResolver;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecursalOperationalAutomationService {

    private final SecretariatOperationalRoutingResolver routingResolver;
    private final SecretariatOperationalAssignmentService assignmentService;
    private final SecretariatQueueProjectionService projectionService;
    private final WorkItemRepository workItemRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecretariatProcessContactEnvelopeResolver contactEnvelopeResolver;

    public RecursalOperationalAutomationService(SecretariatOperationalRoutingResolver routingResolver,
                                                SecretariatOperationalAssignmentService assignmentService,
                                                SecretariatQueueProjectionService projectionService,
                                                WorkItemRepository workItemRepository,
                                                UsuarioRepository usuarioRepository,
                                                SecretariatProcessContactEnvelopeResolver contactEnvelopeResolver) {
        this.routingResolver = Objects.requireNonNull(routingResolver);
        this.assignmentService = Objects.requireNonNull(assignmentService);
        this.projectionService = Objects.requireNonNull(projectionService);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.contactEnvelopeResolver = Objects.requireNonNull(contactEnvelopeResolver);
    }

    @Transactional
    public Map<String, Object> materialize(Processo processo,
                                           Usuario actor,
                                           String tipoRecurso,
                                           WorkItem peticaoWorkItem,
                                           WorkItem recursoWorkItem) {
        SecretariatOperationalRoutingProfile routing = routingResolver.resolve(processo);
        String recursoNormalizado = tipoRecurso == null ? "RECURSO" : tipoRecurso.trim().toUpperCase(Locale.ROOT);
        MagistraturaAuthorityProjection authorityProjection = MagistraturaAuthorityProjection.resolveRecursal(processo, actor, routing, "RECEBIMENTO", routing.deskAxis());
        MagistraturaOperationalUnitContext unitContext = MagistraturaOperationalUnitContext.resolve(processo, actor, routing, authorityProjection);
        RelayTarget petitionTarget = relayTarget(
                unitContext,
                "RECEBIMENTO",
                routing.receiptInboxKey(),
                routing.receiptQueueCode(),
                "/api/v1/secretariat/especializada/processos/" + processo.getId() + "/distribuicao-interna",
                List.of("petição protocolada", "peça vinculada ao processo"),
                "recebimento recursal certificado e encaminhado à fila competente",
                "RECURSO_RECEBIDO"
        );
        RelayTarget recursoTarget = resolveRecursalTarget(processo, routing, recursoNormalizado, unitContext);
        authorityProjection = MagistraturaAuthorityProjection.resolveRecursal(processo, actor, routing, recursoTarget.stageToken(), routing.deskAxis());
        unitContext = MagistraturaOperationalUnitContext.resolve(processo, actor, routing, authorityProjection);
        petitionTarget = relayTarget(
                unitContext,
                "RECEBIMENTO",
                routing.receiptInboxKey(),
                routing.receiptQueueCode(),
                "/api/v1/secretariat/especializada/processos/" + processo.getId() + "/distribuicao-interna",
                List.of("petição protocolada", "peça vinculada ao processo"),
                "recebimento recursal certificado e encaminhado à fila competente",
                "RECURSO_RECEBIDO"
        );
        recursoTarget = resolveRecursalTarget(processo, routing, recursoNormalizado, unitContext);

        AssignmentEnvelope petitionEnvelope = project(peticaoWorkItem, processo, actor, routing, petitionTarget, recursoNormalizado, "PETICAO_RECURSAL", authorityProjection, unitContext);
        AssignmentEnvelope recursoEnvelope = project(recursoWorkItem, processo, actor, routing, recursoTarget, recursoNormalizado, "RECURSO", authorityProjection, unitContext);

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("routeKey", routing.routeKey());
        out.put("tribunalCodigo", routing.tribunalCodigo());
        out.put("instanciaAxis", routing.instanciaAxis());
        out.put("tipoRecurso", recursoNormalizado);
        out.put("authorityScope", authorityProjection.scope());
        out.put("authorityAxis", authorityProjection.authorityAxis());
        out.put("judgmentAxis", authorityProjection.judgmentAxis());
        out.put("authorityPanelRoute", authorityProjection.panelRoute());
        out.put("authorityReturnRoute", authorityProjection.returnRoute());
        out.put("authorityOrgao", authorityProjection.orgaoLabel());
        out.put("authorityJusticeAxis", authorityProjection.justiceAxis());
        out.put("authorityTribunalAxis", authorityProjection.tribunalAxis());
        out.put("authorityClass", authorityProjection.authorityClass());
        out.put("authorityInstitutionalPanelCode", authorityProjection.institutionalPanelCode());
        out.put("authorityInstitutionalLandingPath", authorityProjection.institutionalLandingPath());
        out.put("authorityUnitCode", authorityProjection.authorityUnitCode());
        out.put("authorityUnitLabel", authorityProjection.authorityUnitLabel());
        out.put("authorityUnitBindingKey", authorityProjection.authorityUnitBindingKey());
        out.put("routingUnitCode", unitContext.unidadeCodigo());
        out.put("routingSnapshotRoute", unitContext.snapshotRoute());
        out.put("peticao", petitionEnvelope.toMap());
        out.put("recurso", recursoEnvelope.toMap());
        out.put("participants", contactEnvelopeResolver.participantSnapshots(processo));
        out.put("contactEnvelope", contactEnvelopeResolver.buildEnvelope(processo));
        out.put("nextStages", recursoTarget.nextStages());
        out.put("expectedReturn", recursoTarget.expectedReturn());
        out.put("completionEvent", recursoTarget.completionEvent());
        out.put("sessionRoute", resolveSessionRoute(processo, authorityProjection, recursoTarget));
        out.put("acordaoRoute", resolveAcordaoRoute(processo, authorityProjection, recursoTarget));
        out.put("originReturnRoute", resolveOriginReturnRoute(authorityProjection));
        out.put("colegiateComplianceRoute", authorityProjection.processMeshRoute());
        return safeMap(out);
    }

    private AssignmentEnvelope project(WorkItem item,
                                       Processo processo,
                                       Usuario actor,
                                       SecretariatOperationalRoutingProfile routing,
                                       RelayTarget target,
                                       String tipoRecurso,
                                       String axis,
                                       MagistraturaAuthorityProjection authorityProjection,
                                       MagistraturaOperationalUnitContext unitContext) {
        item.setInboxKey(firstNonBlank(target.inboxKey(), item.getInboxKey()));
        item.setQueueCode(firstNonBlank(target.queueCode(), item.getQueueCode()));
        SecretariatOperationalAssignmentService.AssignmentSnapshot assignment = assignmentService.avaliar(processo, routing, target.stageToken());
        if (assignment != null && assignment.primary() != null) {
            item.setAssignedUser(assignment.primary());
            item.setAssignedRole(assignment.primary().getTipoUsuario() == null ? TipoUsuario.SERVIDOR_FORUM : assignment.primary().getTipoUsuario());
        }
        WorkItem persisted = workItemRepository.save(item);
        projectionService.upsert(persisted, computeScore(target.stageToken()), computeTags(processo, tipoRecurso, axis), buildQueueMetadata(processo, actor, target, tipoRecurso, axis, assignment, authorityProjection, unitContext));
        return new AssignmentEnvelope(persisted, target, assignment);
    }

    private RelayTarget resolveRecursalTarget(Processo processo,
                                              SecretariatOperationalRoutingProfile routing,
                                              String tipoRecurso,
                                              MagistraturaOperationalUnitContext unitContext) {
        Map<String, Object> tribunalFlow = tribunalFlow(routing);
        Map<String, Object> queueCodes = nestedMap(tribunalFlow, "queueCodes");
        boolean embargos = containsAny(tipoRecurso, "EMBARGO", "EMBARGOS", "DECLARACAO", "DECLARAÇÃO");
        boolean admissibilidade = containsAny(tipoRecurso,
                "APELACAO", "APELAÇÃO", "AGRAVO", "CONTRARRAZOES", "CONTRARRAZÕES", "CONTRARAZOES", "CONTRARRAZOES", "ESPECIAL", "EXTRAORDINARIO", "EXTRAORDINÁRIO", "INTERNO")
                || queueCodes.containsKey("admissibilidade");
        boolean colegiado = !tribunalFlow.isEmpty();
        if (embargos) {
            return relayTarget(
                    unitContext,
                    "EMBARGOS",
                    firstNonBlank(routing.executionInboxKey(), routing.receiptInboxKey()),
                    firstNonBlank(stringValue(queueCodes.get("embargos")), routing.secretariatCode() + ":EMBARGOS", routing.executionQueueCode()),
                    OperationalApiRoutes.secretariatJulgamentoProcesso(processo.getId()) + "/painel",
                    List.of("recebimento", "embargos", "publicação/acórdão"),
                    "embargos distribuídos ao fluxo colegiado competente e prontos para retorno ao processo de origem",
                    "EMBARGOS_ENCAMINHADOS"
            );
        }
        if (colegiado && admissibilidade) {
            return relayTarget(
                    unitContext,
                    "ADMISSIBILIDADE",
                    firstNonBlank(routing.executionInboxKey(), routing.receiptInboxKey()),
                    firstNonBlank(stringValue(queueCodes.get("admissibilidade")), stringValue(queueCodes.get("gabineteRelator")), routing.executionQueueCode()),
                    "/api/v1/processual/recursal/admissibilidade",
                    List.of("admissibilidade", "contrarrazões", "colegiado", "acórdão"),
                    "recurso qualificado para admissibilidade, contrarrazões e remessa ao colegiado",
                    "ADMISSIBILIDADE_RECURSAL_ABERTA"
            );
        }
        if (colegiado) {
            return relayTarget(
                    unitContext,
                    "COLEGIADO",
                    firstNonBlank(routing.executionInboxKey(), routing.receiptInboxKey()),
                    firstNonBlank(stringValue(queueCodes.get("gabineteRelator")), stringValue(queueCodes.get("sessao")), routing.executionQueueCode()),
                    OperationalApiRoutes.secretariatJulgamentoProcesso(processo.getId()) + "/painel",
                    List.of("colegiado", "pauta", "acórdão", "baixa à origem"),
                    "recurso encaminhado à célula colegiada com retorno monitorado até o acórdão",
                    "RECURSO_ENCAMINHADO_AO_COLEGIADO"
            );
        }
        return relayTarget(
                unitContext,
                "RECEBIMENTO",
                routing.receiptInboxKey(),
                routing.receiptQueueCode(),
                "/api/v1/secretariat/especializada/processos/" + processo.getId() + "/distribuicao-interna",
                List.of("recebimento", "distribuição interna", "retorno ao gabinete"),
                "recurso recebido e reposicionado na trilha operacional existente",
                "RECURSO_RECEBIDO"
        );
    }

    private RelayTarget relayTarget(MagistraturaOperationalUnitContext unitContext,
                                  String stageToken,
                                  String inboxKey,
                                  String queueCode,
                                  String baseRoute,
                                  List<String> nextStages,
                                  String expectedReturn,
                                  String completionEvent) {
        return new RelayTarget(
                stageToken,
                unitContext.panelRoute(baseRoute, null, stageToken, inboxKey, queueCode),
                inboxKey,
                queueCode,
                unitContext.stageCellCode(stageToken, queueCode),
                unitContext.stageBindingKey(stageToken, inboxKey, queueCode),
                nextStages,
                expectedReturn,
                completionEvent
        );
    }

    private Map<String, Object> buildQueueMetadata(Processo processo,
                                                   Usuario actor,
                                                   RelayTarget target,
                                                   String tipoRecurso,
                                                   String axis,
                                                   SecretariatOperationalAssignmentService.AssignmentSnapshot assignment,
                                                   MagistraturaAuthorityProjection authorityProjection,
                                                   MagistraturaOperationalUnitContext unitContext) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("automationAxis", "RECURSAL_RELAY");
        metadata.put("resourceAxis", axis);
        metadata.put("tipoRecurso", tipoRecurso);
        metadata.put("processoId", processo.getId());
        metadata.put("processoNumero", firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()));
        metadata.put("ritoProcessual", processo.getRito() == null ? null : processo.getRito().name());
        metadata.put("ramoDireito", processo.getRamoDireito() == null ? null : processo.getRamoDireito().name());
        metadata.put("classeProcessual", firstNonBlank(processo.getClasseProcessual(), processo.getClasseTpuCodigo()));
        metadata.put("classeTpuCodigo", processo.getClasseTpuCodigo());
        metadata.put("vara", firstNonBlank(unitContext.varaLabel(), processo.getVara(), authorityProjection.authorityUnitLabel()));
        metadata.put("comarca", firstNonBlank(unitContext.comarcaLabel(), processo.getComarca()));
        metadata.put("tribunalCodigo", firstNonBlank(authorityProjection.tribunalAxis(), processo.getTribunalCodigoRoteado()));
        metadata.put("tribunalNome", processo.getTribunal());
        metadata.put("unidadeJudiciariaCodigo", firstNonBlank(unitContext.unidadeCodigo(), processo.getUnidadeJudiciariaCodigo(), authorityProjection.authorityUnitCode()));
        metadata.put("tipoJustica", processo.getTipoJustica() == null ? null : processo.getTipoJustica().name());
        metadata.put("panelRoute", target.panelRoute());
        metadata.put("stage", target.stageToken());
        metadata.put("participants", contactEnvelopeResolver.participantSnapshots(processo));
        metadata.put("contactEnvelope", contactEnvelopeResolver.buildEnvelope(processo));
        metadata.put("actorId", actor == null ? null : actor.getId());
        metadata.put("actorName", actor == null ? null : actor.getNome());
        metadata.put("assignment", assignmentSummary(assignment));
        metadata.put("routingSecretariatCode", firstNonBlank(unitContext.unidadeCodigo(), authorityProjection.authorityUnitCode(), processo.getUnidadeJudiciariaCodigo()));
        metadata.put("routingSecretariatLabel", firstNonBlank(authorityProjection.authorityUnitLabel(), unitContext.orgaoLabel(), unitContext.varaLabel()));
        metadata.put("nextStages", target.nextStages());
        metadata.put("expectedReturn", target.expectedReturn());
        metadata.put("completionEvent", target.completionEvent());
        metadata.put("authorityScope", authorityProjection.scope());
        metadata.put("authorityAxis", authorityProjection.authorityAxis());
        metadata.put("judgmentAxis", authorityProjection.judgmentAxis());
        metadata.put("authorityPanelRoute", authorityProjection.panelRoute());
        metadata.put("authorityReturnRoute", authorityProjection.returnRoute());
        metadata.put("authorityOrgao", authorityProjection.orgaoLabel());
        metadata.put("authorityJusticeAxis", authorityProjection.justiceAxis());
        metadata.put("authorityTribunalAxis", authorityProjection.tribunalAxis());
        metadata.put("authorityClass", authorityProjection.authorityClass());
        metadata.put("authorityInstitutionalPanelCode", authorityProjection.institutionalPanelCode());
        metadata.put("authorityInstitutionalLandingPath", authorityProjection.institutionalLandingPath());
        metadata.put("sessionRoute", resolveSessionRoute(processo, authorityProjection, target));
        metadata.put("acordaoRoute", resolveAcordaoRoute(processo, authorityProjection, target));
        metadata.put("originReturnRoute", resolveOriginReturnRoute(authorityProjection));
        metadata.put("colegiateComplianceRoute", authorityProjection.processMeshRoute());
        metadata.put("eventTrack", resolveAgendaTrack(target));
        metadata.put("operationalConfirmationStatus", resolveConfirmationStatus(target));
        metadata.put("attendanceStatus", resolveAttendanceStatus(target));
        metadata.put("processReturnRoute", firstNonBlank(resolveOriginReturnRoute(authorityProjection), authorityProjection.returnRoute()));
        metadata.put("processReturnStatus", resolveProcessReturnStatus(target, authorityProjection));
        metadata.put("autoReturnReady", Boolean.FALSE);
        metadata.put("reuseOperationalMesh", Boolean.TRUE);
        metadata.put("routingUnitCode", unitContext.unidadeCodigo());
        metadata.put("routingSnapshotRoute", unitContext.snapshotRoute());
        metadata.put("routingStageBindingKey", target.bindingKey());
        metadata.put("routingCellCode", target.cellCode());
        metadata.put("authorityUnitCode", authorityProjection.authorityUnitCode());
        metadata.put("authorityUnitLabel", authorityProjection.authorityUnitLabel());
        metadata.put("authorityUnitBindingKey", authorityProjection.authorityUnitBindingKey());
        metadata.put("routingVaraLabel", unitContext.varaLabel());
        metadata.put("routingOrgaoLabel", unitContext.orgaoLabel());
        metadata.put("routingComarcaLabel", unitContext.comarcaLabel());
        return safeMap(metadata);
    }

    private String resolveAgendaTrack(RelayTarget target) {
        return switch (firstNonBlank(target == null ? null : target.stageToken(), "BASE")) {
            case "COLEGIADO", "EMBARGOS" -> "SESSAO_COLEGIADA";
            case "ADMISSIBILIDADE" -> "COMUNICACAO_PROCESSUAL";
            default -> "OPERACIONAL_GERAL";
        };
    }

    private String resolveConfirmationStatus(RelayTarget target) {
        return switch (resolveAgendaTrack(target)) {
            case "SESSAO_COLEGIADA", "COMUNICACAO_PROCESSUAL" -> "PENDENTE_CONFIRMACAO";
            default -> "NAO_APLICAVEL";
        };
    }

    private String resolveAttendanceStatus(RelayTarget target) {
        return "SESSAO_COLEGIADA".equals(resolveAgendaTrack(target)) ? "AGUARDANDO_REALIZACAO" : "NAO_APLICAVEL";
    }

    private String resolveProcessReturnStatus(RelayTarget target,
                                              MagistraturaAuthorityProjection authorityProjection) {
        return firstNonBlank(resolveOriginReturnRoute(authorityProjection), authorityProjection == null ? null : authorityProjection.returnRoute()) == null
            ? "NAO_APLICAVEL"
            : "AGUARDANDO_EVENTO";
    }

    private String resolveSessionRoute(Processo processo,
                                       MagistraturaAuthorityProjection authorityProjection,
                                       RelayTarget target) {
        if (authorityProjection == null || "PRIMEIRO_GRAU".equals(authorityProjection.scope())) {
            return null;
        }
        if ("SUPERIOR".equals(authorityProjection.scope())) {
            return OperationalApiRoutes.ministroPlenarioPauta(processo.getId());
        }
        if ("PLENARIO".equals(authorityProjection.judgmentAxis())) {
            return OperationalApiRoutes.desembargadorPlenarioRelator(null);
        }
        return OperationalApiRoutes.secretariatOperationalCollegiatePauta(processo.getId()).replace("/api/v1/secretariat/", "/api/v1/secretaria/");
    }

    private String resolveAcordaoRoute(Processo processo,
                                       MagistraturaAuthorityProjection authorityProjection,
                                       RelayTarget target) {
        if (authorityProjection == null || "PRIMEIRO_GRAU".equals(authorityProjection.scope())) {
            return null;
        }
        if ("SUPERIOR".equals(authorityProjection.scope())) {
            return OperationalApiRoutes.ministroPlenarioDecisaoPlenaria(processo.getId());
        }
        return OperationalApiRoutes.secretariatOperationalCollegiateAcordao(null);
    }

    private String resolveOriginReturnRoute(MagistraturaAuthorityProjection authorityProjection) {
        if (authorityProjection == null || "PRIMEIRO_GRAU".equals(authorityProjection.scope())) {
            return null;
        }
        if ("SUPERIOR".equals(authorityProjection.scope())) {
            return authorityProjection.returnRoute();
        }
        return OperationalApiRoutes.secretariatOperationalCollegiateBaixa(null);
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

    private int computeScore(String stageToken) {
        return switch (stageToken) {
            case "EMBARGOS" -> 90;
            case "ADMISSIBILIDADE" -> 89;
            case "COLEGIADO" -> 88;
            default -> 82;
        };
    }

    private List<String> computeTags(Processo processo, String tipoRecurso, String axis) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add("RECURSAL");
        tags.add(axis);
        tags.add(tipoRecurso);
        if (processo.getTribunalCodigoRoteado() != null && !processo.getTribunalCodigoRoteado().isBlank()) {
            tags.add(processo.getTribunalCodigoRoteado());
        }
        if (processo.getRamoDireito() != null) {
            tags.add(processo.getRamoDireito().name());
        }
        return List.copyOf(tags);
    }

    private Map<String, Object> buildParticipant(String role, String nome, String cpf) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if ((nome == null || nome.isBlank()) && (cpf == null || cpf.isBlank())) {
            return Map.of();
        }
        out.put("role", role);
        out.put("nome", nome);
        out.put("cpf", cpf);
        Usuario linked = cpf == null || cpf.isBlank() ? null : usuarioRepository.findByCpf(cpf).orElse(null);
        if (linked != null) {
            out.put("usuarioId", linked.getId());
            out.put("email", linked.getEmail());
            out.put("tipoUsuario", linked.getTipoUsuario() == null ? null : linked.getTipoUsuario().name());
        }
        return safeMap(out);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> tribunalFlow(SecretariatOperationalRoutingProfile routing) {
        if (routing == null || routing.metadata() == null) {
            return Map.of();
        }
        Object raw = routing.metadata().get("tribunalFlow");
        return raw instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> source, String key) {
        if (source == null || key == null || key.isBlank()) {
            return Map.of();
        }
        Object raw = source.get(key);
        return raw instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private boolean containsAny(String corpus, String... needles) {
        if (corpus == null || corpus.isBlank() || needles == null) {
            return false;
        }
        String normalized = corpus.toUpperCase(Locale.ROOT);
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && normalized.contains(needle.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
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

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> safeMap(Map<String, ?> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((key, value) -> {
                if (key != null && value != null) {
                    out.put(key, value);
                }
            });
        }
        return Collections.unmodifiableMap(out);
    }

    private record RelayTarget(String stageToken,
                               String panelRoute,
                               String inboxKey,
                               String queueCode,
                               String cellCode,
                               String bindingKey,
                               List<String> nextStages,
                               String expectedReturn,
                               String completionEvent) {
    }

    private record AssignmentEnvelope(WorkItem item,
                                      RelayTarget target,
                                      SecretariatOperationalAssignmentService.AssignmentSnapshot assignment) {
        private Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("workItemId", item.getId());
            out.put("queueCode", item.getQueueCode());
            out.put("inboxKey", item.getInboxKey());
            out.put("panelRoute", target.panelRoute());
            out.put("stage", target.stageToken());
            out.put("nextStages", target.nextStages());
            out.put("cellCode", target.cellCode());
            out.put("bindingKey", target.bindingKey());
            out.put("expectedReturn", target.expectedReturn());
            out.put("completionEvent", target.completionEvent());
            if (item.getAssignedUser() != null) {
                out.put("assignedUserId", item.getAssignedUser().getId());
                out.put("assignedUserName", item.getAssignedUser().getNome());
                out.put("assignedUserEmail", item.getAssignedUser().getEmail());
            }
            if (assignment != null && assignment.backup() != null) {
                out.put("backupUserId", assignment.backup().getId());
                out.put("backupUserName", assignment.backup().getNome());
                out.put("backupUserEmail", assignment.backup().getEmail());
            }
            LinkedHashMap<String, Object> filtered = new LinkedHashMap<>();
            out.forEach((key, value) -> {
                if (key != null && value != null) {
                    filtered.put(key, value);
                }
            });
            return Map.copyOf(filtered);
        }
    }
}
