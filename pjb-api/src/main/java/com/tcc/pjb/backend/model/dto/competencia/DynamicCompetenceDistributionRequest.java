package com.tcc.pjb.backend.model.dto.competencia;

import java.math.BigDecimal;
import jakarta.validation.constraints.Size;

public record DynamicCompetenceDistributionRequest(
        @Size(max = 50) String nupn,
        @Size(max = 80) String classeTpu,
        @Size(max = 80) String assuntoTpu,
        @Size(max = 60) String ramoDireito,
        BigDecimal valorCausa,
        @Size(max = 2) String ufAutor,
        @Size(max = 120) String comarcaAutor,
        @Size(max = 2) String ufReu,
        @Size(max = 120) String comarcaReu,
        Boolean requerJuizadoEspecial,
        Boolean requerVaraEspecializada,
        @Size(max = 120) String materiaPrincipal,
        @Size(max = 30) String tipoJustica,
        Boolean casoUrgente,
        Boolean preferenciaDigital,
        Long processoId
) {
}
