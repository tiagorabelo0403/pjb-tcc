package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeJudgeAgendaResponse {
    private LocalDateTime inicio;
    private LocalDateTime fim;
    private int total;
    private int totalConflitos;
    private List<LaianeAgendaEventDto> events;
}
