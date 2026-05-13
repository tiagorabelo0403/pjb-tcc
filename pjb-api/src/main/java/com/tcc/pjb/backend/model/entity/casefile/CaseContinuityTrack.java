package com.tcc.pjb.backend.model.entity.casefile;

import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;

public enum CaseContinuityTrack {
    CONHECIMENTO(10),
    VINCULADO(15),
    RECURSAL(20),
    TRANSITO(30),
    CUMPRIMENTO(40),
    EXECUCAO(45),
    ARQUIVADO(50),
    REATIVADO(60);

    private final int priorityRank;

    CaseContinuityTrack(int priorityRank) {
        this.priorityRank = priorityRank;
    }

    public int priorityRank() {
        return priorityRank;
    }


    public boolean isKnowledgeState() {
        return this == CONHECIMENTO || this == VINCULADO;
    }

    public boolean isExecutory() {
        return this == CUMPRIMENTO || this == EXECUCAO;
    }

    public boolean isRecursalState() {
        return this == RECURSAL || this == TRANSITO;
    }

    public boolean isTerminalState() {
        return this == ARQUIVADO;
    }

    public boolean isReactivatedState() {
        return this == REATIVADO;
    }

    public boolean requiresRecursalMesh() {
        return this == RECURSAL || this == TRANSITO;
    }

    public boolean requiresExecutoryMesh() {
        return this == CUMPRIMENTO || this == EXECUCAO;
    }

    public boolean requiresRemediationSweep() {
        return this == RECURSAL || this == TRANSITO || this == CUMPRIMENTO || this == EXECUCAO || this == REATIVADO;
    }

    public boolean supportsProductionSeal() {
        return this != VINCULADO;
    }

    public boolean acceptsRole(CaseProceedingRole role) {
        if (role == null) {
            return false;
        }
        return switch (this) {
            case CONHECIMENTO -> role == CaseProceedingRole.ROOT || role == CaseProceedingRole.VINCULADO || role == CaseProceedingRole.INCIDENTE;
            case VINCULADO -> role == CaseProceedingRole.VINCULADO || role == CaseProceedingRole.INCIDENTE;
            case RECURSAL, TRANSITO -> role == CaseProceedingRole.RECURSAL || role == CaseProceedingRole.ROOT;
            case CUMPRIMENTO -> role == CaseProceedingRole.CUMPRIMENTO || role == CaseProceedingRole.ROOT;
            case EXECUCAO -> role == CaseProceedingRole.EXECUCAO || role == CaseProceedingRole.CUMPRIMENTO || role == CaseProceedingRole.ROOT;
            case ARQUIVADO -> role == CaseProceedingRole.TERMINAL || role == CaseProceedingRole.ROOT || role == CaseProceedingRole.VINCULADO;
            case REATIVADO -> role != CaseProceedingRole.TERMINAL;
        };
    }

    public boolean supportsFase(FaseProcessual faseAtual) {
        if (faseAtual == null) {
            return true;
        }
        return switch (this) {
            case CONHECIMENTO, VINCULADO -> faseAtual.isKnowledgeLike() || faseAtual == FaseProcessual.LIQUIDACAO;
            case RECURSAL, TRANSITO -> faseAtual.isRecursal();
            case CUMPRIMENTO, EXECUCAO -> faseAtual.isExecutionLike();
            case ARQUIVADO, REATIVADO -> true;
        };
    }

    public boolean supportsStatus(StatusProcesso statusAtual) {
        if (statusAtual == null) {
            return true;
        }
        return switch (this) {
            case CONHECIMENTO, VINCULADO -> statusAtual.isAtivo() && !statusAtual.isRecursalOuEmbargos() && !statusAtual.isExecutorio();
            case RECURSAL -> statusAtual.isRecursalOuEmbargos();
            case TRANSITO -> statusAtual.isTransitado();
            case CUMPRIMENTO, EXECUCAO -> statusAtual.isExecutorio() || statusAtual.isTransitado();
            case ARQUIVADO -> statusAtual.isArquivadoOuBaixado();
            case REATIVADO -> statusAtual.isAtivo();
        };
    }

    public static CaseContinuityTrack dominant(CaseContinuityTrack current, CaseContinuityTrack next) {
        if (current == null) return next == null ? CONHECIMENTO : next;
        if (next == null) return current;
        return current.priorityRank() >= next.priorityRank() ? current : next;
    }

    public static CaseContinuityTrack resolve(ProcessoLifecycleAction action,
                                              FaseProcessual faseAtual,
                                              StatusProcesso statusAtual) {
        if (action == ProcessoLifecycleAction.DESARQUIVAR) return REATIVADO;
        if (action == ProcessoLifecycleAction.ARQUIVAR) return ARQUIVADO;
        if (action == ProcessoLifecycleAction.INICIAR_CUMPRIMENTO) return CUMPRIMENTO;
        if (action == ProcessoLifecycleAction.CERTIFICAR_TRANSITO) return TRANSITO;
        if (statusAtual == null && faseAtual == null) return CONHECIMENTO;
        if (statusAtual != null) {
            if (statusAtual.isArquivadoOuBaixado()) return ARQUIVADO;
            if (statusAtual.isTransitado()) return TRANSITO;
        }
        if (faseAtual != null) {
            if (faseAtual.isRecursal()) return RECURSAL;
            if (faseAtual == FaseProcessual.EXECUCAO) return EXECUCAO;
            if (faseAtual == FaseProcessual.CUMPRIMENTO_SENTENCA) return CUMPRIMENTO;
        }
        if (statusAtual != null && statusAtual.isExecutorio()) {
            return CUMPRIMENTO;
        }
        return CONHECIMENTO;
    }
}
