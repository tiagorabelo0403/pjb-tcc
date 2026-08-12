package com.tcc.pjb.backend.service.secretariat.institucional;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TomarCienciaService {

    private final SecretariaInstitucionalItemRepository repository;
    private final AuditLedgerService auditService;

    public TomarCienciaService(SecretariaInstitucionalItemRepository repository, AuditLedgerService auditService) {
        this.repository = Objects.requireNonNull(repository);
        this.auditService = Objects.requireNonNull(auditService);
    }

    @Transactional
    public void tomarCiencia(Long itemId) {
        SecretariaInstitucionalItem item = repository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item não encontrado: " + itemId));
        if (item.getIntimadoEm() == null) {
            item.setIntimadoEm(Instant.now());
            item.setStatus(StatusSecretariaInstitucionalItem.EM_ANALISE);
            repository.save(item);
            auditService.appendSafely("SECRETARIA_INSTITUCIONAL_CIENCIA", "SECRETARIA_INSTITUCIONAL_ITEM " + itemId);
        }
    }
}
