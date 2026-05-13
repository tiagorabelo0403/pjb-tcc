package com.tcc.pjb.backend.service.jurisprudencia.search;

import com.tcc.pjb.backend.model.dto.jurisprudencia.JurisprudenceContextualSearchResponse;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class JurisprudenceContextualSearchService {

    private final JurisprudenceSearchEngine searchEngine;

    public JurisprudenceContextualSearchService(JurisprudenceSearchEngine searchEngine) {
        this.searchEngine = Objects.requireNonNull(searchEngine);
    }

    public JurisprudenceContextualSearchResponse search(String query, RamoDireito ramo, RitoProcessual rito, int topK) {
        String normalized = normalize(query);
        List<String> expandedQueries = buildExpandedQueries(normalized, ramo, rito);
        ArrayList<JurisprudenceSearchHit> merged = new ArrayList<>();
        LinkedHashSet<Long> seen = new LinkedHashSet<>();
        for (String expanded : expandedQueries) {
            for (JurisprudenceSearchHit hit : searchEngine.search(expanded, ramo, rito, topK)) {
                Long id = hit.id();
                if (id != null && !seen.add(id)) {
                    continue;
                }
                merged.add(hit);
                if (merged.size() >= topK) {
                    break;
                }
            }
            if (merged.size() >= topK) {
                break;
            }
        }
        return new JurisprudenceContextualSearchResponse(
                normalized,
                ramo == null ? null : ramo.name(),
                rito == null ? null : rito.name(),
                List.copyOf(expandedQueries),
                inferSignals(ramo, rito, normalized),
                List.copyOf(merged)
        );
    }

    private List<String> buildExpandedQueries(String query, RamoDireito ramo, RitoProcessual rito) {
        LinkedHashSet<String> variants = new LinkedHashSet<>();
        if (!query.isBlank()) {
            variants.add(query);
        }
        if (ramo != null) {
            for (String synonym : ramoSynonyms(ramo)) {
                variants.add(query.isBlank() ? synonym : query + " " + synonym);
            }
        }
        if (rito != null) {
            for (String synonym : ritoSynonyms(rito)) {
                variants.add(query.isBlank() ? synonym : query + " " + synonym);
            }
        }
        if (variants.isEmpty()) {
            variants.add(query);
        }
        return List.copyOf(variants);
    }

    private List<String> inferSignals(RamoDireito ramo, RitoProcessual rito, String query) {
        ArrayList<String> signals = new ArrayList<>();
        if (ramo != null) {
            signals.add("ramo=" + ramo.name());
        }
        if (rito != null) {
            signals.add("rito=" + rito.name());
        }
        if (query.contains("precedente") || query.contains("tema") || query.contains("tese")) {
            signals.add("foco-precedente");
        }
        if (query.contains("nulidade") || query.contains("cerceamento") || query.contains("contraditório") || query.contains("contraditorio")) {
            signals.add("foco-garantias-processuais");
        }
        if (query.contains("cumprimento") || query.contains("execução") || query.contains("execucao")) {
            signals.add("foco-fase-executiva");
        }
        if (signals.isEmpty()) {
            signals.add("busca-contextual-pjb");
        }
        return List.copyOf(signals);
    }

    private List<String> ramoSynonyms(RamoDireito ramo) {
        return switch (ramo) {
            case PENAL -> List.of("crime", "ação penal", "processo penal");
            case CIVIL -> List.of("responsabilidade civil", "processo civil", "obrigação");
            case TRABALHISTA -> List.of("relação de trabalho", "verbas trabalhistas", "execução trabalhista");
            case ELEITORAL -> List.of("registro de candidatura", "prestação de contas", "propaganda eleitoral");
            case MILITAR -> List.of("crime militar", "auditoria militar", "disciplina militar");
            case TRIBUTARIO -> List.of("execução fiscal", "tributo", "lançamento tributário");
            case PREVIDENCIARIO -> List.of("benefício previdenciário", "aposentadoria", "INSS");
            case ADMINISTRATIVO -> List.of("fazenda pública", "servidor público", "improbidade");
            default -> List.of(ramo.name().toLowerCase(Locale.ROOT).replace('_', ' '));
        };
    }

    private List<String> ritoSynonyms(RitoProcessual rito) {
        String base = rito.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add(base);
        if (base.contains("juizado")) {
            values.add("juizado especial");
        }
        if (base.contains("sumario") || base.contains("sumário")) {
            values.add("rito célere");
        }
        if (base.contains("ordinario") || base.contains("ordinário")) {
            values.add("rito comum");
        }
        return List.copyOf(values);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
