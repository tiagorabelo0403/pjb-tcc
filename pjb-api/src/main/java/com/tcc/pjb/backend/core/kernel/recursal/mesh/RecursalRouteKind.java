package com.tcc.pjb.backend.core.kernel.recursal.mesh;

public enum RecursalRouteKind {
    INTERNAL_SAME_AUTOS,
    INTERNAL_REGIMENTAL,
    EXECUTION_INCIDENT_INTERNAL,
    SECOND_INSTANCE_EXTERNAL,
    JUIZADO_TURMA_RECURSAL,
    JUIZADO_UNIFORMIZACAO,
    SUPERIOR_EXCEPTIONAL,
    EXTRAORDINARY_EXCEPTIONAL,
    ORIGINARY_SUPERIOR,
    ORIGINARY_CONSTITUTIONAL;

    public boolean sameCourt() {
        return switch (this) {
            case INTERNAL_SAME_AUTOS, INTERNAL_REGIMENTAL, EXECUTION_INCIDENT_INTERNAL -> true;
            default -> false;
        };
    }

    public boolean externalRemessa() {
        return !sameCourt();
    }

    public boolean exceptionalUpperCourt() {
        return switch (this) {
            case SUPERIOR_EXCEPTIONAL, EXTRAORDINARY_EXCEPTIONAL, ORIGINARY_SUPERIOR, ORIGINARY_CONSTITUTIONAL -> true;
            default -> false;
        };
    }

    public String descriptor() {
        return switch (this) {
            case INTERNAL_SAME_AUTOS -> "INTERNO_MESMOS_AUTOS";
            case INTERNAL_REGIMENTAL -> "INTERNO_REGIMENTAL";
            case EXECUTION_INCIDENT_INTERNAL -> "INCIDENTE_EXECUTIVO_APARTADO_DEPENDENCIA";
            case SECOND_INSTANCE_EXTERNAL -> "REMESSA_SEGUNDO_GRAU";
            case JUIZADO_TURMA_RECURSAL -> "REMESSA_TURMA_RECURSAL";
            case JUIZADO_UNIFORMIZACAO -> "REMESSA_UNIFORMIZACAO_JUIZADO";
            case SUPERIOR_EXCEPTIONAL -> "REMESSA_SUPERIOR_EXCEPCIONAL";
            case EXTRAORDINARY_EXCEPTIONAL -> "REMESSA_EXTRAORDINARIA";
            case ORIGINARY_SUPERIOR -> "AUTUACAO_ORIGINARIA_SUPERIOR";
            case ORIGINARY_CONSTITUTIONAL -> "AUTUACAO_ORIGINARIA_CONSTITUCIONAL";
        };
    }
}
