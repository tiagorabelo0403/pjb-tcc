package com.tcc.pjb.backend.model.dto.intelligence;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public record CaseTriageResponse(
        String triageId,
        Instant generatedAt,
        RamoDireito ramoSugerido,
        RitoProcessual ritoSugerido,
        JurisdictionSuggestionDto jurisdicao,
        List<PrecedenteRecommendationDto> jurisprudencia,
        Map<String, Object> debug
) {

    public record JurisdictionSuggestionDto(
            boolean found,
            double confidence,
            String reason,
            String label,
            String category,
            String rite,
            List<String> authorities,
            List<String> legalBases
    ) {}

    public record PrecedenteRecommendationDto(
            Long id,
            String fonte,
            String tipo,
            String identificador,
            String titulo,
            String tese,
            double score,
            List<String> citations
    ) {}
}
