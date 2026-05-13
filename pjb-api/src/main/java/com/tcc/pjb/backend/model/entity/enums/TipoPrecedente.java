package com.tcc.pjb.backend.model.entity.enums;

import java.util.Map;
import com.tcc.pjb.backend.core.util.EnumText;

public enum TipoPrecedente {
    SUMULA_VINCULANTE,
    REPERCUSSAO_GERAL,
    TEMA_REPETITIVO,
    IRDR,
    IAC,
    SUMULA,
    ORIENTACAO_JURISPRUDENCIAL,
    INFORMATIVO,
    ACORDAO,
    DECISAO_MONOCRATICA,
    OUTRO;

    
    public static final TipoPrecedente REPETITIVO = TEMA_REPETITIVO;

    public static TipoPrecedente fromString(String raw) {
        if (raw == null || raw.isBlank()) return OUTRO;
        String token = EnumText.normalizeToken(raw);
        if (token.isBlank()) return OUTRO;

        TipoPrecedente alias = ALIASES.get(token);
        if (alias != null) return alias;

        try {
            return TipoPrecedente.valueOf(token);
        } catch (Exception ignored) {
            return OUTRO;
        }
    }

    private static final Map<String, TipoPrecedente> ALIASES = Map.ofEntries(
            Map.entry("SV", SUMULA_VINCULANTE),
            Map.entry("SUMULA_VINC", SUMULA_VINCULANTE),
            Map.entry("SUMULA_VINCULANTE", SUMULA_VINCULANTE),

            Map.entry("RG", REPERCUSSAO_GERAL),
            Map.entry("REPERCUSSAO", REPERCUSSAO_GERAL),
            Map.entry("REPERCUSSAO_GERAL", REPERCUSSAO_GERAL),

            Map.entry("REPETITIVO", TEMA_REPETITIVO),
            Map.entry("TEMA", TEMA_REPETITIVO),
            Map.entry("TEMA_REPETITIVO", TEMA_REPETITIVO),

            Map.entry("INCIDENTE_DE_RESOLUCAO_DE_DEMANDAS_REPETITIVAS", IRDR),
            Map.entry("IRDR", IRDR),

            Map.entry("INCIDENTE_DE_ASSUNCAO_DE_COMPETENCIA", IAC),
            Map.entry("IAC", IAC),

            Map.entry("OJ", ORIENTACAO_JURISPRUDENCIAL),
            Map.entry("ORIENTACAO", ORIENTACAO_JURISPRUDENCIAL),
            Map.entry("ORIENTACAO_JURISPRUDENCIAL", ORIENTACAO_JURISPRUDENCIAL),

            Map.entry("INFORMATIVO", INFORMATIVO),
            Map.entry("ACORDAO", ACORDAO),
            Map.entry("DECISAO", DECISAO_MONOCRATICA),
            Map.entry("MONOCRATICA", DECISAO_MONOCRATICA)
    );
}
