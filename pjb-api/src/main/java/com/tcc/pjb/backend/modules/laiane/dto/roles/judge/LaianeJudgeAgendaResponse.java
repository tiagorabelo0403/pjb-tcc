package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.*;

@Schema(description = "Agenda de audiências e eventos do magistrado no Laiane")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeJudgeAgendaResponse {
    @Schema(description = "Início do período consultado", example = "2026-05-31T08:00:00")
    private OffsetDateTime inicio;
    @Schema(description = "Fim do período consultado", example = "2026-06-07T23:59:59")
    private OffsetDateTime fim;
    @Schema(description = "Total de eventos no período", example = "8")
    private int total;
    @Schema(description = "Total de conflitos de agenda detectados", example = "1")
    private int totalConflitos;
    @Size(max = 200)
    @Schema(description = "Eventos de agenda no período (máx. 200)")
    private List<LaianeAgendaEventDto> events;
}
