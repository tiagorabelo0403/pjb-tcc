package com.tcc.pjb.backend.model.dto.pericia;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeritoNomeacaoRequest {

    @NotNull
    private Long peritoId;

    private String observacao;
}
