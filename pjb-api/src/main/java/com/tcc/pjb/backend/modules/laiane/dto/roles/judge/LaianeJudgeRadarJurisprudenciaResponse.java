package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

@Schema(description = "Radar de jurisprudência gerado pelo Laiane para o magistrado")
public record LaianeJudgeRadarJurisprudenciaResponse(
        @Schema(description = "ID do processo analisado", example = "12345") Long processoId,
        @Schema(description = "Número CNJ do processo", example = "0001234-56.2026.8.06.0001") String numeroProcesso,
        @Schema(description = "Rito processual aplicável", example = "RITO_ORDINARIO") String rito,
        @Schema(description = "Ramo do direito identificado", example = "PENAL") String ramoDireito,
        @Schema(description = "Status da análise",
                example = "CONCLUIDO", allowableValues = {"CONCLUIDO", "PARCIAL", "ERRO"}) String status,
        @Schema(description = "Índice de aderência contextual à jurisprudência (0.0 a 1.0)", example = "0.87") double aderenciaContextual,
        @Size(max = 20)
        @Schema(description = "Consultas de jurisprudência sugeridas (máx. 20)") List<String> consultasSugeridas,
        @Size(max = 20)
        @Schema(description = "Cautelas identificadas pela análise (máx. 20)") List<String> cautelas,
        @Size(max = 10)
        @Schema(description = "Eixos de análise jurisprudencial (máx. 10)") List<String> eixosAnalise,
        @Size(max = 50)
        @Schema(description = "Hits de jurisprudência relevante (máx. 50)") List<LaianeJudgeRadarHitDto> hits,
        @Schema(description = "Instante de geração do radar (UTC ISO-8601)", example = "2026-05-31T12:00:00Z") Instant generatedAt
) {
}
