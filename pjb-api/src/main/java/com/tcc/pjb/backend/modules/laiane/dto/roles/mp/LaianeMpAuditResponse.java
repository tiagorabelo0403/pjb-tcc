package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;

@Schema(description = "Resposta de auditoria comportamental do Ministério Público no Laiane")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeMpAuditResponse {
    @Schema(description = "Número da página atual (base 0)", example = "0")
    private int page;
    @Schema(description = "Tamanho da página", example = "20")
    private int size;
    @Schema(description = "Total de eventos de auditoria encontrados", example = "42")
    private long total;
    @Size(max = 1000)
    @Schema(description = "Eventos de auditoria da página atual (máx. 1000)")
    private List<LaianeMpAuditEventDto> items;
}
