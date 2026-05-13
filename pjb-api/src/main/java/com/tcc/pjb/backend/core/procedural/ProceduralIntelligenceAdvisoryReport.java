package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProceduralIntelligenceAdvisoryReport(
        Instant generatedAt,
        NaturezaJuridicaCanonical naturezaPrincipal,
        List<NaturezaJuridicaQualifier> qualifiers,
        TipoJustica suggestedTipoJustica,
        RamoDireito suggestedRamo,
        RitoProcessual suggestedRito,
        MateriaJurisdicao suggestedMateria,
        NivelSigilo suggestedSigilo,
        double confidence,
        String uncertaintyLevel,
        String primaryReason,
        List<String> supportingSignals,
        List<String> discardedAlternatives,
        List<String> recommendedDocuments,
        List<String> riskFlags,
        boolean urgent,
        boolean reviewRequired,
        boolean fallbackUsed,
        Map<String, Object> metadata
) {

    public ProceduralIntelligenceAdvisoryReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        qualifiers = qualifiers == null ? List.of() : List.copyOf(qualifiers);
        supportingSignals = supportingSignals == null ? List.of() : List.copyOf(supportingSignals);
        discardedAlternatives = discardedAlternatives == null ? List.of() : List.copyOf(discardedAlternatives);
        recommendedDocuments = recommendedDocuments == null ? List.of() : List.copyOf(recommendedDocuments);
        riskFlags = riskFlags == null ? List.of() : List.copyOf(riskFlags);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("naturezaPrincipal", naturezaPrincipal != null ? naturezaPrincipal.name() : null);
        out.put("naturezaPrincipalLabel", naturezaPrincipal != null ? naturezaPrincipal.label() : null);
        out.put("qualifiers", qualifiers.stream().map(Enum::name).toList());
        out.put("suggestedTipoJustica", suggestedTipoJustica != null ? suggestedTipoJustica.name() : null);
        out.put("suggestedRamo", suggestedRamo != null ? suggestedRamo.name() : null);
        out.put("suggestedRito", suggestedRito != null ? suggestedRito.name() : null);
        out.put("suggestedMateria", suggestedMateria != null ? suggestedMateria.name() : null);
        out.put("suggestedSigilo", suggestedSigilo != null ? suggestedSigilo.name() : null);
        out.put("confidence", confidence);
        out.put("uncertaintyLevel", uncertaintyLevel);
        out.put("primaryReason", primaryReason);
        out.put("supportingSignals", supportingSignals);
        out.put("discardedAlternatives", discardedAlternatives);
        out.put("recommendedDocuments", recommendedDocuments);
        out.put("riskFlags", riskFlags);
        out.put("urgent", urgent);
        out.put("reviewRequired", reviewRequired);
        out.put("fallbackUsed", fallbackUsed);
        out.put("metadata", metadata);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
