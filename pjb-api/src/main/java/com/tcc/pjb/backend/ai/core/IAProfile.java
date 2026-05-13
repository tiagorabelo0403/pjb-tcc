package com.tcc.pjb.backend.ai.core;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum IAProfile {
    JUIZ_ESTADUAL,
    JUIZ_FEDERAL,
    JUIZ_ESPECIAL,
    JUIZ_ELEITORAL,
    JUIZ_TRABALHISTA,
    JUIZ_MILITAR,
    DESEMBARGADOR,
    DESEMBARGADOR_FEDERAL,
    MINISTRO,
    ASSESSOR_JUDICIAL,
    ASSESSOR_DESEMBARGADOR,
    ASSESSOR_MINISTRO,
    PROMOTOR,
    PROMOTOR_ELEITORAL,
    PROMOTOR_TRABALHISTA,
    PROCURADOR_GERAL_REPUBLICA,
    DEFENSOR_ESTADUAL,
    DEFENSOR_FEDERAL,
    PROCURADOR_MUNICIPAL,
    PROCURADOR_ESTADUAL,
    PROCURADOR_FEDERAL,
    DELEGADO_ESTADUAL,
    DELEGADO_FEDERAL,
    OFICIAL_JUSTICA,
    OFICIAL_JUSTICA_AVALIADOR,
    PERITO_GERAL,
    PERITO_CRIMINAL,
    PERITO_AMBIENTAL,
    PERITO_CONTABIL,
    PERITO_ENGENHARIA,
    PERITO_DIGITAL,
    PERITO_MEDICO,
    PSICOLOGO_JUDICIAL,
    ASSISTENTE_SOCIAL_JUDICIAL,
    CONCILIADOR,
    MEDIADOR,
    ARBITRO,
    CONTADOR_JUDICIAL,
    ADMINISTRADOR_JUDICIAL,
    LEILOEIRO_JUDICIAL,
    CURADOR_ESPECIAL,
    CURADOR_AUSENTES,
    TABELIAO,
    REGISTRADOR_IMOVEIS,
    ESCREVENTE_CARTORIO,
    ADVOGADO,
    DEFENSOR_PUBLICO,
    PARTE_AUTORA,
    PARTE_RE,
    MINISTERIO_PUBLICO,
    FAZENDA_PUBLICA,
    SERVIDOR_JUDICIAL,
    SISTEMA,
    PERITO;

    public boolean isMagistratura() {
        return switch (this) {
            case JUIZ_ESTADUAL,
                    JUIZ_FEDERAL,
                    JUIZ_ESPECIAL,
                    JUIZ_ELEITORAL,
                    JUIZ_TRABALHISTA,
                    JUIZ_MILITAR,
                    DESEMBARGADOR,
                    DESEMBARGADOR_FEDERAL,
                    MINISTRO -> true;
            default -> false;
        };
    }

    public boolean isParteProcessual() {
        return this == PARTE_AUTORA || this == PARTE_RE;
    }

    public boolean isOrgaoJusticaPublica() {
        return switch (this) {
            case MINISTERIO_PUBLICO,
                    PROMOTOR,
                    PROMOTOR_ELEITORAL,
                    PROMOTOR_TRABALHISTA,
                    PROCURADOR_GERAL_REPUBLICA,
                    DEFENSOR_ESTADUAL,
                    DEFENSOR_FEDERAL,
                    DEFENSOR_PUBLICO,
                    PROCURADOR_MUNICIPAL,
                    PROCURADOR_ESTADUAL,
                    PROCURADOR_FEDERAL,
                    FAZENDA_PUBLICA -> true;
            default -> false;
        };
    }

    public boolean isAuxiliarJustica() {
        return switch (this) {
            case PERITO_GERAL,
                    PERITO_CRIMINAL,
                    PERITO_AMBIENTAL,
                    PERITO_CONTABIL,
                    PERITO_ENGENHARIA,
                    PERITO_DIGITAL,
                    PERITO_MEDICO,
                    PSICOLOGO_JUDICIAL,
                    ASSISTENTE_SOCIAL_JUDICIAL,
                    PERITO,
                    OFICIAL_JUSTICA,
                    OFICIAL_JUSTICA_AVALIADOR,
                    CONTADOR_JUDICIAL,
                    ADMINISTRADOR_JUDICIAL,
                    LEILOEIRO_JUDICIAL,
                    CURADOR_ESPECIAL,
                    CURADOR_AUSENTES,
                    CONCILIADOR,
                    MEDIADOR,
                    ARBITRO,
                    TABELIAO,
                    REGISTRADOR_IMOVEIS,
                    ESCREVENTE_CARTORIO -> true;
            default -> false;
        };
    }

    public Set<IACapability> capabilities() {
        return IACapability.defaultsFor(this);
    }

    public static Optional<IAProfile> tryParse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String token = raw.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_')
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
        try {
            return Optional.of(IAProfile.valueOf(token));
        } catch (Exception ex) {
            return fromTipoUsuario(token);
        }
    }

    public static Optional<IAProfile> fromTipoUsuario(TipoUsuario tipoUsuario) {
        if (tipoUsuario == null) {
            return Optional.empty();
        }
        return fromTipoUsuario(tipoUsuario.name());
    }

    public static Optional<IAProfile> fromTipoUsuario(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String token = raw.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        return Optional.ofNullable(switch (token) {
            case "JUIZ", "JUIZ_ESTADUAL" -> JUIZ_ESTADUAL;
            case "JUIZ_FEDERAL" -> JUIZ_FEDERAL;
            case "JUIZ_TRABALHISTA" -> JUIZ_TRABALHISTA;
            case "JUIZ_ELEITORAL" -> JUIZ_ELEITORAL;
            case "JUIZ_MILITAR" -> JUIZ_MILITAR;
            case "DESEMBARGADOR" -> DESEMBARGADOR;
            case "MINISTRO" -> MINISTRO;
            case "ASSESSOR", "ASSESSOR_JUDICIAL" -> ASSESSOR_JUDICIAL;
            case "PROMOTOR" -> PROMOTOR;
            case "PROMOTOR_ELEITORAL" -> PROMOTOR_ELEITORAL;
            case "PROMOTOR_TRABALHISTA" -> PROMOTOR_TRABALHISTA;
            case "PROCURADOR_GERAL_REPUBLICA" -> PROCURADOR_GERAL_REPUBLICA;
            case "DEFENSOR_PUBLICO" -> DEFENSOR_PUBLICO;
            case "DEFENSOR_ESTADUAL" -> DEFENSOR_ESTADUAL;
            case "DEFENSOR_FEDERAL" -> DEFENSOR_FEDERAL;
            case "ADVOGADO" -> ADVOGADO;
            case "OFICIAL_JUSTICA" -> OFICIAL_JUSTICA;
            case "PERITO" -> PERITO;
            case "PSICOLOGO_JUDICIAL" -> PSICOLOGO_JUDICIAL;
            case "ASSISTENTE_SOCIAL_JUDICIAL" -> ASSISTENTE_SOCIAL_JUDICIAL;
            case "MINISTERIO_PUBLICO" -> MINISTERIO_PUBLICO;
            case "FAZENDA_PUBLICA" -> FAZENDA_PUBLICA;
            case "SERVIDOR", "SERVIDOR_JUDICIAL" -> SERVIDOR_JUDICIAL;
            case "PARTE_AUTORA" -> PARTE_AUTORA;
            case "PARTE_RE", "PARTE_REU" -> PARTE_RE;
            case "DELEGADO", "DELEGADO_ESTADUAL" -> DELEGADO_ESTADUAL;
            case "DELEGADO_FEDERAL" -> DELEGADO_FEDERAL;
            case "CONCILIADOR" -> CONCILIADOR;
            case "MEDIADOR" -> MEDIADOR;
            case "SISTEMA" -> SISTEMA;
            default -> null;
        });
    }
}
