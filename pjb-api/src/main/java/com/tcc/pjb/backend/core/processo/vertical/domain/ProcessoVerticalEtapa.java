package com.tcc.pjb.backend.core.processo.vertical.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoVerticalEtapa(
        String code,
        String title,
        String phase,
        String primaryOwner,
        String queueAxis,
        String color,
        boolean blocking,
        List<String> allowedActs,
        List<String> mandatoryChecks,
        List<String> mandatoryDocuments,
        List<String> handoffTo,
        List<String> fundamentos
) {
    public ProcessoVerticalEtapa {
        Objects.requireNonNull(code);
        Objects.requireNonNull(title);
        phase = phase == null ? "NAO_INFORMADO" : phase;
        primaryOwner = primaryOwner == null ? "NAO_INFORMADO" : primaryOwner;
        queueAxis = queueAxis == null ? "TRILHA_GERAL" : queueAxis;
        color = color == null ? "slate" : color;
        allowedActs = allowedActs == null ? List.of() : List.copyOf(allowedActs);
        mandatoryChecks = mandatoryChecks == null ? List.of() : List.copyOf(mandatoryChecks);
        mandatoryDocuments = mandatoryDocuments == null ? List.of() : List.copyOf(mandatoryDocuments);
        handoffTo = handoffTo == null ? List.of() : List.copyOf(handoffTo);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
