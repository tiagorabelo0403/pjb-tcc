package com.tcc.pjb.backend.modules.laiane.dto.roles.common;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeMovimentacaoLiteDto {
    private Long id;
    private LocalDateTime dataMovimentacao;
    private String tipo;
    private String descricao;
}
