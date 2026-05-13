package com.tcc.pjb.backend.journey;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.domain.enums.TipoJustica;

abstract class JourneyTestSupport {
    protected Processo processo(RitoProcessual rito, StatusProcesso status, FaseProcessual fase) {
        Processo processo = Processo.builder()
                .id(1L)
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(rito.suggestedRamo())
                .rito(rito)
                .statusProcesso(status)
                .faseAtual(fase)
                .numeroUnificado("0000000-00.2026.8.06.0001")
                .numeroProcesso("0000000-00.2026.8.06.0001")
                .build();
        return processo;
    }
}
