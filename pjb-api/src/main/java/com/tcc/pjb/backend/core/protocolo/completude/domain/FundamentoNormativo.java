package com.tcc.pjb.backend.core.protocolo.completude.domain;

import com.tcc.pjb.backend.model.entity.enums.processual.completude.FonteNormativaTipo;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.GrauExigibilidade;

public record FundamentoNormativo(
        FonteNormativaTipo tipo,
        String identificador,
        String resumo,
        GrauExigibilidade grau,
        String autoridadeOrigem
) {}
