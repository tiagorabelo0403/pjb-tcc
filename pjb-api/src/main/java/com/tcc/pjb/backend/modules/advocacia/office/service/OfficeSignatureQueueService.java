package com.tcc.pjb.backend.modules.advocacia.office.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.office.dto.OfficeQueueItemDto;
import com.tcc.pjb.backend.modules.advocacia.office.entity.OfficeSignatureQueueItem;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeQueueStatus;
import com.tcc.pjb.backend.modules.advocacia.office.repository.OfficeSignatureQueueRepository;

@Service
public class OfficeSignatureQueueService {

    public record BulkResult(List<Long> approved, List<Long> rejected, List<String> errors) {
    }

    private final OfficeSignatureQueueRepository queueRepo;
    private final UsuarioRepository usuarioRepo;
    private final OfficeAuthorizationService authz;
    private final OfficeQueueExecutorRegistry executorRegistry;
    private final AuditLedgerService auditLedgerService;

    public OfficeSignatureQueueService(OfficeSignatureQueueRepository queueRepo,
                                       UsuarioRepository usuarioRepo,
                                       OfficeAuthorizationService authz,
                                       OfficeQueueExecutorRegistry executorRegistry,
                                       AuditLedgerService auditLedgerService) {
        this.queueRepo = Objects.requireNonNull(queueRepo);
        this.usuarioRepo = Objects.requireNonNull(usuarioRepo);
        this.authz = Objects.requireNonNull(authz);
        this.executorRegistry = Objects.requireNonNull(executorRegistry);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public Page<OfficeQueueItemDto> listarPorSigner(Long currentUserId, OfficeQueueStatus status, Pageable pageable) {
        Objects.requireNonNull(currentUserId);
        OfficeQueueStatus st = status == null ? OfficeQueueStatus.PENDING : status;
        Page<OfficeSignatureQueueItem> page = queueRepo.findBySignerAndStatus(currentUserId, st, pageable);
        return page.map(this::toDto);
    }

    @Transactional
    public OfficeQueueItemDto aprovar(Long currentUserId, Long queueItemId, String reason) {
        Objects.requireNonNull(currentUserId);
        Objects.requireNonNull(queueItemId);

        OfficeSignatureQueueItem q = queueRepo.findByIdWithGraph(queueItemId)
                .orElseThrow(() -> new EntityNotFoundException("Fila não encontrada."));

        authz.requireSigner(currentUserId, q.getSigner() != null ? q.getSigner().getId() : null);

        if (q.getStatus() != OfficeQueueStatus.PENDING) {
            throw new IllegalStateException("Fila não está pendente.");
        }

        Usuario decidedBy = usuarioRepo.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        q.setStatus(OfficeQueueStatus.APPROVED);
        q.setDecidedAt(LocalDateTime.now());
        q.setDecidedBy(decidedBy);
        q.setDecisionReason(truncate(reason));
        queueRepo.save(q);

        executorRegistry.dispatchApproved(q, currentUserId, truncate(reason));
        auditLedgerService.appendSafely("ADV_OFFICE_QUEUE_APPROVED", q.getResourceType(), q.getResourceId(), q.getPayloadHash(), truncate(reason));

        return toDto(q);
    }

    @Transactional
    public OfficeQueueItemDto rejeitar(Long currentUserId, Long queueItemId, String reason) {
        Objects.requireNonNull(currentUserId);
        Objects.requireNonNull(queueItemId);

        OfficeSignatureQueueItem q = queueRepo.findByIdWithGraph(queueItemId)
                .orElseThrow(() -> new EntityNotFoundException("Fila não encontrada."));

        authz.requireSigner(currentUserId, q.getSigner() != null ? q.getSigner().getId() : null);

        if (q.getStatus() != OfficeQueueStatus.PENDING) {
            throw new IllegalStateException("Fila não está pendente.");
        }

        Usuario decidedBy = usuarioRepo.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        q.setStatus(OfficeQueueStatus.REJECTED);
        q.setDecidedAt(LocalDateTime.now());
        q.setDecidedBy(decidedBy);
        q.setDecisionReason(truncate(reason));
        queueRepo.save(q);

        executorRegistry.dispatchRejected(q, currentUserId, truncate(reason));
        auditLedgerService.appendSafely("ADV_OFFICE_QUEUE_REJECTED", q.getResourceType(), q.getResourceId(), q.getPayloadHash(), truncate(reason));

        return toDto(q);
    }

    @Transactional
    public BulkResult bulkApprove(Long currentUserId, List<Long> ids, String reason) {
        Objects.requireNonNull(currentUserId);
        List<Long> approved = new ArrayList<>();
        List<Long> rejected = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (ids == null || ids.isEmpty()) {
            return new BulkResult(approved, rejected, errors);
        }

        String r = truncate(reason);

        for (Long id : ids) {
            try {
                aprovar(currentUserId, id, r);
                approved.add(id);
            } catch (Exception e) {
                rejected.add(id);
                errors.add("id=" + id + " err=" + safeMsg(e));
            }
        }

        return new BulkResult(approved, rejected, errors);
    }

    private OfficeQueueItemDto toDto(OfficeSignatureQueueItem q) {
        return OfficeQueueItemDto.builder()
                .id(q.getId())
                .equipeId(q.getEquipe() != null ? q.getEquipe().getId() : null)
                .executorUserId(q.getExecutor() != null ? q.getExecutor().getId() : null)
                .signerUserId(q.getSigner() != null ? q.getSigner().getId() : null)
                .actionType(q.getActionType())
                .resourceType(q.getResourceType())
                .resourceId(q.getResourceId())
                .status(q.getStatus())
                .createdAt(q.getCreatedAt())
                .decidedAt(q.getDecidedAt())
                .decidedByUserId(q.getDecidedBy() != null ? q.getDecidedBy().getId() : null)
                .decisionReason(q.getDecisionReason())
                .requestId(q.getRequestId())
                .payloadHash(q.getPayloadHash())
                .summary(q.getSummary())
                .build();
    }

    private String truncate(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return t.length() > 240 ? t.substring(0, 240) : t;
    }

    private String safeMsg(Exception e) {
        if (e == null) return "";
        String m = e.getMessage();
        if (m == null) return e.getClass().getSimpleName();
        m = m.trim();
        return m.isEmpty() ? e.getClass().getSimpleName() : m;
    }
}
