package com.tcc.pjb.backend.model.entity.enums;

import com.tcc.pjb.backend.core.util.EnumText;

public enum InstrumentoRepresentacaoProcessual {
    MANDATO_AD_JUDICIA,
    MANDATO_AD_JUDICIA_ET_EXTRA,
    PROCURACAO_APUD_ACTA,
    JUS_POSTULANDI_TRABALHISTA,
    DEFENSORIA_PUBLICA_INSTITUCIONAL,
    ADVOCACIA_PUBLICA_INSTITUCIONAL,
    MINISTERIO_PUBLICO_FUNCIONAL,
    CURADORIA_ESPECIAL,
    PREPOSICAO_COM_PODERES;

    public boolean dispensaMandatoFormal() {
        return switch (this) {
            case PROCURACAO_APUD_ACTA,
                    JUS_POSTULANDI_TRABALHISTA,
                    DEFENSORIA_PUBLICA_INSTITUCIONAL,
                    ADVOCACIA_PUBLICA_INSTITUCIONAL,
                    MINISTERIO_PUBLICO_FUNCIONAL,
                    CURADORIA_ESPECIAL -> true;
            default -> false;
        };
    }

    public boolean isInstitucional() {
        return switch (this) {
            case DEFENSORIA_PUBLICA_INSTITUCIONAL,
                    ADVOCACIA_PUBLICA_INSTITUCIONAL,
                    MINISTERIO_PUBLICO_FUNCIONAL,
                    CURADORIA_ESPECIAL -> true;
            default -> false;
        };
    }

    public boolean exigePoderesEspeciaisTransigirPorNatureza() {
        return this == MANDATO_AD_JUDICIA_ET_EXTRA || this == PREPOSICAO_COM_PODERES;
    }

    public boolean dependeAtaOuTermo() {
        return this == PROCURACAO_APUD_ACTA;
    }

    public static InstrumentoRepresentacaoProcessual fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = EnumText.normalizeToken(raw);
        if (normalized.isBlank()) {
            return null;
        }
        normalized = switch (normalized) {
            case "PROC_APUD_ACTA", "PROCURACAO_APUDACTA", "PROCURACAO_APUD_ACTA", "APUD_ACTA", "APUDACTA" -> "PROCURACAO_APUD_ACTA";
            case "JUS_POSTULANDI", "JUS_POSTULANDI_TRABALHO", "JUS_POSTULANDI_TRABALHISTA" -> "JUS_POSTULANDI_TRABALHISTA";
            case "MANDATO", "PROC_FORO_GERAL", "PROCURACAO", "PROCURACAO_PARTICULAR", "AD_JUDICIA" -> "MANDATO_AD_JUDICIA";
            case "MANDATO_AD_JUDICIA_EXTRA", "MANDATO_AD_JUDICIA_ET_EXTRA", "PROCURACAO_PODERES_ESPECIAIS", "PROCURACAO_TRANSIGIR", "PODERES_ESPECIAIS" -> "MANDATO_AD_JUDICIA_ET_EXTRA";
            case "DEFENSORIA", "DEFENSORIA_PUBLICA", "REPRESENTACAO_DEFENSORIA" -> "DEFENSORIA_PUBLICA_INSTITUCIONAL";
            case "ADVOCACIA_PUBLICA", "AGU", "PGFN", "PROCURADORIA", "REPRESENTACAO_PROCURADORIA" -> "ADVOCACIA_PUBLICA_INSTITUCIONAL";
            case "MINISTERIO_PUBLICO", "MP", "PROMOTORIA", "REPRESENTACAO_MP" -> "MINISTERIO_PUBLICO_FUNCIONAL";
            case "CURADOR", "CURADOR_ESPECIAL" -> "CURADORIA_ESPECIAL";
            case "PREPOSTO", "PREPOSICAO", "CARTA_PREPOSICAO", "PREPOSICAO_COM_PODERES" -> "PREPOSICAO_COM_PODERES";
            default -> normalized;
        };
        try {
            return InstrumentoRepresentacaoProcessual.valueOf(normalized);
        } catch (Exception ignored) {
            return null;
        }
    }
}
