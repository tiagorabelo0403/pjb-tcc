package com.tcc.pjb.backend.model.dto.judge;

import java.time.LocalDateTime;
import java.util.List;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeDocketItemDto {

    private Long processoId;
    private String numero;
    private String classeProcessual;
    private String assunto;
    private String jurisdicao;
    private StatusProcesso status;
    private FaseProcessual fase;
    private LocalDateTime ultimaMovimentacao;

    
    private int urgencyScore;
    private List<String> reasons;
}
