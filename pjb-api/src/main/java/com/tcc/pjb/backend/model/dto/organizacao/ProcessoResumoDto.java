package com.tcc.pjb.backend.model.dto.organizacao;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public record ProcessoResumoDto(
        Long id,
        String numeroUnificado,
        String vara,
        String comarca,
        RitoProcessual rito,
        FaseProcessual faseAtual,
        StatusProcesso statusProcesso,
        String parteAutoraNome,
        String parteReuNome
) {
    public static ProcessoResumoDto de(Processo p) {
        return new ProcessoResumoDto(
                p.getId(), p.getNumeroUnificado(),
                p.getVara(), p.getComarca(),
                p.getRito(), p.getFaseAtual(), p.getStatusProcesso(),
                p.getParteAutoraNome(), p.getParteReuNome()
        );
    }
}
