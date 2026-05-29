package com.tcc.pjb.backend.modules.laiane.security;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.context.CurrentAuthenticationContextService;
import com.tcc.pjb.backend.integration.govbr.GovBrAssuranceLevel;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeOficio;
import com.tcc.pjb.backend.modules.laiane.model.LaianeOficioStatus;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public final class LaianeOficioAccessGuard {

    private final CurrentUserService currentUserService;
    private final CurrentAuthenticationContextService authContextService;

    public LaianeOficioAccessGuard(CurrentUserService currentUserService,
                                   CurrentAuthenticationContextService authContextService) {
        this.currentUserService = currentUserService;
        this.authContextService = authContextService;
    }

    public void requireRead(LaianeOficio oficio) {
        Usuario me = currentUserService.get();
        if (!canRead(me, oficio)) {
            throw new AccessDeniedException("Acesso negado ao ofício.");
        }
    }

    public void requireUpdateStatus(LaianeOficio oficio) {
        Usuario me = currentUserService.get();
        if (!canUpdateStatus(me, oficio)) {
            throw new AccessDeniedException("Operação negada para este ofício.");
        }
    }

    public void requireHighAssuranceForCancellation(LaianeOficioStatus targetStatus) {
        if (targetStatus != LaianeOficioStatus.CANCELADO) return;
        String acr = authContextService.current().acr();
        if (!GovBrAssuranceLevel.meetsMinimum(acr, GovBrAssuranceLevel.OURO)) {
            throw new AccessDeniedException("Cancelamento de ofício exige nível de segurança Ouro.");
        }
    }

    private boolean canRead(Usuario me, LaianeOficio oficio) {
        return sameUser(me, oficio == null ? null : oficio.getOrigem())
                || sameUser(me, oficio == null ? null : oficio.getDestino())
                || sameInstitution(me, oficio == null ? null : oficio.getOrigem())
                || sameInstitution(me, oficio == null ? null : oficio.getDestino());
    }

    private boolean canUpdateStatus(Usuario me, LaianeOficio oficio) {
        return sameUser(me, oficio == null ? null : oficio.getDestino())
                || sameInstitution(me, oficio == null ? null : oficio.getDestino());
    }

    private boolean sameUser(Usuario left, Usuario right) {
        return left != null
                && right != null
                && left.getId() != null
                && Objects.equals(left.getId(), right.getId());
    }

    private boolean sameInstitution(Usuario left, Usuario right) {
        if (left == null || right == null) {
            return false;
        }
        UUID leftIdentity = left.getIdentidadeJuridicaId();
        UUID rightIdentity = right.getIdentidadeJuridicaId();
        return leftIdentity != null && Objects.equals(leftIdentity, rightIdentity);
    }
}
