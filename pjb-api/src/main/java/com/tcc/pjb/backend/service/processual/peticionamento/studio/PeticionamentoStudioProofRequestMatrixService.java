package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoStudioProofRequestMatrixService {

    private static final Set<String> STOPWORDS = Set.of(
            "A", "AO", "AOS", "AS", "COM", "COMO", "CONTRA", "DA", "DAS", "DE", "DO", "DOS",
            "E", "EM", "NA", "NAS", "NO", "NOS", "O", "OS", "OU", "PARA", "PELA", "PELAS", "PELO", "PELOS",
            "POR", "QUE", "SE", "SEM", "SOB", "SUA", "SUAS", "SEU", "SEUS", "UM", "UMA", "REQUER", "REQUERER",
            "PEDIDO", "PEDIDOS", "AUTOR", "REU", "RE", "PARTE", "PROCESSO", "DECISAO", "SENTENCA", "ACORDAO"
    );

    public ProofRequestMatrixReport build(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        List<String> requests = sanitize(safe.requests());
        List<String> facts = sanitize(safe.facts());
        List<String> grounds = sanitize(safe.grounds());
        List<Map<String, Object>> evidenceItems = safe.evidenceItems() == null ? List.of() : List.copyOf(safe.evidenceItems());

        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        int robust = 0;
        int moderate = 0;
        int fragile = 0;
        int critical = 0;

        int index = 1;
        for (String requestLine : requests) {
            List<String> requestTokens = tokens(requestLine);
            List<String> supportingFacts = supportBySimilarity(facts, requestTokens, 1);
            List<String> supportingGrounds = supportBySimilarity(grounds, requestTokens, 1);
            List<String> supportingEvidence = evidenceBySimilarity(evidenceItems, requestTokens, 1);
            String strength = strengthLabel(supportingFacts, supportingGrounds, supportingEvidence);
            String colorBand = colorBand(strength);
            List<String> recommendations = recommendations(strength, supportingFacts, supportingGrounds, supportingEvidence);

            if ("ROBUSTO".equals(strength)) {
                robust++;
            } else if ("MODERADO".equals(strength)) {
                moderate++;
            } else if ("FRAGIL".equals(strength)) {
                fragile++;
            } else {
                critical++;
            }

            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("code", "REQUEST_SUPPORT_" + index);
            row.put("requestLabel", requestLine);
            row.put("strength", strength);
            row.put("colorBand", colorBand);
            row.put("supportFacts", supportingFacts);
            row.put("supportGrounds", supportingGrounds);
            row.put("supportEvidence", supportingEvidence);
            row.put("coverage", Map.of(
                    "facts", supportingFacts.size(),
                    "grounds", supportingGrounds.size(),
                    "evidence", supportingEvidence.size()
            ));
            row.put("recommendations", recommendations);
            rows.add(Map.copyOf(row));
            index++;
        }

        if (rows.isEmpty()) {
            rows.add(Map.of(
                    "code", "REQUEST_SUPPORT_EMPTY",
                    "requestLabel", "Nenhum pedido estruturado no dossiê atual.",
                    "strength", "CRITICO",
                    "colorBand", "CRITICAL_RED",
                    "supportFacts", List.of(),
                    "supportGrounds", List.of(),
                    "supportEvidence", List.of(),
                    "coverage", Map.of("facts", 0, "grounds", 0, "evidence", 0),
                    "recommendations", List.of("Estruturar ao menos um pedido principal antes da assinatura e do protocolo.")
            ));
            critical = 1;
        }

        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("profile", "PETITION_PROOF_REQUEST_MATRIX_V2");
        workspace.put("items", List.copyOf(rows));
        workspace.put("summary", Map.of(
                "robustos", robust,
                "moderados", moderate,
                "frageis", fragile,
                "criticos", critical,
                "total", rows.size()
        ));
        workspace.put("overallStrength", overallStrength(robust, moderate, fragile, critical));
        return new ProofRequestMatrixReport(List.copyOf(rows), Map.copyOf(workspace));
    }

    private List<String> supportBySimilarity(List<String> lines, List<String> requestTokens, int minimumOverlap) {
        ArrayList<String> matches = new ArrayList<>();
        for (String line : lines) {
            if (overlap(tokens(line), requestTokens) >= minimumOverlap) {
                matches.add(line);
            }
        }
        return matches.isEmpty() ? List.of() : List.copyOf(matches.subList(0, Math.min(matches.size(), 3)));
    }

    private List<String> evidenceBySimilarity(List<Map<String, Object>> evidenceItems, List<String> requestTokens, int minimumOverlap) {
        ArrayList<String> matches = new ArrayList<>();
        for (Map<String, Object> item : evidenceItems) {
            String candidate = stringValue(item.get("label"), null) + " " + stringValue(item.get("summary"), "");
            if (overlap(tokens(candidate), requestTokens) >= minimumOverlap || Boolean.TRUE.equals(item.get("sensitive")) && requestTokens.contains("DANO")) {
                matches.add(stringValue(item.get("label"), "Evidência sem rótulo"));
            }
        }
        return matches.isEmpty() ? List.of() : List.copyOf(matches.subList(0, Math.min(matches.size(), 3)));
    }

    private int overlap(List<String> left, List<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String token : left) {
            if (right.contains(token)) {
                count++;
            }
        }
        return count;
    }

    private List<String> recommendations(String strength,
                                         List<String> facts,
                                         List<String> grounds,
                                         List<String> evidence) {
        ArrayList<String> out = new ArrayList<>();
        if (facts.isEmpty()) {
            out.add("Amarrar o pedido a pelo menos um fato objetivo e datado.");
        }
        if (grounds.isEmpty()) {
            out.add("Amarrar o pedido a fundamento normativo ou tese jurídica específica.");
        }
        if (evidence.isEmpty()) {
            out.add("Vincular ao menos um documento, mídia ou prova técnica ao pedido.");
        }
        if (out.isEmpty() && "ROBUSTO".equals(strength)) {
            out.add("Cobertura mínima presente; revisar proporcionalidade do pedido e coerência com o rito.");
        }
        return List.copyOf(out);
    }

    private String strengthLabel(List<String> facts, List<String> grounds, List<String> evidence) {
        int axes = 0;
        if (!facts.isEmpty()) {
            axes++;
        }
        if (!grounds.isEmpty()) {
            axes++;
        }
        if (!evidence.isEmpty()) {
            axes++;
        }
        return switch (axes) {
            case 3 -> "ROBUSTO";
            case 2 -> "MODERADO";
            case 1 -> "FRAGIL";
            default -> "CRITICO";
        };
    }

    private String colorBand(String strength) {
        return switch (strength) {
            case "ROBUSTO" -> "ACTIVE_BLUE";
            case "MODERADO" -> "ATTENTION_ORANGE";
            case "FRAGIL" -> "TAGGED_PURPLE";
            default -> "CRITICAL_RED";
        };
    }

    private String overallStrength(int robust, int moderate, int fragile, int critical) {
        if (critical > 0) {
            return "CRITICO";
        }
        if (fragile > 0) {
            return "FRAGIL";
        }
        if (moderate > 0 && robust == 0) {
            return "MODERADO";
        }
        return "ROBUSTO";
    }

    private List<String> tokens(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String part : normalized.split("[^A-Z0-9]+")) {
            if (part.length() >= 4 && !STOPWORDS.contains(part)) {
                out.add(part);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
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

    private String stringValue(Object value, String fallback) {
        String normalized = trimToNull(value == null ? null : String.valueOf(value));
        return normalized == null ? fallback : normalized;
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

    public record ResolveRequest(List<String> requests,
                                 List<String> facts,
                                 List<String> grounds,
                                 List<Map<String, Object>> evidenceItems) {
        public ResolveRequest {
            requests = requests == null ? List.of() : List.copyOf(requests);
            facts = facts == null ? List.of() : List.copyOf(facts);
            grounds = grounds == null ? List.of() : List.copyOf(grounds);
            evidenceItems = evidenceItems == null ? List.of() : List.copyOf(evidenceItems);
        }

        public static ResolveRequest empty() {
            return new ResolveRequest(List.of(), List.of(), List.of(), List.of());
        }
    }

    public record ProofRequestMatrixReport(List<Map<String, Object>> items,
                                           Map<String, Object> workspace) {
        public ProofRequestMatrixReport {
            items = items == null ? List.of() : List.copyOf(items);
            workspace = workspace == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(workspace));
        }
    }
}
