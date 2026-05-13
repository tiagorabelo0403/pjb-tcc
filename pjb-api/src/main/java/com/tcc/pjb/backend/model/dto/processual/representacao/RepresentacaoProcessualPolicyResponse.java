package com.tcc.pjb.backend.model.dto.processual.representacao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RepresentacaoProcessualPolicyResponse(
        String requestedInstrument,
        String resolvedInstrument,
        String regimePostulacao,
        String perfilAtor,
        String ramoDireito,
        String ritoProcessual,
        String tribunalCodigo,
        boolean exigeProcuracaoFormal,
        boolean admiteApudActa,
        boolean admiteJusPostulandi,
        boolean dispensaMandatoPorFuncaoInstitucional,
        boolean exigePoderesEspeciaisTransigir,
        boolean exigeTermoOuAtaAudiencia,
        boolean contextoAudiencia,
        boolean contextoConsensual,
        boolean regularidadeSuficiente,
        List<String> fundamentosLegais,
        List<String> documentosBase,
        List<String> documentosAudiencia,
        List<String> validacoesObrigatorias,
        List<String> alertas,
        List<String> atosPermitidos,
        Map<String, Object> envelope
) {
    public String instrumentoResolvido() {
        return resolvedInstrument;
    }

    public RepresentacaoProcessualPolicyResponse {
        fundamentosLegais = fundamentosLegais == null ? List.of() : List.copyOf(fundamentosLegais);
        documentosBase = documentosBase == null ? List.of() : List.copyOf(documentosBase);
        documentosAudiencia = documentosAudiencia == null ? List.of() : List.copyOf(documentosAudiencia);
        validacoesObrigatorias = validacoesObrigatorias == null ? List.of() : List.copyOf(validacoesObrigatorias);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        atosPermitidos = atosPermitidos == null ? List.of() : List.copyOf(atosPermitidos);
        envelope = envelope == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(envelope));
    }
}
