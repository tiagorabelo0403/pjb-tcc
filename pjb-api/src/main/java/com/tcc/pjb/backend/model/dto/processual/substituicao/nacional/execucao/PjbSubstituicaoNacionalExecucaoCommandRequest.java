package com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoModo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;

public record PjbSubstituicaoNacionalExecucaoCommandRequest(
        @NotBlank @Size(max = 24) String tribunalCodigo,
        @NotNull PjbSubstituicaoExecucaoAcao acao,
        PjbSubstituicaoExecucaoModo modoExecucao,
        Boolean dryRun,
        @Size(max = 64) String ondaAlvo,
        @Size(max = 1000) String justificativa,
        Map<String, Object> metadados
) {
    public PjbSubstituicaoNacionalExecucaoCommandRequest {
        metadados = metadados == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(metadados));
    }
}
