package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CalculoJudicialTabelaOficialItemResponse(
        String codigo,
        String dominio,
        String titulo,
        String orgaoOficial,
        String urlOficial,
        String modoAdocaoPjb,
        String referenciaTemporal,
        @Schema(description = "Início da vigência da tabela no PJB", format = "date",
                example = "2026-01-01") String vigenciaPjbInicio,
        @Schema(description = "Fim da vigência da tabela no PJB", format = "date",
                example = "2026-12-31") String vigenciaPjbFim,
        String fingerprint,
        String algoritmoFingerprint,
        @Schema(description = "Cobertura da tabela oficial por dominio juridico — estrutura varia por tipo (trabalhista/previdenciario/federal)", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> cobertura,
        @Schema(description = "Links de interoperabilidade com sistemas externos — estrutura varia por tribunal e sistema-alvo", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> interoperabilidade,
        @Schema(description = "Diff de atualizacao da tabela oficial — comparacao com versao anterior vigente", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> diffAtual,
        @Schema(description = "Trilha de auditoria de atualizacoes da tabela — historico de versoes com autoria e data", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<Map<String, Object>> trilhaAtualizacao
) {
}

