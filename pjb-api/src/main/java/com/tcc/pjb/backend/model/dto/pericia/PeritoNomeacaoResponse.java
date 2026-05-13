package com.tcc.pjb.backend.model.dto.pericia;

import java.time.LocalDateTime;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacaoStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeritoNomeacaoResponse {

    private Long id;
    private Long processoId;
    private Long peritoId;
    private String peritoNome;
    private PeritoNomeacaoStatus status;
    private LocalDateTime nomeadoEm;
    private Long nomeadoPorId;
    private LocalDateTime respondidoEm;
    private Long respondidoPorId;
    private LocalDateTime revogadoEm;
    private Long revogadoPorId;
    private String observacao;
}
