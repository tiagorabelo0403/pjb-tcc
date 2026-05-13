package com.tcc.pjb.backend.core.kernel.recursal.mesh;

public enum RecursalTramitationMode {
    SAME_AUTOS_SAME_GRADE(
            false,
            false,
            true,
            true,
            "TRAMITACAO_COMPLETA_NO_MESMO_FEITO",
            "SEM_TRILHA_DESTACADA_NO_GRAU_DESTINO",
            "MESMOS_AUTOS_MESMO_GRAU"),
    APARTADO_DEPENDENCIA_SAME_GRADE(
            false,
            true,
            false,
            true,
            "REFERENCIA_A_AUTOS_APARTADOS_DEPENDENCIA",
            "TRAMITACAO_ATIVA_EM_APARTADO_DEPENDENCIA",
            "AUTOS_APARTADOS_DEPENDENCIA"),
    HIGHER_GRADE_SAME_NUMBERING(
            true,
            false,
            false,
            true,
            "SOMENTE_REMESSA_E_RETORNO_NO_GRAU_REMETENTE",
            "TRAMITACAO_ATIVA_NO_GRAU_DESTINO",
            "REMESSA_GRAU_SUPERIOR_MESMA_NUMERACAO"),
    HIGHER_GRADE_AUTONOMOUS(
            true,
            true,
            false,
            true,
            "REFERENCIA_A_AUTUACAO_AUTONOMA_NO_GRAU_REMETENTE",
            "TRAMITACAO_ATIVA_NA_AUTUACAO_AUTONOMA_DESTINO",
            "AUTUACAO_AUTONOMA_GRAU_SUPERIOR");

    private final boolean freezeSourceTimeline;
    private final boolean targetProceedingOwnNumber;
    private final boolean sameProceeding;
    private final boolean targetTimelineActive;
    private final String sourceTimelineMode;
    private final String targetTimelineMode;
    private final String descriptor;

    RecursalTramitationMode(boolean freezeSourceTimeline,
                            boolean targetProceedingOwnNumber,
                            boolean sameProceeding,
                            boolean targetTimelineActive,
                            String sourceTimelineMode,
                            String targetTimelineMode,
                            String descriptor) {
        this.freezeSourceTimeline = freezeSourceTimeline;
        this.targetProceedingOwnNumber = targetProceedingOwnNumber;
        this.sameProceeding = sameProceeding;
        this.targetTimelineActive = targetTimelineActive;
        this.sourceTimelineMode = sourceTimelineMode;
        this.targetTimelineMode = targetTimelineMode;
        this.descriptor = descriptor;
    }

    public boolean freezeSourceTimeline() {
        return freezeSourceTimeline;
    }

    public boolean targetProceedingOwnNumber() {
        return targetProceedingOwnNumber;
    }

    public boolean sameProceeding() {
        return sameProceeding;
    }

    public boolean targetTimelineActive() {
        return targetTimelineActive;
    }

    public String sourceTimelineMode() {
        return sourceTimelineMode;
    }

    public String targetTimelineMode() {
        return targetTimelineMode;
    }

    public String descriptor() {
        return descriptor;
    }
}
