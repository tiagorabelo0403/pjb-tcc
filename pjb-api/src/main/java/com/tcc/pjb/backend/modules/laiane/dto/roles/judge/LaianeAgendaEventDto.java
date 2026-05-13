package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import java.time.LocalDateTime;
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
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private boolean conflict;
    private int conflictCount;
}
