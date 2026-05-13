package com.tcc.pjb.backend.core.comunicacao.institucional.processual.application;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;

record InstitutionalProcessWorkspaceSnapshot(
        RitoProcessual rito,
        FaseProcessual fase,
        StatusProcesso status,
        RamoDireito ramo,
        boolean urgente,
        boolean recursal,
        boolean embargos,
        boolean execucao
) {
}
