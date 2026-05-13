package com.tcc.pjb.backend.core.dje;

import com.tcc.pjb.backend.core.dje.domain.DjeEnvioCommand;
import com.tcc.pjb.backend.core.dje.domain.DjeEnvioResult;

public interface DjeHttpClient {

    default DjeEnvioResult enviar(DjeEnvioCommand command) {
        return enviar(command.tribunalCodigo(), command.conteudo(), command.tipoAto());
    }

    DjeEnvioResult enviar(String tribunalCodigo, String conteudo, String tipoAto);
}
