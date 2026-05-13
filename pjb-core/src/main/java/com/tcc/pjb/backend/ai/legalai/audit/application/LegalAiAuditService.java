package com.tcc.pjb.backend.ai.legalai.audit.application;

import com.tcc.pjb.backend.ai.legalai.audit.domain.LegalAiAuditLog;
import com.tcc.pjb.backend.ai.legalai.audit.domain.LegalAiAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegalAiAuditService {

    private final LegalAiAuditRepository repository;

    public LegalAiAuditService(LegalAiAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(LegalAiAuditLog log) {
        repository.salvar(log);
    }
}
