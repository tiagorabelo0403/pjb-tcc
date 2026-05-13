package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.math.BigDecimal;
import java.util.Locale;

public enum PjbDigitalCoreRitoKind {
    ORDINARIO,
    JUIZADO_ESPECIAL_CIVEL,
    JUIZADO_ESPECIAL_FAZENDA_PUBLICA,
    JUIZADO_ESPECIAL_CRIMINAL,
    NUCLEO_DIGITAL_ESPECIALIZADO,
    CRIMINAL,
    FAZENDA_PUBLICA,
    INFANCIA_JUVENTUDE,
    TRABALHISTA,
    ELEITORAL,
    MILITAR,
    PREVIDENCIARIO,
    FALENCIA_RECUPERACAO,
    FAMILIA,
    DESCONHECIDO;

    public boolean juizado() {
        return this == JUIZADO_ESPECIAL_CIVEL
                || this == JUIZADO_ESPECIAL_FAZENDA_PUBLICA
                || this == JUIZADO_ESPECIAL_CRIMINAL;
    }

    public boolean ritoDigitalPreferencial() {
        return juizado() || this == NUCLEO_DIGITAL_ESPECIALIZADO;
    }

    public RitoProcessual toCanonicalRito() {
        return switch (this) {
            case JUIZADO_ESPECIAL_CIVEL -> RitoProcessual.JUIZADO_ESPECIAL_CIVEL;
            case JUIZADO_ESPECIAL_FAZENDA_PUBLICA -> RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA;
            case JUIZADO_ESPECIAL_CRIMINAL -> RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL;
            case CRIMINAL -> RitoProcessual.PROCEDIMENTO_PENAL_COMUM;
            case FAZENDA_PUBLICA -> RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO;
            case INFANCIA_JUVENTUDE -> RitoProcessual.INFANCIA_JUVENTUDE_ECA;
            case TRABALHISTA -> RitoProcessual.TRABALHISTA_ORDINARIO;
            case ELEITORAL -> RitoProcessual.ELEITORAL;
            case MILITAR -> RitoProcessual.MILITAR_PROCESSO_PENAL_MILITAR;
            case PREVIDENCIARIO -> RitoProcessual.PREVIDENCIARIO_COMUM;
            case FALENCIA_RECUPERACAO -> RitoProcessual.RECUPERACAO_JUDICIAL;
            case FAMILIA -> RitoProcessual.CIVIL_FAMILIA_DIVORCIO;
            default -> RitoProcessual.COMUM_ORDINARIO;
        };
    }

    public static PjbDigitalCoreRitoKind fromRitoProcessual(RitoProcessual rito) {
        if (rito == null) return DESCONHECIDO;
        if (rito.isJuizado()) {
            String n = rito.name();
            if (n.contains("FAZENDA")) return JUIZADO_ESPECIAL_FAZENDA_PUBLICA;
            if (n.contains("CRIMINAL")) return JUIZADO_ESPECIAL_CRIMINAL;
            return JUIZADO_ESPECIAL_CIVEL;
        }
        if (rito.isInfancia()) return INFANCIA_JUVENTUDE;
        return switch (rito.getGrupoPrincipal()) {
            case PENAL, EXECUCAO_PENAL -> CRIMINAL;
            case TRABALHISTA -> TRABALHISTA;
            case ELEITORAL -> ELEITORAL;
            case MILITAR -> MILITAR;
            case EXECUCAO_FISCAL -> FAZENDA_PUBLICA;
            case PREVIDENCIARIO -> PREVIDENCIARIO;
            case FALENCIA_RECUPERACAO -> FALENCIA_RECUPERACAO;
            case FAMILIA -> FAMILIA;
            default -> ORDINARIO;
        };
    }

    public static PjbDigitalCoreRitoKind resolve(String classeProcessual,
                                                 String assuntoPrincipal,
                                                 BigDecimal valorCausa) {
        String source = normalize(classeProcessual) + " " + normalize(assuntoPrincipal);
        if (source.contains("JUIZADO") && source.contains("FAZENDA")) {
            return JUIZADO_ESPECIAL_FAZENDA_PUBLICA;
        }
        if (source.contains("JUIZADO") && (source.contains("CRIMINAL") || source.contains("CRIME"))) {
            return JUIZADO_ESPECIAL_CRIMINAL;
        }
        if (source.contains("JUIZADO") || source.contains("SUMARISSIMO")) {
            return JUIZADO_ESPECIAL_CIVEL;
        }
        if (source.contains("NUCLEO") && source.contains("DIGITAL")) {
            return NUCLEO_DIGITAL_ESPECIALIZADO;
        }
        if (source.contains("CRIMINAL") || source.contains("CRIME")) {
            return CRIMINAL;
        }
        if (source.contains("FAZENDA") || source.contains("PUBLICA")) {
            return FAZENDA_PUBLICA;
        }
        if (source.contains("INFANCIA") || source.contains("ADOLESCENTE")) {
            return INFANCIA_JUVENTUDE;
        }
        if (valorCausa != null && valorCausa.signum() >= 0) {
            return ORDINARIO;
        }
        return DESCONHECIDO;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.strip().toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Â', 'A')
                .replace('Ã', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
    }
}
