package com.tcc.pjb.backend.service.financeiro.previdenciario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CnisResultado {

    private boolean possuiLacunas;

    
    private String recomendacao;
}
