package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

public record ProcessoContexto(
        Long processoId,
        String processoNumero,
        RamoDireito ramoDireito,
        GrauJurisdicao grauJurisdicao,
        String uf,
        String comarca,
        String foro,
        String faseProcessual,
        String poloProcessual,
        boolean haIncapaz,
        boolean urgente
) {}
