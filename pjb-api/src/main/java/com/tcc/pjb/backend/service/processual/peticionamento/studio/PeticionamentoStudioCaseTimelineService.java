package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierReport;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoStudioCaseTimelineService {

    private static final Pattern DATE_HINT = Pattern.compile("\\b(\\d{1,2}/\\d{1,2}/\\d{2,4})\\b");

    public TimelineReport build(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        ArrayList<Map<String, Object>> items = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();
        LinkedHashSet<String> anchors = new LinkedHashSet<>();

        addTimelineSeed(items, anchors, "ABERTURA_CASO", "ABERTURA", "CONFIRMED",
                firstNonBlank(safe.caseTitle(), "Caso em elaboração"),
                "Workspace de peticionamento iniciado com consolidação do dossiê do caso.",
                null,
                List.of("TITULO", "PARTES"));

        int factIndex = 1;
        for (String fact : sanitize(safe.facts())) {
            String dateHint = extractDateHint(fact);
            ArrayList<String> tags = new ArrayList<>();
            tags.add("FATO");
            if (containsAny(fact, "urg", "liminar", "tutela", "risco", "dano")) {
                tags.add("URGENCIA");
            }
            addTimelineSeed(items, anchors, "FATO_" + factIndex, "FATO", "DECLARED",
                    "Fato estruturado " + factIndex,
                    fact,
                    dateHint,
                    tags);
            factIndex++;
        }

        int evidenceIndex = 1;
        for (Map<String, Object> item : safe.evidenceItems()) {
            String label = stringValue(item.get("label"), "Evidência " + evidenceIndex);
            String summary = stringValue(item.get("summary"), "Resumo probatório indisponível.");
            String evidenceType = stringValue(item.get("evidenceType"), "DOCUMENTO_GERAL");
            ArrayList<String> tags = new ArrayList<>(List.of("EVIDENCIA", evidenceType));
            if (Boolean.TRUE.equals(item.get("sensitive"))) {
                tags.add("SENSIVEL");
                warnings.add("A timeline contém evidência sensível que exige visualização controlada antes do uso narrativo intensivo.");
            }
            addTimelineSeed(items, anchors, "EVIDENCIA_" + evidenceIndex, "EVIDENCIA", "PROBATIVE",
                    label,
                    summary,
                    null,
                    tags);
            evidenceIndex++;
        }

        if (safe.requestedUrgency()) {
            addTimelineSeed(items, anchors, "JANELA_URGENTE", "PROCEDURAL", "ACTION_REQUIRED",
                    "Janela de urgência declarada",
                    "O caso foi marcado com urgência/tutela e deve receber ordenação prioritária da narrativa, da prova e dos pedidos.",
                    null,
                    List.of("URGENCIA", "TUTELA"));
            if (safe.evidenceItems().isEmpty()) {
                warnings.add("Há urgência declarada sem lastro probatório materializado no dossiê visível.");
            }
        }

        if (isRecursalFamily(safe.petitionFamily())) {
            addTimelineSeed(items, anchors, "JANELA_RECURSAL", "PROCEDURAL", "ACTION_REQUIRED",
                    "Janela recursal ativa",
                    recursalSummary(safe.petitionFamily(), safe.canonicalAppealType(), safe.counterReasons(), safe.embargosGrounds()),
                    null,
                    recursalTags(safe.petitionFamily(), safe.canonicalAppealType(), safe.counterReasons()));
        }

        ProcessMaterialDossierReport dossier = safe.materialDossier();
        if (dossier != null) {
            int gapIndex = 1;
            for (String gap : sanitize(dossier.proofGaps())) {
                addTimelineSeed(items, anchors, "LACUNA_" + gapIndex, "LACUNA", "ACTION_REQUIRED",
                        "Lacuna probatória " + gapIndex,
                        gap,
                        null,
                        List.of("LACUNA", "PROVA"));
                gapIndex++;
            }
        }

        if (items.size() < 3) {
            warnings.add("A timeline do caso está rasa; ampliar fatos com datas, agentes e suportes documentais melhora a robustez da peça.");
        }

        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("profile", isRecursalFamily(safe.petitionFamily()) ? "PETITION_TIMELINE_RECURSAL_V2" : "PETITION_TIMELINE_CASE_V2");
        workspace.put("petitionFamily", safe.petitionFamily());
        workspace.put("items", List.copyOf(items));
        workspace.put("anchors", List.copyOf(anchors));
        workspace.put("warnings", deduplicate(warnings));
        workspace.put("stats", Map.of(
                "total", items.size(),
                "fatos", countByPhase(items, "FATO"),
                "evidencias", countByPhase(items, "EVIDENCIA"),
                "lacunas", countByPhase(items, "LACUNA")
        ));
        return new TimelineReport(List.copyOf(items), deduplicate(warnings), Map.copyOf(workspace));
    }

    private void addTimelineSeed(List<Map<String, Object>> target,
                                 LinkedHashSet<String> anchors,
                                 String code,
                                 String phase,
                                 String status,
                                 String title,
                                 String detail,
                                 String dateHint,
                                 List<String> tags) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("phase", phase);
        item.put("status", status);
        item.put("title", title);
        item.put("detail", detail);
        if (dateHint != null) {
            item.put("dateHint", dateHint);
        }
        item.put("tags", sanitize(tags));
        target.add(Map.copyOf(item));
        anchors.add(phase);
    }

    private long countByPhase(List<Map<String, Object>> items, String phase) {
        return items.stream().filter(item -> phase.equals(item.get("phase"))).count();
    }

    private boolean isRecursalFamily(String family) {
        String normalized = trimToNull(family);
        return normalized != null && !"PETICAO_BASE".equals(normalized);
    }

    private String recursalSummary(String family, String appealType, boolean counterReasons, List<String> embargosGrounds) {
        if ("EMBARGOS".equals(family)) {
            return "Janela integrativa ativa para " + firstNonBlank(appealType, "embargos")
                    + (embargosGrounds == null || embargosGrounds.isEmpty() ? "." : " com vícios mapeados: " + String.join(", ", embargosGrounds) + ".");
        }
        if (counterReasons) {
            return "Janela de contrarrazões ativa; a timeline precisa fechar decisão recorrida, ciência e capítulos efetivamente atacados.";
        }
        return "Janela recursal ativa para " + firstNonBlank(appealType, "recurso") + ".";
    }

    private List<String> recursalTags(String family, String appealType, boolean counterReasons) {
        ArrayList<String> tags = new ArrayList<>(List.of("RECURSAL"));
        if (family != null) {
            tags.add(family);
        }
        if (appealType != null) {
            tags.add(appealType);
        }
        if (counterReasons) {
            tags.add("CONTRARRAZOES");
        }
        return List.copyOf(tags);
    }

    private String extractDateHint(String text) {
        String normalized = trimToNull(text);
        if (normalized == null) {
            return null;
        }
        Matcher matcher = DATE_HINT.matcher(normalized);
        return matcher.find() ? matcher.group(1) : null;
    }

    private boolean containsAny(String text, String... tokens) {
        String normalized = normalize(text);
        if (normalized == null || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            String candidate = normalize(token);
            if (candidate != null && normalized.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private List<String> sanitize(List<String> values) {
        ArrayList<String> out = new ArrayList<>();
        if (values == null) {
            return List.of();
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null && !out.contains(normalized)) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private List<String> deduplicate(List<String> values) {
        return sanitize(values);
    }

    private String stringValue(Object value, String fallback) {
        String normalized = trimToNull(value == null ? null : String.valueOf(value));
        return normalized == null ? fallback : normalized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String normalize(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Õ', 'O')
                .replace('Ô', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ResolveRequest(String caseTitle,
                                 List<String> facts,
                                 List<Map<String, Object>> evidenceItems,
                                 String petitionFamily,
                                 String canonicalAppealType,
                                 boolean counterReasons,
                                 List<String> embargosGrounds,
                                 boolean requestedUrgency,
                                 ProcessMaterialDossierReport materialDossier,
                                 List<PeticionamentoMediaBlocoRequest> mediaBlocks) {
        public ResolveRequest {
            facts = facts == null ? List.of() : List.copyOf(facts);
            evidenceItems = evidenceItems == null ? List.of() : List.copyOf(evidenceItems);
            petitionFamily = petitionFamily == null || petitionFamily.isBlank() ? "PETICAO_BASE" : petitionFamily.trim();
            embargosGrounds = embargosGrounds == null ? List.of() : List.copyOf(embargosGrounds);
            mediaBlocks = mediaBlocks == null ? List.of() : List.copyOf(mediaBlocks);
        }

        public static ResolveRequest empty() {
            return new ResolveRequest(null, List.of(), List.of(), "PETICAO_BASE", null, false, List.of(), false, null, List.of());
        }
    }

    public record TimelineReport(List<Map<String, Object>> items,
                                 List<String> warnings,
                                 Map<String, Object> workspace) {
        public TimelineReport {
            items = items == null ? List.of() : List.copyOf(items);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
            workspace = workspace == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(workspace));
        }
    }
}
