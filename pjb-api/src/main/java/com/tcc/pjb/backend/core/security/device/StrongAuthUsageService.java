package com.tcc.pjb.backend.core.security.device;

import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.StrongAuthUsage;
import com.tcc.pjb.backend.model.repository.security.StrongAuthUsageRepository;

@Service
public class StrongAuthUsageService {

    private final StrongAuthUsageRepository repo;

    public StrongAuthUsageService(StrongAuthUsageRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Transactional
    public void consumeOnce(Long sessionId, Usuario usuario, String actionHash, String requestHash) {
        if (sessionId == null) throw new IllegalArgumentException("sessionId obrigatório");
        if (usuario == null || usuario.getId() == null) throw new IllegalArgumentException("usuario obrigatório");
        if (actionHash == null || actionHash.isBlank()) throw new IllegalArgumentException("actionHash obrigatório");
        if (requestHash == null || requestHash.isBlank()) throw new IllegalArgumentException("requestHash obrigatório");

        String localKey = "strongauth:" + sessionId + ":" + requestHash;
        if (!RequestContext.markOnce(localKey)) {
            throw new IllegalStateException("step-up já consumido nesta requisição");
        }

        StrongAuthUsage u = new StrongAuthUsage();
        u.setUsuario(usuario);
        u.setSessionId(sessionId);
        u.setActionHash(actionHash.trim());
        u.setRequestHash(requestHash.trim());

        try {
            repo.save(u);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("step-up já consumido");
        }
    }
}
