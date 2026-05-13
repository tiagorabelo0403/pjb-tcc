package com.tcc.pjb.backend.service.processual.recursal.workspace;

public record PerfilRecursalDescriptor(String profileCode,
                                        String historyScope,
                                        String[] allowedRoles,
                                        String peticaoQueueCode,
                                        String recursoQueueCode,
                                        int dueHours,
                                        boolean autoIsencaoBase,
                                        boolean ministerioPublico,
                                        boolean institucional) {

    public boolean isMinisterioPublico() {
        return ministerioPublico;
    }

    public boolean isInstitucional() {
        return institucional;
    }
}
