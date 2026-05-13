package com.tcc.pjb.backend.model.dto.magistratura;

public enum MagistraturaJudicialActCode {
    DESPACHO,
    DECISAO_INTERLOCUTORIA,
    SENTENCA,
    DESIGNAR_AUDIENCIA,
    ORDEM_CUMPRIMENTO_OFICIAL,
    CERTIDAO_TRANSITO_JULGADO,
    NOMEACAO_PERITO,
    DESPACHO_RELATOR,
    DECISAO_MONOCRATICA,
    VOTO_COLEGIADO,
    ACORDAO,
    PEDIDO_VISTA,
    DESTAQUE,
    INCLUSAO_PAUTA,
    DECISAO_PLENARIA;

    public static MagistraturaJudicialActCode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("action é obrigatória");
        }
        return valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
    }
}
