package com.tcc.pjb.backend.model.entity.enums;

import java.util.Locale;

public enum InstitutionalOrganizationScope {
    FORUM,
    SECRETARIA_UNIDADE_JUDICIARIA,
    CENTRAL_AUDIENCIAS,
    CENTRAL_MANDADOS,
    PROMOTORIA,
    NUCLEO_DEFENSORIA,
    PROCURADORIA_PUBLICA,
    DELEGACIA,
    POLICIA_PENAL,
    UNIDADE_PRISIONAL,
    CEJUSC,
    CONTADORIA,
    EQUIPE_PSICOSSOCIAL,
    CARTORIO_INTEGRADO,
    CONSELHO_TUTELAR,
    ORGAO_TECNICO_CONVENIADO,
    COOPERACAO_JUDICIAL_EXTERNA,
    GENERICO_INSTITUCIONAL;

    public static final InstitutionalOrganizationScope VARA = SECRETARIA_UNIDADE_JUDICIARIA;

    public static InstitutionalOrganizationScope fromTexto(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String token = raw.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        token = switch (token) {
            case "SECRETARIA", "SECRETARIA_FORUM", "SECRETARIA_UNIDADE", "CORREGEDORIA_DO_FORO" -> "SECRETARIA_UNIDADE_JUDICIARIA";
            case "AUDIENCIAS", "CENTRAL_DE_AUDIENCIAS", "CENTRAL_DE_PAUTA", "AGENDAMENTO_AUDIENCIA" -> "CENTRAL_AUDIENCIAS";
            case "MANDADOS", "CENTRAL_DE_MANDADOS", "CUMPRIMENTO_MANDADOS" -> "CENTRAL_MANDADOS";
            case "MP", "MINISTERIO_PUBLICO", "MINISTERIO_PUBLICO_ESTADUAL" -> "PROMOTORIA";
            case "DEFENSORIA", "DEFENSORIA_PUBLICA" -> "NUCLEO_DEFENSORIA";
            case "PROCURADORIA", "ADVOCACIA_PUBLICA", "AGU", "PROCURADORIA_ESTADO", "PROCURADORIA_MUNICIPIO" -> "PROCURADORIA_PUBLICA";
            case "POLICIA_CIVIL", "DELEGACIA_POLICIA", "DELEGACIA_POLICIA_CIVIL", "DELEGACIA_POLICIA_FEDERAL" -> "DELEGACIA";
            case "PRISIONAL", "ADMINISTRACAO_PRISIONAL", "SOCIOEDUCATIVO", "CASE" -> "UNIDADE_PRISIONAL";
            case "PSICOSSOCIAL" -> "EQUIPE_PSICOSSOCIAL";
            case "CARTORIO", "CARTORIO_EXTRAJUDICIAL" -> "CARTORIO_INTEGRADO";
            case "COOPERACAO", "JUIZO_DEPRECADO" -> "COOPERACAO_JUDICIAL_EXTERNA";
            case "DIRETORIA_DO_FORO" -> "FORUM";
            case "NATJUS" -> "ORGAO_TECNICO_CONVENIADO";
            default -> token;
        };
        try {
            return InstitutionalOrganizationScope.valueOf(token);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
