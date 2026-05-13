package com.tcc.pjb.backend.model.entity.casefile;

public enum CaseProceedingRole {
    TERMINAL(10),
    VINCULADO(20),
    INCIDENTE(30),
    RECURSAL(40),
    CUMPRIMENTO(50),
    EXECUCAO(60),
    ROOT(70);

    private final int priorityRank;

    CaseProceedingRole(int priorityRank) {
        this.priorityRank = priorityRank;
    }

    public int priorityRank() {
        return priorityRank;
    }


    public boolean isKnowledgeBranch() {
        return this == ROOT || this == VINCULADO || this == INCIDENTE;
    }

    public boolean isRootLike() {
        return this == ROOT || this == VINCULADO;
    }

    public boolean isExecutoryBranch() {
        return this == CUMPRIMENTO || this == EXECUCAO;
    }

    public boolean isRecursalBranch() {
        return this == RECURSAL;
    }

    public boolean isTerminalBranch() {
        return this == TERMINAL;
    }

    public boolean requiresStructuralParent() {
        return this == RECURSAL || this == CUMPRIMENTO || this == EXECUCAO || this == INCIDENTE;
    }

    public CaseContinuityTrack suggestedTrack() {
        return switch (this) {
            case TERMINAL -> CaseContinuityTrack.ARQUIVADO;
            case VINCULADO, INCIDENTE, ROOT -> CaseContinuityTrack.CONHECIMENTO;
            case RECURSAL -> CaseContinuityTrack.RECURSAL;
            case CUMPRIMENTO -> CaseContinuityTrack.CUMPRIMENTO;
            case EXECUCAO -> CaseContinuityTrack.EXECUCAO;
        };
    }

    public boolean acceptsTrack(CaseContinuityTrack track) {
        if (track == null) {
            return false;
        }
        return switch (this) {
            case TERMINAL -> track.isTerminalState() || track.isReactivatedState();
            case VINCULADO -> track == CaseContinuityTrack.CONHECIMENTO || track == CaseContinuityTrack.VINCULADO || track.isReactivatedState();
            case INCIDENTE -> track == CaseContinuityTrack.CONHECIMENTO || track == CaseContinuityTrack.VINCULADO || track == CaseContinuityTrack.RECURSAL;
            case RECURSAL -> track.isRecursalState();
            case CUMPRIMENTO -> track == CaseContinuityTrack.CUMPRIMENTO || track == CaseContinuityTrack.EXECUCAO;
            case EXECUCAO -> track == CaseContinuityTrack.EXECUCAO || track == CaseContinuityTrack.CUMPRIMENTO;
            case ROOT -> !track.isTerminalState();
        };
    }

    public static CaseProceedingRole dominant(CaseProceedingRole current, CaseProceedingRole next) {
        if (current == null) return next == null ? VINCULADO : next;
        if (next == null) return current;
        return current.priorityRank() >= next.priorityRank() ? current : next;
    }
}
