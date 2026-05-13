package com.tcc.pjb.backend.core.processo.sigilo.domain;

import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoSigiloInteligenteAggregate(
        ProcessoUnificadoIdentity identity,
        NivelSigilo nivelAtual,
        NivelSigilo nivelRecomendado,
        String statusClassificacao,
        boolean revisaoJudicialObrigatoria,
        boolean decretoExclusivoMagistrado,
        boolean operacaoPolicialSigilosa,
        boolean protecaoDocumentalReforcada,
        String audienceMode,
        ProcessoSigiloJurisdicaoBridge jurisdicaoBridge,
        List<String> triggers,
        List<ProcessoSigiloDestinatario> destinatarios,
        List<ProcessoSigiloProtecaoDado> protecoesDados,
        List<ProcessoSigiloFinding> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public ProcessoSigiloInteligenteAggregate {
        Objects.requireNonNull(identity);
        nivelAtual = nivelAtual == null ? NivelSigilo.PUBLICO : nivelAtual;
        nivelRecomendado = nivelRecomendado == null ? nivelAtual : nivelRecomendado;
        statusClassificacao = statusClassificacao == null || statusClassificacao.isBlank() ? "MANTER_CLASSIFICACAO" : statusClassificacao;
        audienceMode = audienceMode == null || audienceMode.isBlank() ? "PADRAO" : audienceMode;
        Objects.requireNonNull(jurisdicaoBridge);
        triggers = triggers == null ? List.of() : List.copyOf(triggers);
        destinatarios = destinatarios == null ? List.of() : List.copyOf(destinatarios);
        protecoesDados = protecoesDados == null ? List.of() : List.copyOf(protecoesDados);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
