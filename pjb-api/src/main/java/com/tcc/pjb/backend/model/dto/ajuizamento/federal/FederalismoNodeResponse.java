package com.tcc.pjb.backend.model.dto.ajuizamento.federal;

import java.time.Instant;
import java.util.Set;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.federalismo.NoFederacaoJudicial;
import com.tcc.pjb.backend.model.entity.federalismo.StatusNoFederacao;

public record FederalismoNodeResponse(
        Long id,
        String codigoTribunal,
        String nome,
        String uf,
        TipoJustica tipoJustica,
        String endpointPrincipal,
        String endpointBackup,
        String kafkaBrokers,
        String chavePublicaFingerprint,
        StatusNoFederacao statusAtual,
        Instant ultimaHeartbeatEm,
        Instant ultimaSincronizacaoEm,
        long backlogPendente,
        long capacidadeBacklog,
        boolean operacaoAutonomaAtiva,
        boolean aceitaRecepcaoEventos,
        long versaoSchemaAtual,
        String regiao,
        String zona,
        int prioridadeFailover,
        long clockLogico,
        double disponibilidadeFederativa,
        Set<String> topicosPermitidos,
        Set<String> capacidades
) {
    public static FederalismoNodeResponse of(NoFederacaoJudicial no) {
        return new FederalismoNodeResponse(
                no.getId(),
                no.getCodigoTribunal(),
                no.getNome(),
                no.getUf(),
                no.getTipoJustica(),
                no.getEndpointPrincipal(),
                no.getEndpointBackup(),
                no.getKafkaBrokers(),
                no.getChavePublicaFingerprint(),
                no.getStatusAtual(),
                no.getUltimaHeartbeatEm(),
                no.getUltimaSincronizacaoEm(),
                no.getBacklogPendente(),
                no.getCapacidadeBacklog(),
                no.isOperacaoAutonomaAtiva(),
                no.isAceitaRecepcaoEventos(),
                no.getVersaoSchemaAtual(),
                no.getRegiao(),
                no.getZona(),
                no.getPrioridadeFailover(),
                no.getClockLogico(),
                no.disponibilidadeFederativa(),
                Set.copyOf(no.getTopicosPermitidos()),
                Set.copyOf(no.getCapacidades())
        );
    }
}
