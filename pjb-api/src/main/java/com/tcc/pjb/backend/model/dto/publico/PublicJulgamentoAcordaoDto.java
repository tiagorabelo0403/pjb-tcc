package com.tcc.pjb.backend.model.dto.publico;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;






@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicJulgamentoAcordaoDto(
        Long julgamentoId,
        String grau,
        String tribunalSigla,
        String orgaoJulgador,
        String relatorNome,
        String status,

        String numeroAcordao,
        String ementaResumo,
        String inteiroTeorRef,
        LocalDateTime publicadoEm,

        
        String placarFinal
) {
}
