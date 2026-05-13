package com.tcc.pjb.backend.model.dto.magistratura;

import java.util.Locale;

public enum MagistraturaJudicialProvidenceCode {
    PREPARAR_AUDIENCIA,
    EXPEDIR_INTIMACOES,
    PROVIDENCIAR_PUBLICACAO,
    CUMPRIR_DETERMINACAO_CARTORIO,
    PROVIDENCIAR_PERICIA,
    EXPEDIR_ORDEM_CUMPRIMENTO,
    ORGANIZAR_CONCLUSAO,
    REMETER_COLEGIADO_OU_PLENARIO,
    ABRIR_VISTA_TECNICA,
    CONTROLAR_CALCULO_LIQUIDACAO,
    IMPULSIONAR_EXECUCAO,
    SANEAR_PROCESSO,
    PROCESSAR_INCIDENTE_PROCESSUAL,
    REDISTRIBUIR_OU_PREVENIR;

    public static MagistraturaJudicialProvidenceCode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("providencia é obrigatória");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "EXPEDIR_COMUNICACAO_PROCESSUAL", "INTIMAR_OU_CITAR" -> EXPEDIR_INTIMACOES;
            case "VISTA_TECNICA", "ABRIR_VISTA_ORGAO_OBRIGATORIO" -> ABRIR_VISTA_TECNICA;
            case "CALCULO_OU_LIQUIDACAO", "LIQUIDACAO", "CONTADORIA" -> CONTROLAR_CALCULO_LIQUIDACAO;
            case "EXECUCAO", "CUMPRIMENTO_SENTENCA" -> IMPULSIONAR_EXECUCAO;
            case "SANEAMENTO" -> SANEAR_PROCESSO;
            case "INCIDENTE_PROCESSUAL" -> PROCESSAR_INCIDENTE_PROCESSUAL;
            case "REDISTRIBUICAO", "PREVENCAO" -> REDISTRIBUIR_OU_PREVENIR;
            default -> valueOf(normalized);
        };
    }
}
