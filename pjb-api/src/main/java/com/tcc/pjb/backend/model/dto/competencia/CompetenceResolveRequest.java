package com.tcc.pjb.backend.model.dto.competencia;

import java.math.BigDecimal;
import jakarta.validation.constraints.Size;

public record CompetenceResolveRequest(
        
        @Size(max = 30_000) String textoCaso,

        
        @Size(max = 180) String assunto,
        @Size(max = 120) String classeProcessual,
        @Size(max = 120) String materia,
        @Size(max = 2) String uf,
        @Size(max = 120) String comarca,

        
        BigDecimal valorCausa,

        
        Boolean envolveUniao,
        Boolean envolveAutarquiaFederal,
        Boolean envolveEmpresaPublicaFederal,
        Boolean envolveEstado,
        Boolean envolveMunicipio,
        Boolean envolveRelacaoTrabalho,
        Boolean envolveEleitoral,
        Boolean envolveMilitar
) {
}
