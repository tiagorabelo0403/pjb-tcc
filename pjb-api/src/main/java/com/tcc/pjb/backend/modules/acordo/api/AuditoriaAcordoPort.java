package com.tcc.pjb.backend.modules.acordo.api;

public interface AuditoriaAcordoPort {

    void registrarEvento(AuditoriaAcordoCommand command);

    void registrarEventoSensivel(AuditoriaAcordoCommand command);

    void registrarTentativaNegada(AuditoriaAcordoCommand command);
}
