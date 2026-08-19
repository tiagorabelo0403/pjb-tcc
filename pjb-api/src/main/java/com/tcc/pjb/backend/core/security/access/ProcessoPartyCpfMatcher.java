package com.tcc.pjb.backend.core.security.access;

import com.tcc.pjb.backend.model.entity.Processo;
import org.springframework.stereotype.Component;

@Component
public class ProcessoPartyCpfMatcher {

    public PartyMatchResult match(String cpf, Processo processo) {
        if (cpf == null || cpf.isBlank() || processo == null) {
            return new PartyMatchResult.NotMatched();
        }
        if (cpf.equals(processo.getParteAutoraCpf())) {
            return new PartyMatchResult.Matched(PartyRole.AUTOR);
        }
        if (cpf.equals(processo.getParteReuCpf())) {
            return new PartyMatchResult.Matched(PartyRole.REU);
        }
        if (processo.getUsuario() != null && cpf.equals(processo.getUsuario().getCpf())) {
            return new PartyMatchResult.Matched(PartyRole.USUARIO_VINCULADO);
        }
        return new PartyMatchResult.NotMatched();
    }
}
