package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import java.util.Map;
import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

record NationalProceduralForumAllocationContext(
        Map<String, Object> payload,
        String corpus,
        ProceduralCanonicalResolver.CanonicalContext canonical,
        CompetenceResolveResponse competence,
        TipoJustica tipoJustica,
        String ritoSugerido,
        NationalProceduralActionProfile actionProfile,
        NationalProceduralJuizadoDecision juizadoDecision,
        String cidadeBase,
        String ufBase,
        String tribunalCodigoBase,
        String tribunalNomeBase,
        String varaBase,
        String tipoVaraBase,
        NationalProceduralDistributionSuggestion distribution
) {

    NationalProceduralForumAllocationContext {
        payload = PayloadMaps.deepCopyWithoutNulls(payload);
        corpus = trimToNull(corpus);
        ritoSugerido = trimToNull(ritoSugerido);
        cidadeBase = trimToNull(cidadeBase);
        ufBase = trimToNull(ufBase);
        tribunalCodigoBase = trimToNull(tribunalCodigoBase);
        tribunalNomeBase = trimToNull(tribunalNomeBase);
        varaBase = trimToNull(varaBase);
        tipoVaraBase = trimToNull(tipoVaraBase);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
