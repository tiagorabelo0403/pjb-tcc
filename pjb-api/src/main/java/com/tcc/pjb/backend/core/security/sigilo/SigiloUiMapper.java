package com.tcc.pjb.backend.core.security.sigilo;

import com.tcc.pjb.backend.model.dto.publico.SigiloUiDTO;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;

public final class SigiloUiMapper {

    private SigiloUiMapper() {
    }

    public static SigiloUiDTO toUi(NivelSigilo sigilo) {
        NivelSigilo s = sigilo != null ? sigilo : NivelSigilo.PUBLICO;
        return new SigiloUiDTO(
                s.exigeCredencial(),
                s.nivel(),
                s.label(),
                s.icon(),
                s.color(),
                s.mensagemPublica()
        );
    }
}
