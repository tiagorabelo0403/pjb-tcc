package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoStudioDraftDiffService {

    public DraftDiffReport diff(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        List<String> baselineLines = linesOf(safe.baselineMarkdown());
        List<String> targetLines = linesOf(safe.targetMarkdown());
        List<String> baselineHeadings = headingsOf(baselineLines);
        List<String> targetHeadings = headingsOf(targetLines);

        ArrayList<Map<String, Object>> sections = new ArrayList<>();
        for (String heading : union(baselineHeadings, targetHeadings)) {
            boolean baselinePresent = baselineHeadings.contains(heading);
            boolean targetPresent = targetHeadings.contains(heading);
            String status = baselinePresent && targetPresent ? "KEPT" : baselinePresent ? "REMOVED" : "ADDED";
            LinkedHashMap<String, Object> section = new LinkedHashMap<>();
            section.put("heading", heading);
            section.put("status", status);
            section.put("summary", switch (status) {
                case "ADDED" -> "Seção nova adicionada na minuta consolidada.";
                case "REMOVED" -> "Seção presente no rascunho-base e ausente na versão consolidada.";
                default -> "Seção preservada entre a base e a minuta consolidada.";
            });
            sections.add(Map.copyOf(section));
        }

        int addedLines = countAdded(targetLines, baselineLines);
        int removedLines = countAdded(baselineLines, targetLines);
        int preservedLines = Math.max(0, Math.min(targetLines.size(), baselineLines.size()) - Math.min(addedLines, removedLines));

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("baselineLabel", safe.baselineLabel());
        summary.put("targetLabel", safe.targetLabel());
        summary.put("addedLines", addedLines);
        summary.put("removedLines", removedLines);
        summary.put("preservedLines", preservedLines);
        summary.put("sections", List.copyOf(sections));
        summary.put("structuralChange", !sections.isEmpty());
        summary.put("emptyBaseline", baselineLines.isEmpty());
        return new DraftDiffReport(
                List.copyOf(sections),
                Map.copyOf(summary)
        );
    }

    private List<String> linesOf(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }
        String[] split = markdown.replace("\r", "").split("\n");
        ArrayList<String> out = new ArrayList<>();
        for (String line : split) {
            String trimmed = trimToNull(line);
            if (trimmed != null) {
                out.add(trimmed);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private List<String> headingsOf(List<String> lines) {
        ArrayList<String> out = new ArrayList<>();
        for (String line : lines) {
            if (line.startsWith("#")) {
                String heading = trimToNull(line.replaceFirst("^#+", ""));
                if (heading != null && !out.contains(heading)) {
                    out.add(heading);
                }
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private List<String> union(List<String> left, List<String> right) {
        ArrayList<String> out = new ArrayList<>();
        if (left != null) {
            for (String value : left) {
                if (value != null && !out.contains(value)) {
                    out.add(value);
                }
            }
        }
        if (right != null) {
            for (String value : right) {
                if (value != null && !out.contains(value)) {
                    out.add(value);
                }
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private int countAdded(List<String> primary, List<String> reference) {
        int count = 0;
        for (String line : primary) {
            if (!reference.contains(line)) {
                count++;
            }
        }
        return count;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ResolveRequest(String baselineLabel,
                                 String baselineMarkdown,
                                 String targetLabel,
                                 String targetMarkdown) {
        public ResolveRequest {
            baselineLabel = baselineLabel == null || baselineLabel.isBlank() ? "BASELINE" : baselineLabel.trim().toUpperCase(Locale.ROOT);
            targetLabel = targetLabel == null || targetLabel.isBlank() ? "MINUTA_CONSOLIDADA" : targetLabel.trim().toUpperCase(Locale.ROOT);
        }

        public static ResolveRequest empty() {
            return new ResolveRequest("BASELINE", null, "MINUTA_CONSOLIDADA", null);
        }
    }

    public record DraftDiffReport(List<Map<String, Object>> sections,
                                  Map<String, Object> summary) {
        public DraftDiffReport {
            sections = sections == null ? List.of() : List.copyOf(sections);
            summary = summary == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(summary));
        }
    }
}
