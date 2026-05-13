package com.tcc.pjb.backend.model.dto.ajuizamento.federal;

import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.federalismo.StatusNoFederacao;

public record FederalismoNodeUpsertRequest(
        @NotBlank String codigoTribunal,
        @NotBlank String nome,
        String uf,
        @NotNull TipoJustica tipoJustica,
        @NotBlank String endpointPrincipal,
        String endpointBackup,
        String kafkaBrokers,
        String chavePublicaBase64,
        StatusNoFederacao statusAtual,
        Long versaoSchemaAtual,
        Long capacidadeBacklog,
        Boolean operacaoAutonomaAtiva,
        Boolean aceitaRecepcaoEventos,
        String regiao,
        String zona,
        Integer prioridadeFailover,
        Set<String> topicosPermitidos,
        Set<String> capacidades
) {
}
