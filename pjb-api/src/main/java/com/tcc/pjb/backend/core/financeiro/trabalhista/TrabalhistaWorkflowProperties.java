package com.tcc.pjb.backend.core.financeiro.trabalhista;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.trabalhista")
public record TrabalhistaWorkflowProperties(
        boolean enabled,
        String indiceAtualizacaoDefault,
        int prazoPagamentoGruDias,
        BigDecimal depositoRecursalTetoDefault
) {
}
