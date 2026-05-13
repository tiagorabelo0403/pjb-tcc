package com.tcc.pjb.backend.core.security.magistratura.delegation;

import java.time.Instant;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;

@Service
public class MagistraturaAccessService {

    private final CurrentUserService currentUserService;

    public MagistraturaAccessService(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    
    public void requireDraftAccess() {
        Usuario u = currentUserService.getRequired();
        if (u.isMagistrado()) {
            return;
        }

        DelegationCredential dc = RequestContext.getDelegationCredential().orElse(null);
        if (dc == null) {
            throw new SecurityException("Acesso negado: requer magistrado ou delegação DCP.");
        }
        if (dc.delegateId() == null || !dc.delegateId().equals(u.getId())) {
            throw new SecurityException("Acesso negado: delegação não pertence ao usuário autenticado.");
        }
        if (dc.scope() == null || !dc.scope().canDraft()) {
            throw new SecurityException("Acesso negado: delegação sem escopo para minutas.");
        }
        if (dc.validUntil() != null && Instant.now().isAfter(dc.validUntil())) {
            throw new SecurityException("Acesso negado: delegação expirada.");
        }
    }

    
    public void requireFinalActAccess() {
        Usuario u = currentUserService.getRequired();
        if (!u.isMagistrado()) {
            throw new SecurityException("Acesso negado: ato final exige magistrado titular.");
        }
    }

    
    @Deprecated(forRemoval = true)
    public void requirePublishAccess() {
        requireFinalActAccess();
    }
}
