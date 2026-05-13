package com.tcc.pjb.backend.core.processo.runtime.domain;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.List;
import java.util.Objects;

public record ProcessoMalhaActorContext(
        Long actorId,
        String nome,
        String cpf,
        TipoUsuario tipoUsuario,
        TipoUsuario papelEfetivo,
        RamoDireito ramoEfetivo,
        List<String> roles,
        boolean visualizacaoElevada,
        boolean visualizacaoContextual,
        boolean parteRelacionada
) {
    public ProcessoMalhaActorContext {
        nome = Objects.toString(nome, "").trim();
        cpf = Objects.toString(cpf, "").trim();
        tipoUsuario = tipoUsuario == null ? TipoUsuario.CIDADAO : tipoUsuario;
        papelEfetivo = papelEfetivo == null ? tipoUsuario : papelEfetivo;
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
