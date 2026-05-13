package com.tcc.pjb.backend.core.processo.recursal.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoRecursalAggregate(
        ProcessoRecursalIdentity identity,
        String familiaRecursal,
        String instanciaAtual,
        long totalCabiveis,
        long totalMesmosAutos,
        long totalExterno,
        long totalEmbargos,
        List<ProcessoRecursalJanela> janelas,
        List<String> proximosPassos,
        List<String> travas,
        List<String> alertas,
        ProcessoRecursalDecisionCarryOver cadernoDecisorioOrigem,
        Instant geradoEm
) {
    public ProcessoRecursalAggregate {
        Objects.requireNonNull(identity);
        familiaRecursal = familiaRecursal == null ? "OUTRA" : familiaRecursal;
        instanciaAtual = instanciaAtual == null ? "FIRST_INSTANCE" : instanciaAtual;
        janelas = janelas == null ? List.of() : List.copyOf(janelas);
        proximosPassos = proximosPassos == null ? List.of() : List.copyOf(proximosPassos);
        travas = travas == null ? List.of() : List.copyOf(travas);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }

    public ProcessoRecursalAggregate(ProcessoRecursalIdentity identity,
                                     String familiaRecursal,
                                     String instanciaAtual,
                                     long totalCabiveis,
                                     long totalMesmosAutos,
                                     long totalExterno,
                                     long totalEmbargos,
                                     List<ProcessoRecursalJanela> janelas,
                                     List<String> proximosPassos,
                                     List<String> travas,
                                     List<String> alertas,
                                     Instant geradoEm) {
        this(identity, familiaRecursal, instanciaAtual, totalCabiveis, totalMesmosAutos, totalExterno, totalEmbargos, janelas, proximosPassos, travas, alertas, null, geradoEm);
    }
}
