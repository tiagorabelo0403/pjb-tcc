package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService.RoutingDecision;
import com.tcc.pjb.backend.model.entity.Processo;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProceduralConnectorExecutionService {

    private final ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService;
    private final TribunalProtocolRoutingService tribunalProtocolRoutingService;

    public ProceduralConnectorExecutionService(ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService,
                                               TribunalProtocolRoutingService tribunalProtocolRoutingService) {
        this.proceduralSubmissionBlueprintService = Objects.requireNonNull(proceduralSubmissionBlueprintService);
        this.tribunalProtocolRoutingService = Objects.requireNonNull(tribunalProtocolRoutingService);
    }

    public ProceduralConnectorExecutionReport analyzeProcess(Processo processo) {
        return analyzeProcess(processo, null, null);
    }

    public ProceduralConnectorExecutionReport analyzeProcess(Processo processo,
                                                             ProceduralRoutingReport routing,
                                                             ProceduralSubmissionBlueprintReport blueprint) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        if (processo != null) {
            payload.put("processoId", processo.getId());
            payload.put("numeroUnificado", processo.getNumeroUnificado());
            payload.put("numeroProcesso", processo.getNumeroProcesso());
            payload.put("classe", processo.getClasseProcessual());
            payload.put("classeTpu", processo.getClasseTpuCodigo());
            payload.put("assunto", processo.getAssunto());
            payload.put("objetoProcessual", processo.getObjetoProcessual());
            payload.put("pedidoPrincipal", processo.getPedidoPrincipal());
            payload.put("pedidos", processo.getPedidosConsolidados());
            payload.put("ramoDireito", processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null);
            payload.put("rito", processo.getRito() != null ? processo.getRito().name() : null);
            payload.put("tipoJustica", processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null);
            payload.put("valorCausa", processo.getValorCausa());
            payload.put("parteAutoraCpf", processo.getParteAutoraCpf());
            payload.put("parteReuCpf", processo.getParteReuCpf());
            if (processo.getJurisdicao() != null) {
                payload.put("tribunalCodigo", processo.getJurisdicao().getCodigo());
                payload.put("varaPretendida", processo.getJurisdicao().getNome());
                payload.put("comarcaAutor", processo.getJurisdicao().getCidade());
                payload.put("ufAutor", processo.getJurisdicao().getEstado());
            }
        }
        return analyzeContext(payload, routing, blueprint);
    }

    public ProceduralConnectorExecutionReport analyzeContext(Map<String, Object> payload,
                                                             ProceduralRoutingReport routing,
                                                             ProceduralSubmissionBlueprintReport blueprint) {
        LinkedHashMap<String, Object> safePayload = new LinkedHashMap<>();
        if (payload != null) {
            safePayload.putAll(payload);
        }
        ProceduralRoutingReport resolvedRouting = routing;
        ProceduralSubmissionBlueprintReport resolvedBlueprint = blueprint == null ? proceduralSubmissionBlueprintService.analyzeContext(safePayload, resolvedRouting, null) : blueprint;
        RoutingDecision routingDecision = tribunalProtocolRoutingService.resolve(
                safePayload,
                firstNonBlank(resolvedBlueprint == null ? null : resolvedBlueprint.rito(), resolvedRouting == null ? null : resolvedRouting.ritoSugerido(), text(safePayload.get("rito"))),
                firstNonBlank(resolvedRouting != null ? resolvedRouting.actionFamily() : null, text(safePayload.get("ramoDireito"))),
                firstNonBlank(resolvedRouting != null ? resolvedRouting.tipoJusticaSugerida() : null, text(safePayload.get("tipoJustica"))),
                false
        );
        JudicialSubmissionCapability capability = routingDecision != null ? routingDecision.capability() : null;
        JudicialSystem system = routingDecision != null ? routingDecision.judicialSystem() : resolvedBlueprint != null ? resolvedBlueprint.judicialSystem() : JudicialSystem.OUTRO;
        String tribunalCodigo = firstNonBlank(
                resolvedBlueprint != null ? resolvedBlueprint.tribunalCodigo() : null,
                routingDecision != null ? routingDecision.tribunalCodigo() : null,
                text(safePayload.get("tribunalCodigo"))
        );
        String unidadeCodigo = firstNonBlank(
                resolvedBlueprint != null ? resolvedBlueprint.unidadeJudiciariaCodigo() : null,
                resolvedRouting != null && resolvedRouting.forumAllocation() != null ? resolvedRouting.forumAllocation().unidadeJudiciariaCodigo() : null,
                text(safePayload.get("varaPretendida"))
        );
        String classeTpuCodigo = firstNonBlank(
                resolvedBlueprint != null ? resolvedBlueprint.classeTpuCodigo() : null,
                resolvedRouting != null && resolvedRouting.forumAllocation() != null ? resolvedRouting.forumAllocation().classeTpuCodigo() : null,
                text(safePayload.get("classeTpu")),
                text(safePayload.get("classe"))
        );
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> checklist = new LinkedHashSet<>();
        if (resolvedBlueprint != null) {
            blockers.addAll(safeList(resolvedBlueprint.blockingIssues()));
            warnings.addAll(safeList(resolvedBlueprint.warnings()));
            checklist.addAll(safeList(resolvedBlueprint.reviewChecklist()));
        }
        if (resolvedRouting != null) {
            blockers.addAll(safeList(resolvedRouting.blockingIssues()));
            warnings.addAll(safeList(resolvedRouting.alerts()));
            checklist.addAll(safeList(resolvedRouting.reviewChecklist()));
            if (resolvedRouting.forumAllocation() != null) {
                blockers.addAll(safeList(resolvedRouting.forumAllocation().incompatibilities()));
                warnings.addAll(safeList(resolvedRouting.forumAllocation().warnings()));
                checklist.addAll(safeList(resolvedRouting.forumAllocation().reviewChecklist()));
            }
        }
        if (routingDecision != null) {
            warnings.addAll(safeList(routingDecision.warnings()));
        }
        addBaseExecutionChecklist(checklist, capability, resolvedBlueprint, tribunalCodigo, unidadeCodigo, classeTpuCodigo);
        List<String> phases = buildPhases(capability, resolvedBlueprint, blockers);
        String executionMode = resolveExecutionMode(blockers, capability, resolvedBlueprint);
        String submissionLane = resolveSubmissionLane(executionMode, capability, resolvedBlueprint);
        String signerMode = resolveSignerMode(capability, resolvedBlueprint);
        String retryPolicy = resolveRetryPolicy(executionMode, blockers, capability);
        boolean allowedToAutoSubmit = "REAL_CONNECTOR".equals(executionMode) && blockers.isEmpty();
        boolean requiresHumanReview = !blockers.isEmpty()
                || "REVIEW_REQUIRED".equals(submissionLane)
                || (resolvedBlueprint != null && !resolvedBlueprint.readyForAssistedSubmission());
        String targetKey = buildTargetKey(system, tribunalCodigo, unidadeCodigo, classeTpuCodigo);
        String idempotencyKey = buildIdempotencyKey(targetKey, resolvedBlueprint != null ? resolvedBlueprint.requestId() : null, safePayload);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("routingDecision", routingDecision != null ? routingDecision.metadata() : Map.of());
        metadata.put("blueprint", resolvedBlueprint != null ? resolvedBlueprint.toMap() : Map.of());
        metadata.put("routing", resolvedRouting != null ? resolvedRouting.toMap() : Map.of());
        metadata.put("connectorCapability", capability == null ? Map.of() : JudicialMapSupport.compact(
                "system", capability.system() != null ? capability.system().name() : null,
                "enabled", capability.enabled(),
                "supportsProtocol", capability.supportsProtocol(),
                "supportsDryRun", capability.supportsDryRun(),
                "operational", capability.operational(),
                "requiresGovBrStepUp", capability.requiresStepUpGovBr(),
                "requiresCertificate", capability.requiresCertificate(),
                "baseUrl", capability.baseUrl()
        ));
        metadata.put("resolvedAt", Instant.now().toString());
        metadata.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);

        return new ProceduralConnectorExecutionReport(
                Instant.now(),
                executionMode,
                submissionLane,
                targetKey,
                system,
                tribunalCodigo,
                unidadeCodigo,
                classeTpuCodigo,
                idempotencyKey,
                signerMode,
                retryPolicy,
                allowedToAutoSubmit,
                requiresHumanReview,
                capability != null && capability.requiresStepUpGovBr(),
                capability != null && capability.requiresCertificate(),
                List.copyOf(phases),
                List.copyOf(checklist),
                List.copyOf(blockers),
                List.copyOf(warnings),
                Collections.unmodifiableMap(metadata)
        );
    }

    private void addBaseExecutionChecklist(LinkedHashSet<String> checklist,
                                           JudicialSubmissionCapability capability,
                                           ProceduralSubmissionBlueprintReport blueprint,
                                           String tribunalCodigo,
                                           String unidadeCodigo,
                                           String classeTpuCodigo) {
        checklist.add("Validar tribunal alvo: " + firstNonBlank(tribunalCodigo, "N/A"));
        checklist.add("Validar unidade judiciária alvo: " + firstNonBlank(unidadeCodigo, "N/A"));
        checklist.add("Validar classe TPU final: " + firstNonBlank(classeTpuCodigo, "N/A"));
        if (blueprint != null && blueprint.localCorrelationMode() != null) {
            checklist.add("Executar guarda de correlação local: " + blueprint.localCorrelationMode());
        }
        if (blueprint != null && blueprint.dryRunStatus() != null) {
            checklist.add("Conferir status do dry-run judicial: " + blueprint.dryRunStatus());
        }
        if (capability != null && capability.requiresStepUpGovBr()) {
            checklist.add("Realizar step-up Gov.br antes da assinatura e transmissão.");
        }
        if (capability != null && capability.requiresCertificate()) {
            checklist.add("Validar certificado digital compatível com o tribunal selecionado.");
        }
        if (capability == null || !capability.operational()) {
            checklist.add("Conector judicial sem operação completa exige validação operacional humana.");
        }
    }

    private List<String> buildPhases(JudicialSubmissionCapability capability,
                                     ProceduralSubmissionBlueprintReport blueprint,
                                     LinkedHashSet<String> blockers) {
        List<String> phases = new ArrayList<>();
        phases.add("CANONICAL_INTENT_CAPTURE");
        phases.add("COMPETENCE_AND_CLASS_LOCK");
        phases.add("LOCAL_CORRELATION_GUARD");
        if (blueprint != null && blueprint.dryRunStatus() != null) {
            phases.add("CONNECTOR_DRY_RUN_VALIDATION");
        }
        if (capability != null && capability.requiresStepUpGovBr()) {
            phases.add("GOVBR_STEP_UP_AUTH");
        }
        if (capability != null && capability.requiresCertificate()) {
            phases.add("CERTIFICATE_BINDING");
        }
        phases.add("PAYLOAD_SEAL_AND_IDEMPOTENCY");
        phases.add(blockers.isEmpty() ? "PROTOCOL_SUBMISSION_GATE" : "PROTOCOL_BLOCKING_REVIEW");
        phases.add("POST_SUBMISSION_DISTRIBUTION_SYNC");
        return List.copyOf(phases);
    }

    private String resolveExecutionMode(LinkedHashSet<String> blockers,
                                        JudicialSubmissionCapability capability,
                                        ProceduralSubmissionBlueprintReport blueprint) {
        if (!blockers.isEmpty()) {
            return "BLOCKED";
        }
        boolean realReady = blueprint != null && blueprint.readyForRealConnectorSubmission();
        boolean assistedReady = blueprint != null && blueprint.readyForAssistedSubmission();
        if (realReady && capability != null && capability.operational() && capability.supportsProtocol()) {
            return "REAL_CONNECTOR";
        }
        if (assistedReady) {
            return "ASSISTED_CONNECTOR";
        }
        if (capability == null || !capability.supportsProtocol()) {
            return "MANUAL_PROTOCOL";
        }
        return "REVIEW_REQUIRED";
    }

    private String resolveSubmissionLane(String executionMode,
                                         JudicialSubmissionCapability capability,
                                         ProceduralSubmissionBlueprintReport blueprint) {
        return switch (executionMode) {
            case "REAL_CONNECTOR" -> "DIRECT_PROTOCOL";
            case "ASSISTED_CONNECTOR" -> capability != null && capability.supportsDryRun() && blueprint != null && blueprint.dryRunAccepted() ? "ASSISTED_PROTOCOL" : "GUIDED_REVIEW";
            case "MANUAL_PROTOCOL" -> "MANUAL_UPLOAD";
            case "BLOCKED", "REVIEW_REQUIRED" -> "REVIEW_REQUIRED";
            default -> "REVIEW_REQUIRED";
        };
    }

    private String resolveSignerMode(JudicialSubmissionCapability capability,
                                     ProceduralSubmissionBlueprintReport blueprint) {
        if (capability != null && capability.requiresCertificate()) {
            return "CERTIFICATE";
        }
        if (capability != null && capability.requiresStepUpGovBr()) {
            return "GOVBR_STEP_UP";
        }
        if (blueprint != null && blueprint.requiresCertificate()) {
            return "CERTIFICATE";
        }
        if (blueprint != null && blueprint.requiresGovBrStepUp()) {
            return "GOVBR_STEP_UP";
        }
        return "STANDARD";
    }

    private String resolveRetryPolicy(String executionMode,
                                      LinkedHashSet<String> blockers,
                                      JudicialSubmissionCapability capability) {
        if (!blockers.isEmpty()) {
            return "NO_RETRY";
        }
        if ("REAL_CONNECTOR".equals(executionMode) && capability != null && capability.operational()) {
            return "SAFE_RETRY";
        }
        if ("ASSISTED_CONNECTOR".equals(executionMode) || "MANUAL_PROTOCOL".equals(executionMode)) {
            return "MANUAL_RETRY";
        }
        return "NO_RETRY";
    }

    private String buildTargetKey(JudicialSystem system,
                                  String tribunalCodigo,
                                  String unidadeCodigo,
                                  String classeTpuCodigo) {
        return firstNonBlank(
                join(system != null ? system.name() : null, tribunalCodigo, unidadeCodigo, classeTpuCodigo),
                "PJB::UNRESOLVED_TARGET"
        );
    }

    private String buildIdempotencyKey(String targetKey,
                                       String requestId,
                                       Map<String, Object> payload) {
        String seed = join(targetKey, requestId, text(payload.get("numeroUnificado")), text(payload.get("numeroProcesso")), text(payload.get("pedidoPrincipal")));
        return integrityHash(seed == null ? Objects.toString(payload, "") : seed).substring(0, 24).toUpperCase(Locale.ROOT);
    }

    private static List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String out = String.valueOf(value).trim();
        return out.isEmpty() ? null : out;
    }

    private static String join(String... values) {
        StringBuilder sb = new StringBuilder();
        if (values != null) {
            for (String value : values) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append("::");
                }
                sb.append(value.trim());
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static String firstNonBlank(String... values) {
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

    private static String integrityHash(Object payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(String.valueOf(payload).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
