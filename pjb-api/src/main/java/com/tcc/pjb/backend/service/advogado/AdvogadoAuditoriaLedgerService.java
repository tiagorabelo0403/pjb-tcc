package com.tcc.pjb.backend.service.advogado;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerRepository;
import com.tcc.pjb.backend.model.dto.advogado.AdvogadoAuditDto;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trilha de auditoria de um advogado. A consulta é sempre escopada ao próprio solicitante: o
 * identificador do ator não vem de parâmetro da requisição, e sim de quem está autenticado, para que
 * um advogado não alcance a trilha de outro trocando um id na URL.
 */
@Service
public class AdvogadoAuditoriaLedgerService {

    private final AuditLedgerRepository auditLedgerRepository;

    public AdvogadoAuditoriaLedgerService(AuditLedgerRepository auditLedgerRepository) {
        this.auditLedgerRepository = Objects.requireNonNull(auditLedgerRepository);
    }

    @Transactional(readOnly = true)
    public Page<AdvogadoAuditDto.LedgerEventResponse> doAdvogado(Long advogadoId,
                                                                 String actionPrefix,
                                                                 String resourceType,
                                                                 String resourceId,
                                                                 Pageable pageable) {
        Objects.requireNonNull(advogadoId, "advogadoId");
        return auditLedgerRepository
                .search(advogadoId, normalizar(actionPrefix), normalizar(resourceType), normalizar(resourceId),
                        pageable)
                .map(AdvogadoAuditoriaLedgerService::toResponse);
    }

    /** Filtro em branco é filtro ausente: string vazia no SQL não filtraria nada e não é intenção. */
    private static String normalizar(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }

    private static AdvogadoAuditDto.LedgerEventResponse toResponse(
            com.tcc.pjb.backend.core.audit.ledger.AuditLedgerEntry entrada) {
        return new AdvogadoAuditDto.LedgerEventResponse(
                entrada.getId(),
                entrada.getCreatedAt() != null ? entrada.getCreatedAt().toString() : null,
                entrada.getAction(),
                entrada.getResourceType(),
                entrada.getResourceId(),
                entrada.getRequestId(),
                entrada.getPayloadHash(),
                entrada.getEntryHash());
    }
}
