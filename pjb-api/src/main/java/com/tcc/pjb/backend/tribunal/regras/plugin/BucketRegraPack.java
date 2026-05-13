package com.tcc.pjb.backend.tribunal.regras.plugin;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;


    public record BucketRegraPack(
            String tribunalCodigo,
            RamoDireito ramo,
            GrauJurisdicao grau
    ) {}
