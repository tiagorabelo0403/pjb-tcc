package com.tcc.pjb.backend.model.entity.enums.processual.completude;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

public enum TipoRepresentanteProcessual {

    ADVOGADO_PRIVADO,
    DEFENSOR_PUBLICO,
    MINISTERIO_PUBLICO,
    PARTE_SEM_ADVOGADO;

    public static TipoRepresentanteProcessual doAtor(TipoUsuario tipo) {
        if (tipo == null) return PARTE_SEM_ADVOGADO;
        if (tipo.isDefensoriaPublica()) return DEFENSOR_PUBLICO;
        if (tipo.isMinisterioPublico() || tipo.isProcuradoria()) return MINISTERIO_PUBLICO;
        if (tipo.isAdvocacia()) return ADVOGADO_PRIVADO;
        return PARTE_SEM_ADVOGADO;
    }
}
