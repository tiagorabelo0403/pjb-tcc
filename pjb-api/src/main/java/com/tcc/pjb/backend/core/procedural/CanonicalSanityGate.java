package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.DefinitionSnapshot;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.DocumentSpec;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.PartyRoleSpec;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import java.time.Instant;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CanonicalSanityGate {

    public enum Status {
        OK,
        CANONICAL_CONTEXT_INCOMPLETE,
        COMPETENCE_UNRESOLVED,
        TRIBUNAL_NOT_HOMOLOGATED,
        WORKFLOW_BLUEPRINT_INCOMPLETE,
        DOCUMENT_SCHEMA_INCOMPLETE
    }

    public record GateIssue(
            Status status,
            String field,
            String message,
            boolean blocking
    ) {
        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("status", status == null ? Status.CANONICAL_CONTEXT_INCOMPLETE.name() : status.name());
            out.put("field", field == null ? "" : field);
            out.put("message", message == null ? "Inconsistência canônica identificada." : message);
            out.put("blocking", blocking);
            return Collections.unmodifiableMap(out);
        }
    }

    public record GateResult(
            String overallStatus,
            boolean passed,
            List<GateIssue> issues,
            Map<String, Object> metadata,
            Instant evaluatedAt
    ) {
        public boolean hasBlockingIssues() {
            return issues.stream().anyMatch(GateIssue::blocking);
        }

        public List<String> statusCodes() {
            return issues.stream().map(issue -> issue.status().name()).distinct().toList();
        }

        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("overallStatus", overallStatus == null ? Status.CANONICAL_CONTEXT_INCOMPLETE.name() : overallStatus);
            out.put("passed", passed);
            out.put("issues", issues == null ? List.of() : issues.stream().map(GateIssue::toMap).toList());
            out.put("metadata", freezeMetadata(metadata));
            out.put("evaluatedAt", evaluatedAt != null ? evaluatedAt.toString() : "");
            return Collections.unmodifiableMap(out);
        }
    }

    private final ProceduralCatalogService proceduralCatalogService;

    public CanonicalSanityGate(ProceduralCatalogService proceduralCatalogService) {
        this.proceduralCatalogService = Objects.requireNonNull(proceduralCatalogService);
    }

    public GateResult evaluate(CanonicalContext canonicalContext) {
        List<GateIssue> issues = new ArrayList<>();
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("hasContext", canonicalContext != null);
        if (canonicalContext == null) {
            issues.add(issue(Status.CANONICAL_CONTEXT_INCOMPLETE, "canonicalContext",
                    "Contexto canônico ausente. Não é seguro prosseguir sem resolução procedural canônica.", true));
            return result(issues, metadata);
        }

        metadata.put("resolvedRito", canonicalContext.rito() != null ? canonicalContext.rito().name() : null);
        metadata.put("resolvedTribunalCodigo", canonicalContext.tribunalCodigo());
        metadata.put("resolvedClasseTpuCodigo", canonicalContext.classeTpuCodigo());

        if (canonicalContext.rito() == null) {
            issues.add(issue(Status.CANONICAL_CONTEXT_INCOMPLETE, "rito",
                    "Rito canônico não resolvido no núcleo procedural.", true));
        }
        if (isBlank(canonicalContext.ramoDireito())) {
            issues.add(issue(Status.CANONICAL_CONTEXT_INCOMPLETE, "ramoDireito",
                    "Ramo do direito canônico não resolvido.", true));
        }

        Optional<TpuClasseCnj> classe = resolveClasse(canonicalContext);
        metadata.put("classeResolved", classe.isPresent());
        if (classe.isPresent()) {
            metadata.put("classeTpu", TpuClasseCnj.toMap(classe.get()));
        } else {
            issues.add(issue(Status.CANONICAL_CONTEXT_INCOMPLETE, "classeTpu",
                    "Classe TPU não homologada ou não resolvida pelo catálogo canônico.", true));
        }

        DefinitionSnapshot snapshot = canonicalContext.rito() != null
                ? proceduralCatalogService.snapshot(canonicalContext.rito())
                : null;
        metadata.put("snapshotPresent", snapshot != null);
        if (snapshot == null) {
            issues.add(issue(Status.WORKFLOW_BLUEPRINT_INCOMPLETE, "workflow",
                    "Blueprint workflow ausente para o rito canônico resolvido.", true));
        } else {
            long requiredParties = snapshot.parties().stream().filter(PartyRoleSpec::required).count();
            long requiredDocuments = snapshot.documents().stream().filter(DocumentSpec::required).count();
            metadata.put("requiredParties", requiredParties);
            metadata.put("requiredDocuments", requiredDocuments);
            metadata.put("stages", snapshot.stages().size());
            if (requiredParties == 0) {
                issues.add(issue(Status.CANONICAL_CONTEXT_INCOMPLETE, "parties",
                        "Schema de partes obrigatório não foi definido para o rito canônico.", true));
            }
            if (requiredDocuments == 0) {
                issues.add(issue(Status.DOCUMENT_SCHEMA_INCOMPLETE, "documents",
                        "Schema documental obrigatório não foi definido para o rito canônico.", true));
            }
            if (snapshot.stages().isEmpty()) {
                issues.add(issue(Status.WORKFLOW_BLUEPRINT_INCOMPLETE, "workflow",
                        "Blueprint BPMN sem fases ou tarefas para o rito canônico.", true));
            }
        }

        if (isBlank(canonicalContext.tribunalCodigo())) {
            issues.add(issue(Status.COMPETENCE_UNRESOLVED, "tribunalCodigo",
                    "Competência não fechada até tribunal no contexto canônico.", true));
        } else {
            Optional<NationalCompetenceMatrix> tribunal = NationalCompetenceMatrix.porCodigo(canonicalContext.tribunalCodigo());
            if (tribunal.isEmpty()) {
                issues.add(issue(Status.TRIBUNAL_NOT_HOMOLOGATED, "tribunalCodigo",
                        "Tribunal resolvido fora da matriz nacional homologada.", true));
            } else {
                metadata.put("tribunalMatrix", tribunal.get().toMap());
                if (!tribunal.get().supportsProtocolo()) {
                    issues.add(issue(Status.TRIBUNAL_NOT_HOMOLOGATED, "connector",
                            "Tribunal homologado sem capability de protocolo eletrônico operacional.", true));
                }
            }
        }

        return result(issues, metadata);
    }

    public Map<String, Object> mergeMetadata(Map<String, Object> target, GateResult gateResult) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(target == null ? Map.of() : target);
        if (gateResult != null) {
            out.put("sanityGate", gateResult.toMap());
            out.put("sanityStatus", gateResult.overallStatus());
            out.put("sanityPassed", gateResult.passed());
            out.put("sanityCodes", gateResult.statusCodes());
        }
        return freezeMetadata(out);
    }

    private static Map<String, Object> freezeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null && value != null) {
                out.put(key, value);
            }
        });
        return PayloadMaps.deepCopyWithoutNulls(out);
    }

    private GateResult result(List<GateIssue> issues, Map<String, Object> metadata) {
        List<GateIssue> frozenIssues = List.copyOf(issues);
        String overall = frozenIssues.stream()
                .filter(GateIssue::blocking)
                .map(issue -> issue.status().name())
                .findFirst()
                .orElse(Status.OK.name());
        return new GateResult(overall, frozenIssues.stream().noneMatch(GateIssue::blocking), frozenIssues,
                freezeMetadata(metadata), Instant.now());
    }

    private Optional<TpuClasseCnj> resolveClasse(CanonicalContext canonicalContext) {
        if (canonicalContext == null) {
            return Optional.empty();
        }
        String classeKey = firstNonBlank(canonicalContext.classeTpuCodigo(), canonicalContext.classeTpuNome());
        if (classeKey == null) {
            return Optional.empty();
        }
        return proceduralCatalogService.resolveClasseTpu(classeKey, canonicalContext.rito());
    }

    private GateIssue issue(Status status, String field, String message, boolean blocking) {
        return new GateIssue(Objects.requireNonNull(status), normalizeField(field), normalizeMessage(message), blocking);
    }

    private static String normalizeField(String field) {
        return field == null ? "" : field.trim();
    }

    private static String normalizeMessage(String message) {
        return message == null ? "" : message.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
