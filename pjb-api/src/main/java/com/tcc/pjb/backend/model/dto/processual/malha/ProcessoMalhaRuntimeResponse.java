package com.tcc.pjb.backend.model.dto.processual.malha;

import java.time.Instant;
import java.util.List;

public record ProcessoMalhaRuntimeResponse(
        String numeroProcesso,
        String numeroUnificado,
        String ramoDireito,
        String ritoProcessual,
        String papelPrincipal,
        String tribunal,
        String vara,
        String comarca,
        String uf,
        boolean sigiloReforcado,
        int percentualProntidao,
        boolean prontoMinimo,
        List<String> componentesAusentes,
        List<String> alertas,
        String fingerprint,
        Instant preparadoEm
) {
    public ProcessoMalhaRuntimeResponse {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso.trim();
        numeroUnificado = numeroUnificado == null ? "" : numeroUnificado.trim();
        ramoDireito = ramoDireito == null ? "NAO_INFORMADO" : ramoDireito.trim();
        ritoProcessual = ritoProcessual == null ? "NAO_INFORMADO" : ritoProcessual.trim();
        papelPrincipal = papelPrincipal == null ? "CIDADAO" : papelPrincipal.trim();
        tribunal = tribunal == null ? "" : tribunal.trim();
        vara = vara == null ? "" : vara.trim();
        comarca = comarca == null ? "" : comarca.trim();
        uf = uf == null ? "" : uf.trim();
        componentesAusentes = componentesAusentes == null ? List.of() : List.copyOf(componentesAusentes);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        fingerprint = fingerprint == null ? "" : fingerprint.trim();
        preparadoEm = preparadoEm == null ? Instant.now() : preparadoEm;
    }
}
