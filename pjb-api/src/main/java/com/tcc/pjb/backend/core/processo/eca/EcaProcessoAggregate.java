package com.tcc.pjb.backend.core.processo.eca;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record EcaProcessoAggregate(
        Long processoId,
        String numeroUnificado,
        EcaRitoNivel ritoNivel,
        EcaSigiloPolicy sigiloPolicy,
        EcaAudienciaApresentacaoRule audienciaApresentacaoRule,
        boolean mpNotificado,
        boolean conselhoTutelarNotificado,
        boolean defensorConstituido,
        String medidaSocioeducativaAtual,
        List<String> revisoesProgramadas,
        Instant criadoEm
) {
    public EcaProcessoAggregate {
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(ritoNivel, "ritoNivel");
        sigiloPolicy = sigiloPolicy == null ? EcaSigiloPolicy.padrao() : sigiloPolicy;
        audienciaApresentacaoRule = audienciaApresentacaoRule == null ? EcaAudienciaApresentacaoRule.padrao() : audienciaApresentacaoRule;
        revisoesProgramadas = revisoesProgramadas == null ? List.of() : List.copyOf(revisoesProgramadas);
        criadoEm = criadoEm == null ? Instant.now() : criadoEm;
    }
}
