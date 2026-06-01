package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeAgendaEventDto {
    private Long id;
    private Long processoId;
    private String tipo;
    private String status;
    private String titulo;
    private String descricao;
    @Schema(description = "Início do evento na agenda judicial (ISO-8601 com timezone)", example = "2026-06-10T09:00:00-03:00")
    private OffsetDateTime dataInicio;
    @Schema(description = "Fim do evento na agenda judicial (ISO-8601 com timezone)", example = "2026-06-10T11:00:00-03:00")
    private OffsetDateTime dataFim;
    private boolean conflict;
    private int conflictCount;
}
