package com.tcc.pjb.backend.core.security;

import com.tcc.pjb.backend.model.entity.Processo;

public final class ProcessoPartyCpfLinkPolicy {

    private ProcessoPartyCpfLinkPolicy() {
    }

    public static boolean vinculado(String cpf, Processo processo) {
        if (cpf == null || cpf.isBlank() || processo == null) {
            return false;
        }
        return cpf.equals(processo.getParteAutoraCpf())
                || cpf.equals(processo.getParteReuCpf())
                || (processo.getUsuario() != null && cpf.equals(processo.getUsuario().getCpf()));
    }
}
