package com.tcc.pjb.backend.core.processo.runtime.domain;

import java.util.ArrayList;
import java.util.List;

public record ProcessoRuntimeIntegrationStatus(
        boolean decisionTraceDisponivel,
        boolean auditLedgerDisponivel,
        boolean outboxDisponivel,
        boolean securityAlertDisponivel,
        boolean observabilidadeNacionalDisponivel,
        boolean observabilidadeProcessualDisponivel,
        boolean processoPersistidoDisponivel,
        boolean documentoProcessualDisponivel,
        boolean usuarioDisponivel,
        boolean distribuicaoNacionalDisponivel,
        boolean competenciaNacionalDisponivel
) {
    public int percentualProntidao() {
        List<Boolean> estados = List.of(
                decisionTraceDisponivel,
                auditLedgerDisponivel,
                outboxDisponivel,
                securityAlertDisponivel,
                observabilidadeNacionalDisponivel,
                observabilidadeProcessualDisponivel,
                processoPersistidoDisponivel,
                documentoProcessualDisponivel,
                usuarioDisponivel,
                distribuicaoNacionalDisponivel,
                competenciaNacionalDisponivel
        );
        long ativos = estados.stream().filter(Boolean::booleanValue).count();
        return (int) Math.round((ativos * 100.0d) / estados.size());
    }

    public boolean prontoMinimo() {
        return processoPersistidoDisponivel
                && distribuicaoNacionalDisponivel
                && competenciaNacionalDisponivel
                && outboxDisponivel;
    }

    public List<String> componentesAusentes() {
        List<String> ausentes = new ArrayList<>();
        if (!decisionTraceDisponivel) ausentes.add("decisionTrace");
        if (!auditLedgerDisponivel) ausentes.add("auditLedger");
        if (!outboxDisponivel) ausentes.add("outbox");
        if (!securityAlertDisponivel) ausentes.add("securityAlert");
        if (!observabilidadeNacionalDisponivel) ausentes.add("observabilidadeNacional");
        if (!observabilidadeProcessualDisponivel) ausentes.add("observabilidadeProcessual");
        if (!processoPersistidoDisponivel) ausentes.add("processoRepository");
        if (!documentoProcessualDisponivel) ausentes.add("documentoProcessualRepository");
        if (!usuarioDisponivel) ausentes.add("usuarioRepository");
        if (!distribuicaoNacionalDisponivel) ausentes.add("distribuicaoNacional");
        if (!competenciaNacionalDisponivel) ausentes.add("competenciaNacional");
        return List.copyOf(ausentes);
    }


    public boolean usuarioRepositoryDisponivel() {
        return usuarioDisponivel;
    }

    public boolean segurancaDisponivel() {
        return securityAlertDisponivel;
    }
}
