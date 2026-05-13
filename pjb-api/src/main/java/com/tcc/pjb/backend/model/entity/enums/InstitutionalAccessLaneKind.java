package com.tcc.pjb.backend.model.entity.enums;

import java.util.Locale;

public enum InstitutionalAccessLaneKind {
    ADMINISTRACAO_MESTRA,
    DIRETORIA,
    SECRETARIA,
    TRIAGEM,
    TITULAR,
    ASSESSORIA,
    AGENDAMENTO_AUDIENCIA,
    AGENDAMENTO_CONCILIACAO,
    CENTRAL_MANDADOS,
    CARTORIO_POLICIAL,
    CUSTODIA,
    DIRECAO_PRISIONAL,
    APOIO_TECNICO,
    CONTADORIA,
    PSICOSSOCIAL,
    COOPERACAO,
    ATENDIMENTO_INSTITUCIONAL;

    public static InstitutionalAccessLaneKind fromTexto(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String token = raw.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        token = switch (token) {
            case "ADMINISTRADOR", "ADMINISTRADOR_INSTITUCIONAL", "GESTAO", "GESTAO_MESTRA" -> "ADMINISTRACAO_MESTRA";
            case "AGENDA_AUDIENCIA", "AGENDADOR_AUDIENCIA" -> "AGENDAMENTO_AUDIENCIA";
            case "AGENDA_CONCILIACAO", "AGENDADOR_CONCILIACAO" -> "AGENDAMENTO_CONCILIACAO";
            case "DIRECAO" -> "DIRECAO_PRISIONAL";
            default -> token;
        };
        try {
            return InstitutionalAccessLaneKind.valueOf(token);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
