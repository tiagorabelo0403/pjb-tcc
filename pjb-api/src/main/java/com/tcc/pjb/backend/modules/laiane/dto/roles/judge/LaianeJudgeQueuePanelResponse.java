package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.*;

@Schema(description = "Painel de fila de processos do magistrado no Laiane")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeJudgeQueuePanelResponse {
    @Schema(description = "Instante de geração do painel (UTC ISO-8601)", example = "2026-05-31T12:00:00Z")
    private Instant generatedAt;
    @Schema(description = "UF do magistrado", example = "CE")
    private String uf;
    @Schema(description = "Comarca do magistrado", example = "Fortaleza")
    private String comarca;
    @Schema(description = "Total de processos na fila", example = "47")
    private int total;
    @Size(max = 20)
    @Schema(description = "Buckets de processos agrupados por critério (máx. 20)")
    private List<LaianeJudgeQueueBucketDto> buckets;
}
