package com.tcc.pjb.backend.configs;

import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class EquipeContexto {

    private static final ThreadLocal<MembroEquipe> FALLBACK_MEMBRO_ATIVO = new ThreadLocal<>();

    private EquipeContexto() {
        
    }

    

    public static void setMembroAtivo(MembroEquipe membro) {
        if (membro != null) {
            log.trace(
                    "MembroEquipe ativo definido | UsuarioID={} | EquipeID={}",
                    membro.getUsuario() != null ? membro.getUsuario().getId() : "N/A",
                    membro.getEquipe() != null ? membro.getEquipe().getId() : "N/A"
            );
            if (RequestContext.isBound()) {
                RequestContext.setMembroEquipeAtivo(membro);
            } else {
                FALLBACK_MEMBRO_ATIVO.set(membro);
            }
        } else {
            if (RequestContext.isBound()) {
                RequestContext.setMembroEquipeAtivo(null);
            }
            FALLBACK_MEMBRO_ATIVO.remove();
        }
    }

    public static MembroEquipe getMembroDaEquipeAtiva() {
        MembroEquipe requestBound = RequestContext.getMembroEquipeAtivo().orElse(null);
        return requestBound == null ? FALLBACK_MEMBRO_ATIVO.get() : requestBound;
    }

    public static void clear() {
        if (RequestContext.isBound()) {
            RequestContext.setMembroEquipeAtivo(null);
        }
        FALLBACK_MEMBRO_ATIVO.remove();
    }

    

    
    public static boolean isUsuarioMinisterioPublico() {

        MembroEquipe membro = getMembroDaEquipeAtiva();

        if (membro == null || membro.getUsuario() == null) {
            log.debug("MP check: contexto inexistente.");
            return false;
        }

        TipoUsuario tipo = membro.getUsuario().getTipoUsuario();

        if (tipo == null) {
            log.debug("MP check: tipo de usuário não definido.");
            return false;
        }

        boolean resultado = tipo.isMinisterioPublico();

        log.trace(
                "MP check | UsuarioID={} | Tipo={} | Resultado={}",
                membro.getUsuario().getId(),
                tipo,
                resultado
        );

        return resultado;
    }
}
