package com.tcc.pjb.backend.model.dto.jurisprudencia;

import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceSearchHit;
import java.util.List;

public record JurisprudenceContextualSearchResponse(
        String query,
        String ramo,
        String rito,
        List<String> expandedQueries,
        List<String> contextualSignals,
        List<JurisprudenceSearchHit> hits
) {
}
