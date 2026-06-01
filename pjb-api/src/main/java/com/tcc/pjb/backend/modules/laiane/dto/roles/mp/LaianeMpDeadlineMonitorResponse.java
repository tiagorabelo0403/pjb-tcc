package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianeWorkItemLiteDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.*;

@Schema(description = "Monitor de prazos processuais do Ministério Público no Laiane")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeMpDeadlineMonitorResponse {
    @Schema(description = "Quantidade de prazos com vencimento próximo no horizonte definido", example = "5")
    private int upcoming;
    @Schema(description = "Instante em que o monitor foi gerado (UTC ISO-8601)",
            example = "2026-05-31T12:00:00Z")
    private Instant generatedAt;
    @Schema(description = "Horizonte de consulta em dias", example = "7")
    private int horizonDays;
    @Schema(description = "Total de prazos no período", example = "12")
    private int total;
    @Schema(description = "Quantidade de prazos já vencidos", example = "2")
    private int overdue;
    @Size(max = 500)
    @Schema(description = "Lista de itens de trabalho com prazo no horizonte (máx. 500)")
    private List<LaianeWorkItemLiteDto> items;
}
