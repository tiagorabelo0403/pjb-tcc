package com.tcc.pjb.backend.service.exception;

import java.util.Collections;
import java.util.List;

public class EquipeNaoSelecionadaException extends RuntimeException {

    private final String requiredHeader;
    private final List<Long> candidateEquipeIds;

    public EquipeNaoSelecionadaException() {
        this("Equipe não selecionada.", "X-Equipe-ID", List.of());
    }

    public EquipeNaoSelecionadaException(String message, String requiredHeader, List<Long> candidateEquipeIds) {
        super(message != null && !message.isBlank() ? message : "Equipe não selecionada.");
        this.requiredHeader = requiredHeader != null && !requiredHeader.isBlank() ? requiredHeader : "X-Equipe-ID";
        this.candidateEquipeIds = candidateEquipeIds == null ? List.of() : Collections.unmodifiableList(candidateEquipeIds);
    }

    public String getRequiredHeader() {
        return requiredHeader;
    }

    public List<Long> getCandidateEquipeIds() {
        return candidateEquipeIds;
    }
}
