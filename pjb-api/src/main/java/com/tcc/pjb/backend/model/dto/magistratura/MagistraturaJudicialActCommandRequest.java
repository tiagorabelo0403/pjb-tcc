package com.tcc.pjb.backend.model.dto.magistratura;

import java.time.Instant;
import java.util.List;

public record MagistraturaJudicialActCommandRequest(
        String action,
        String conteudo,
        String fundamentacao,
        String dispositivo,
        String tipo,
        String local,
        Instant dataHora,
        Long oficialId,
        Long peritoId,
        Integer prioridade,
        Integer diasVista,
        String observacao,
        String relatorio,
        String voto,
        String decisao,
        String ementa,
        String orgao,
        String votacao,
        List<String> providencias,
        Boolean encaminharAutomaticamenteSecretaria
) {
    public MagistraturaJudicialActCode resolvedAction() {
        return MagistraturaJudicialActCode.parse(action);
    }
}
