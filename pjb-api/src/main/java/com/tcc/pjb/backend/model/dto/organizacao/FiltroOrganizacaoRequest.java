package com.tcc.pjb.backend.model.dto.organizacao;

import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public record FiltroOrganizacaoRequest(
        String vara,
        String comarca,
        String uf,
        RitoProcessual rito,
        FaseProcessual faseAtual,
        StatusProcesso statusProcesso,
        String agruparPor
) {
    public String agruparPorEfetivo() {
        return agruparPor != null ? agruparPor : "vara";
    }
}
