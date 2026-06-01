package com.tcc.pjb.backend.modules.laiane.dto.roles.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeMovimentacaoLiteDto {
    private Long id;
    @Schema(description = "Data e hora da movimentação processual (ISO-8601 com timezone)", example = "2026-05-31T14:00:00-03:00")
    private OffsetDateTime dataMovimentacao;
    private String tipo;
    private String descricao;
}
