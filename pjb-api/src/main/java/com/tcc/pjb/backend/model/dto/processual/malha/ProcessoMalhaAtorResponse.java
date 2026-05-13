package com.tcc.pjb.backend.model.dto.processual.malha;

import java.util.List;

public record ProcessoMalhaAtorResponse(
        Long actorId,
        String nome,
        String cpf,
        String tipoUsuario,
        String papelEfetivo,
        String ramoEfetivo,
        List<String> roles,
        boolean visualizacaoElevada,
        boolean visualizacaoContextual,
        boolean parteRelacionada
) {
    public ProcessoMalhaAtorResponse {
        nome = nome == null ? "" : nome.trim();
        cpf = cpf == null ? "" : cpf.trim();
        tipoUsuario = tipoUsuario == null ? "CIDADAO" : tipoUsuario.trim();
        papelEfetivo = papelEfetivo == null ? "CIDADAO" : papelEfetivo.trim();
        ramoEfetivo = ramoEfetivo == null ? "NAO_INFORMADO" : ramoEfetivo.trim();
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
