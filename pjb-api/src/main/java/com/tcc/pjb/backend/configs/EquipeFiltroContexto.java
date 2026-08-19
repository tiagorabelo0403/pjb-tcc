package com.tcc.pjb.backend.configs;

import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService.WorkspaceProcessFilterProfile;

/**
 * Contexto resolvido pelo {@link EquipeSwitchInterceptor#preHandle} e consumido por
 * {@link EquipeFiltroRepositoryAspect}. Guarda os parametros ja calculados (usuario, equipe,
 * perfil de workspace) num ThreadLocal simples: a mesma thread atende o preHandle, o
 * controller e as chamadas de repositorio dentro de uma requisicao MockMvc/Tomcat sincrona
 * (confirmado por {@code RequestCorrelationFilter}, que envolve toda a cadeia num unico
 * {@code Runnable}), entao nao ha necessidade de propagacao entre threads aqui.
 */
final class EquipeFiltroContexto {

    private static final ThreadLocal<Estado> PENDENTE = new ThreadLocal<>();

    private EquipeFiltroContexto() {
    }

    static void set(Estado estado) {
        if (estado == null) {
            PENDENTE.remove();
        } else {
            PENDENTE.set(estado);
        }
    }

    static Estado getOrNull() {
        return PENDENTE.get();
    }

    static void clear() {
        PENDENTE.remove();
    }

    record Estado(long usuarioId, long equipeIdParam, WorkspaceProcessFilterProfile processoProfile) {
    }
}
