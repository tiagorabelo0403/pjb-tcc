package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.*;

@Schema(description = "Resposta de consolidação de bundle de processos pelo Laiane")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeCaseBundleConsolidationResponse {
    @Size(max = 2000)
    @Schema(description = "Sugestão de estratégia para o bundle consolidado",
            example = "Agrupar pedidos idênticos — possível litisconsórcio ativo")
    private String suggestion;
    @Schema(description = "ID do bundle consolidado", example = "42")
    private Long bundleId;
    @Schema(description = "Total de processos no bundle", example = "5")
    private int totalProcessos;
    @Size(max = 500)
    @Schema(description = "IDs dos processos consolidados (máx. 500)")
    private List<Long> processosIds;
    @Schema(description = "Número de itens de trabalho em aberto", example = "3")
    private int openWorkItems;
    @Schema(description = "Prazo mais próximo entre todos os processos do bundle (UTC ISO-8601)",
            example = "2026-06-15T23:59:59Z")
    private Instant earliestDueAt;
    @Size(max = 5000)
    @Schema(description = "Resumo estratégico do bundle",
            example = "Cinco processos com causa de pedir comum — padrão de conduta repetida identificado")
    private String resumo;
}
