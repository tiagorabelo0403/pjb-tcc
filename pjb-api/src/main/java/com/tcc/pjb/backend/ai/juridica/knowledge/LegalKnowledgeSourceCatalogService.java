package com.tcc.pjb.backend.ai.juridica.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeJsonResourceLoader;
import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeResourcePaths;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeSourceDescriptor;
import jakarta.annotation.PostConstruct;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class LegalKnowledgeSourceCatalogService {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private final LegalKnowledgeJsonResourceLoader resourceLoader;
    private volatile List<LegalKnowledgeSourceDescriptor> sources = List.of();
    private volatile List<BranchInferenceRule> branchRules = List.of();
    private volatile List<String> defaultBranches = List.of("CONSTITUCIONAL", "CIVIL");
    private volatile Map<String, Object> doctrinePolicy = Map.of();
    private volatile List<String> priorityOrder = List.of();
    private volatile List<String> ingestionPolicies = List.of();
    private volatile List<String> officialOnlyReasons = List.of();
    private volatile List<String> officialPlusDoctrineReasons = List.of();

    public LegalKnowledgeSourceCatalogService(LegalKnowledgeJsonResourceLoader resourceLoader) {
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader");
    }

    @PostConstruct
    void load() {
        loadSources();
        loadBranchInference();
        loadPolicyCatalog();
    }

    public List<LegalKnowledgeSourceDescriptor> listAll() {
        return sources;
    }

    public List<LegalKnowledgeSourceDescriptor> listOfficialSources() {
        return sources.stream()
                .filter(this::isOfficial)
                .toList();
    }

    public List<LegalKnowledgeSourceDescriptor> listDoctrineSources() {
        return sources.stream()
                .filter(item -> "DOCTRINE".equals(item.sourceKind()))
                .toList();
    }

    public List<LegalKnowledgeSourceDescriptor> selectForBranches(List<String> requestedBranches, boolean includeDoctrine) {
        LinkedHashSet<String> normalizedBranches = new LinkedHashSet<>(normalizeList(requestedBranches));
        ArrayList<LegalKnowledgeSourceDescriptor> selected = new ArrayList<>();
        for (LegalKnowledgeSourceDescriptor source : sources) {
            if (!includeDoctrine && "DOCTRINE".equals(source.sourceKind())) {
                continue;
            }
            if (normalizedBranches.isEmpty() || intersects(source.branches(), normalizedBranches)) {
                selected.add(source);
            }
        }
        if (selected.isEmpty()) {
            return includeDoctrine ? sources : listOfficialSources();
        }
        return List.copyOf(selected);
    }

    public List<String> inferBranches(String message, Map<String, Object> context) {
        String haystack = normalizeSearchText(message) + ' ' + normalizeSearchText(flattenContext(context));
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (BranchInferenceRule rule : branchRules) {
            if (rule.matches(haystack)) {
                out.add(rule.branchCode());
            }
        }
        if (out.isEmpty()) {
            out.addAll(defaultBranches);
        }
        return List.copyOf(out);
    }

    public Map<String, Object> doctrinePolicy() {
        return doctrinePolicy;
    }

    public List<String> priorityOrder() {
        return priorityOrder;
    }

    public List<String> ingestionPolicies() {
        return ingestionPolicies;
    }

    public List<String> selectionReasons(boolean includeDoctrine) {
        return includeDoctrine ? officialPlusDoctrineReasons : officialOnlyReasons;
    }

    private void loadSources() {
        List<SourceSeed> loaded = resourceLoader.readList(LegalKnowledgeResourcePaths.SOURCE_CATALOG, new TypeReference<>() {});
        ArrayList<LegalKnowledgeSourceDescriptor> items = new ArrayList<>(loaded.size());
        for (SourceSeed item : loaded) {
            items.add(new LegalKnowledgeSourceDescriptor(
                    normalizeText(item.sourceId()),
                    item.title() == null ? "" : item.title().trim(),
                    normalizeText(item.sourceKind()),
                    normalizeText(item.authorityLevel()),
                    item.institution() == null ? "" : item.institution().trim(),
                    normalizeText(item.storageLane()),
                    normalizeText(item.licensingModel()),
                    item.baseUrl() == null ? "" : item.baseUrl().trim(),
                    normalizeText(item.refreshStrategy()),
                    normalizeList(item.branches()),
                    normalizeList(item.artifactTypes()),
                    normalizeList(item.retrievalTags()),
                    normalizeFreeList(item.restrictions())
            ));
        }
        sources = List.copyOf(items);
    }

    private void loadBranchInference() {
        JsonNode root = resourceLoader.readTree(LegalKnowledgeResourcePaths.BRANCH_INFERENCE_CATALOG);
        defaultBranches = normalizeList(readArray(root.path("defaultBranches")));
        ArrayList<BranchInferenceRule> rules = new ArrayList<>();
        JsonNode ruleNodes = root.path("rules");
        if (ruleNodes.isArray()) {
            for (JsonNode ruleNode : ruleNodes) {
                String branchCode = normalizeText(ruleNode.path("branchCode").asText());
                List<String> keywords = readArray(ruleNode.path("keywords")).stream()
                        .map(this::normalizeSearchText)
                        .filter(item -> !item.isBlank())
                        .toList();
                if (!branchCode.isBlank() && !keywords.isEmpty()) {
                    rules.add(new BranchInferenceRule(branchCode, keywords));
                }
            }
        }
        branchRules = List.copyOf(rules);
        if (defaultBranches.isEmpty()) {
            defaultBranches = List.of("CONSTITUCIONAL", "CIVIL");
        }
    }

    private void loadPolicyCatalog() {
        JsonNode root = resourceLoader.readTree(LegalKnowledgeResourcePaths.INGESTION_POLICY_CATALOG);
        priorityOrder = normalizeList(readArray(root.path("priorityOrder")));
        ingestionPolicies = normalizeFreeList(readArray(root.path("ingestionPolicies")));
        officialOnlyReasons = normalizeFreeList(readArray(root.path("selectionReasons").path("officialOnly")));
        officialPlusDoctrineReasons = normalizeFreeList(readArray(root.path("selectionReasons").path("officialPlusDoctrine")));
        doctrinePolicy = normalizePolicy(root.path("doctrinePolicy"));
        if (priorityOrder.isEmpty()) {
            priorityOrder = List.of(
                    "CONSTITUTION_AND_CODES",
                    "OFFICIAL_NORMATIVE_TEXT",
                    "BINDING_PRECEDENTS_AND_THEMES",
                    "SUMULAS_AND_OJS",
                    "OFFICIAL_JURISPRUDENCE",
                    "LICENSED_DOCTRINE_AND_INTERNAL_MANUALS"
            );
        }
        if (ingestionPolicies.isEmpty()) {
            ingestionPolicies = List.of(
                    "Constituição, códigos, leis complementares e leis ordinárias devem entrar por fonte oficial consolidada.",
                    "Precedentes vinculantes, temas repetitivos, súmulas e orientações jurisprudenciais devem entrar por fonte oficial do tribunal competente.",
                    "Doutrina e livros só podem entrar por licença, propriedade comprovada, acervo institucional ou upload controlado.",
                    "Promoção para grounding permanente exige metadados de origem, versão, data de coleta e hash do conteúdo."
            );
        }
        if (officialOnlyReasons.isEmpty()) {
            officialOnlyReasons = List.of(
                    "Cobertura oficial priorizada por ramo inferido na conversa.",
                    "Ordem de fundamentação preserva hierarquia normativa e precedencial antes de material doutrinário.",
                    "Sem doutrina acoplada no catálogo inicial; usar apenas se houver licença, upload controlado ou acervo institucional válido."
            );
        }
        if (officialPlusDoctrineReasons.isEmpty()) {
            officialPlusDoctrineReasons = List.of(
                    "Cobertura oficial priorizada por ramo inferido na conversa.",
                    "Ordem de fundamentação preserva hierarquia normativa e precedencial antes de material doutrinário.",
                    "Há lane específica para doutrina licenciada e acervo interno controlado."
            );
        }
    }

    private Map<String, Object> normalizePolicy(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        forEachObjectField(node, (key, value) -> {
            if (value.isArray()) {
                out.put(key, normalizeFreeList(readArray(value)));
            } else if (value.isBoolean()) {
                out.put(key, value.asBoolean());
            } else if (value.isNumber()) {
                out.put(key, value.numberValue());
            } else {
                String textValue = value.asText();
                if (!textValue.isBlank()) {
                    out.put(key, textValue.trim());
                }
            }
        });
        return Collections.unmodifiableMap(out);
    }

    private void forEachObjectField(JsonNode node, java.util.function.BiConsumer<String, JsonNode> consumer) {
        if (node == null || !node.isObject()) {
            return;
        }
        var fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            consumer.accept(fieldName, node.path(fieldName));
        }
    }

    private boolean isOfficial(LegalKnowledgeSourceDescriptor item) {
        return "PRIMARY_OFFICIAL".equals(item.authorityLevel()) || "OFFICIAL_CURATED".equals(item.authorityLevel());
    }

    private boolean intersects(List<String> branches, Set<String> requested) {
        if (branches == null || branches.isEmpty()) {
            return true;
        }
        for (String item : branches) {
            if (requested.contains(item)) {
                return true;
            }
        }
        return false;
    }

    private String flattenContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        context.forEach((key, value) -> {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(key).append(' ').append(String.valueOf(value));
        });
        return builder.toString();
    }

    private List<String> readArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        node.forEach(item -> {
            String value = item.asText();
            if (value != null && !value.isBlank()) {
                out.add(value.trim());
            }
        });
        return List.copyOf(out);
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String item : values) {
            String value = normalizeText(item);
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return List.copyOf(normalized);
    }

    private List<String> normalizeFreeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String item : values) {
            if (item != null && !item.isBlank()) {
                normalized.add(item.trim());
            }
        }
        return List.copyOf(normalized);
    }

    private String normalizeText(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .trim();
        return NON_ALNUM.matcher(normalized.toLowerCase(Locale.ROOT)).replaceAll("_").replaceAll("^_+|_+$", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeSearchText(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record SourceSeed(
            String sourceId,
            String title,
            String sourceKind,
            String authorityLevel,
            String institution,
            String storageLane,
            String licensingModel,
            String baseUrl,
            String refreshStrategy,
            List<String> branches,
            List<String> artifactTypes,
            List<String> retrievalTags,
            List<String> restrictions
    ) {
    }

    private record BranchInferenceRule(String branchCode, List<String> normalizedKeywords) {
        private boolean matches(String haystack) {
            for (String keyword : normalizedKeywords) {
                if (!keyword.isBlank() && haystack.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }
    }
}
