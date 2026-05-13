package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedDocumentBatchLinkView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedDocumentBatchPreviewView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedDocumentBatchLinkRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeGovernedDocumentFilingService {

    private final CurrentUserService currentUserService;
    private final OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService;
    private final OfficeDocumentBatchGovernanceService officeDocumentBatchGovernanceService;
    private final OfficeGovernedProcessOperationService officeGovernedProcessOperationService;

    public OfficeGovernedDocumentFilingService(CurrentUserService currentUserService,
                                               OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService,
                                               OfficeDocumentBatchGovernanceService officeDocumentBatchGovernanceService,
                                               OfficeGovernedProcessOperationService officeGovernedProcessOperationService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.officeProcessWorkspaceScopeService = Objects.requireNonNull(officeProcessWorkspaceScopeService);
        this.officeDocumentBatchGovernanceService = Objects.requireNonNull(officeDocumentBatchGovernanceService);
        this.officeGovernedProcessOperationService = Objects.requireNonNull(officeGovernedProcessOperationService);
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeGovernedDocumentBatchPreviewView preview(Long processoId, UUID batchId, HttpServletRequest request) {
        Usuario actor = currentUserService.getRequired();
        PjbFrontendOfficeProcessAccessView access = officeProcessWorkspaceScopeService.access(processoId, OfficeActionType.JUNTAR_DOCUMENTO, request);
        OfficeDocumentBatchGovernanceService.DocumentBatchSnapshot snapshot = officeDocumentBatchGovernanceService.snapshot(batchId);
        ArrayList<String> blockers = new ArrayList<>(safeList(access.blockers()));
        ArrayList<String> warnings = new ArrayList<>(safeList(access.warnings()));
        if (!Objects.equals(snapshot.processoId(), processoId)) {
            blockers.add("BATCH_PROCESS_MISMATCH");
        }
        if (snapshot.createdByUserId() != null && !Objects.equals(snapshot.createdByUserId(), actor.getId())) {
            blockers.add("BATCH_NOT_OWNED_BY_ACTOR");
        }
        if (!"INITIATED".equals(snapshot.status())) {
            blockers.add("BATCH_NOT_INITIATED");
        }
        if (snapshot.uploadedCount() <= 0) {
            blockers.add("BATCH_WITHOUT_UPLOADED_ITEMS");
        }
        if (snapshot.reservedCount() > 0) {
            warnings.add("BATCH_HAS_RESERVED_ITEMS");
        }
        if (snapshot.failedCount() > 0) {
            warnings.add("BATCH_HAS_FAILED_ITEMS");
        }
        if (snapshot.expectedCount() != null && snapshot.expectedCount() > snapshot.uploadedCount()) {
            warnings.add("BATCH_EXPECTED_COUNT_NOT_REACHED");
        }
        boolean allowed = access.allowed() && blockers.isEmpty();
        return new PjbFrontendOfficeGovernedDocumentBatchPreviewView(
                processoId,
                batchId,
                snapshot.status(),
                snapshot.expectedCount(),
                snapshot.itemCount(),
                snapshot.uploadedCount(),
                snapshot.reservedCount(),
                snapshot.linkedCount(),
                snapshot.failedCount(),
                snapshot.totalBytes(),
                snapshot.fingerprint(),
                access.mode(),
                access.activeEquipeId(),
                access.allowed(),
                access.queueRequired(),
                access.effectiveSignerUserId(),
                access.effectiveSignerNome(),
                allowed,
                blockers,
                warnings
        );
    }

    @Transactional
    public PjbFrontendOfficeGovernedDocumentBatchLinkView linkBatch(Long processoId,
                                                                    FrontendOfficeGovernedDocumentBatchLinkRequest request,
                                                                    HttpServletRequest httpServletRequest) {
        PjbFrontendOfficeGovernedDocumentBatchPreviewView preview = preview(processoId, request.batchId(), httpServletRequest);
        if (!preview.allowed()) {
            throw new IllegalStateException("Lote de documentos fora do escopo governado: " + String.join(", ", preview.blockers()));
        }
        Map<String, Object> result = officeGovernedProcessOperationService.juntarDocumentosPorBatch(
                processoId,
                request.batchId(),
                request.titulo(),
                request.categoria(),
                request.nivelSigilo(),
                request.origemSistema(),
                preview.batchFingerprint(),
                Math.toIntExact(preview.uploadedCount())
        );
        return new PjbFrontendOfficeGovernedDocumentBatchLinkView(
                processoId,
                request.batchId(),
                stringValue(result.get("status")),
                longValue(result.get("operationId")),
                longValue(result.get("queueItemId")),
                longValue(result.get("signerUserId")),
                stringValue(result.get("delegationMode")),
                integerValue(result.get("linkedCount")),
                uuidList(result.get("linkedDocumentIds")),
                stringValue(result.get("batchFingerprint")),
                preview.queueRequired(),
                preview.effectiveSignerUserId(),
                preview.effectiveSignerNome(),
                preview.warnings(),
                safeStringList(result.get("warnings"))
        );
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private List<String> safeStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(Object::toString).toList();
    }

    private List<UUID> uuidList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(item -> item instanceof UUID uuid ? uuid : UUID.fromString(item.toString())).toList();
    }
}
