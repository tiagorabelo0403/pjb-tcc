package com.tcc.pjb.backend.model.dto.processual.completude.certificacao;

import java.time.Instant;
import java.util.List;

public record ProcessoCertificacaoOperacionalResponse(
        Long processoId,
        String numeroProcesso,
        int percentualCobertura,
        boolean possuiFalhaCritica,
        List<String> modulosCriticos,
        List<ProcessoCertificacaoOperacionalItemResponse> itens,
        Instant geradoEm
) {
    public ProcessoCertificacaoOperacionalResponse {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso;
        modulosCriticos = modulosCriticos == null ? List.of() : List.copyOf(modulosCriticos);
        itens = itens == null ? List.of() : List.copyOf(itens);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
