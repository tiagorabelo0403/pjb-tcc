package com.tcc.pjb.backend.core.security.persona;

import java.util.Objects;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

public record UserPersona(
        TipoUsuario tipoUsuario,
        PersonaKey personaKey,
        String displayPerfil,
        String tratamento,
        GrauJurisdicao grau,
        EsferaJurisdicao esfera,
        boolean delegacaoAtiva
) {

    public UserPersona {
        Objects.requireNonNull(personaKey, "personaKey");
        Objects.requireNonNull(displayPerfil, "displayPerfil");
        Objects.requireNonNull(tratamento, "tratamento");
    }
}
