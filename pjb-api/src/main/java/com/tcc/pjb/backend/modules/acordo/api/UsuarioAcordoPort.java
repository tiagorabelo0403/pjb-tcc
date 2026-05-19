package com.tcc.pjb.backend.modules.acordo.api;

public interface UsuarioAcordoPort {

    boolean existeUsuario(Long usuarioId);

    boolean usuarioPodeParticipar(Long processoId, Long usuarioId);

    boolean usuarioPodeHomologar(Long usuarioId);
}
