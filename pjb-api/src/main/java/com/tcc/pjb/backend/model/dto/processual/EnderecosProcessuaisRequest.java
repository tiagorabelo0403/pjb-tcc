package com.tcc.pjb.backend.model.dto.processual;

import com.tcc.pjb.backend.model.entity.enums.processual.CriterioTerritorial;

public record EnderecosProcessuaisRequest(
        AncoraTerritorial domicilioAutor,
        AncoraTerritorial domicilioReu,
        AncoraTerritorial localPrestacaoServico,
        AncoraTerritorial localDoFato,
        boolean domicilioReuDesconhecido
) {
    public static EnderecosProcessuaisRequest vazio() {
        return new EnderecosProcessuaisRequest(null, null, null, null, false);
    }

    public AncoraTerritorial ancoraPara(CriterioTerritorial criterio) {
        return switch (criterio) {
            case DOMICILIO_REU -> domicilioReuDesconhecido ? null : domicilioReu;
            case DOMICILIO_AUTOR_HERANCA, DOMICILIO_ALIMENTANDO -> domicilioAutor;
            case SITUACAO_DA_COISA, LOCAL_DO_FATO -> localDoFato;
            case LOCAL_PRESTACAO_SERVICO -> localPrestacaoServico;
        };
    }
}
