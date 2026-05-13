package com.tcc.pjb.backend.service.financeiro.previdenciario;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CnisVinculo {

    
    private String empregador;

    private LocalDate inicio;
    private LocalDate fim;

    
    @Builder.Default
    private int mesesSemContribuicao = 0;
}
