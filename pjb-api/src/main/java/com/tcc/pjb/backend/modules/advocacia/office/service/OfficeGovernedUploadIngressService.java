package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedUploadBatchView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedUploadFinalizeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedUploadIngressView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedUploadItemReservationView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.jobs.JobCreateResponse;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedUploadBatchCreateRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedUploadFinalizeRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedUploadReserveItemRequest;
import com.tcc.pjb.backend.model.dto.upload.UploadBatchCreateRequest;
import com.tcc.pjb.backend.model.dto.upload.UploadBatchCreateResponse;
import com.tcc.pjb.backend.model.dto.upload.UploadIngressResponse;
import com.tcc.pjb.backend.model.dto.upload.UploadItemReserveRequest;
import com.tcc.pjb.backend.model.dto.upload.UploadItemReserveResponse;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.service.upload.surface.UploadBatchSurfaceFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeGovernedUploadIngressService {

    private final CurrentUserService currentUserService;
    private final OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService;
    private final OfficeDocumentBatchGovernanceService officeDocumentBatchGovernanceService;
    private final UploadBatchSurfaceFacadeService uploadBatchSurfaceFacadeService;

    public OfficeGovernedUploadIngressService(CurrentUserService currentUserService,
                                              OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService,
                                              OfficeDocumentBatchGovernanceService officeDocumentBatchGovernanceService,
                                              UploadBatchSurfaceFacadeService uploadBatchSurfaceFacadeService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.officeProcessWorkspaceScopeService = Objects.requireNonNull(officeProcessWorkspaceScopeService);
        this.officeDocumentBatchGovernanceService = Objects.requireNonNull(officeDocumentBatchGovernanceService);
        this.uploadBatchSurfaceFacadeService = Objects.requireNonNull(uploadBatchSurfaceFacadeService);
    }

    @Transactional
    public PjbFrontendOfficeGovernedUploadBatchView createBatch(Long processoId,
                                                                FrontendOfficeGovernedUploadBatchCreateRequest request,
                                                                HttpServletRequest httpServletRequest) {
        PjbFrontendOfficeProcessAccessView access = officeProcessWorkspaceScopeService.access(processoId, OfficeActionType.JUNTAR_DOCUMENTO, httpServletRequest);
        if (!access.allowed()) {
            throw new IllegalStateException("Upload fora do escopo operacional do workspace: " + String.join(", ", safeList(access.blockers())));
        }
        UploadBatchCreateResponse created = uploadBatchSurfaceFacadeService.createBatch(new UploadBatchCreateRequest(
                processoId,
                request == null ? null : request.expectedCount()
        ));
        return batch(processoId, created.batchId(), httpServletRequest);
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeGovernedUploadBatchView batch(Long processoId, UUID batchId, HttpServletRequest httpServletRequest) {
        PjbFrontendOfficeProcessAccessView access = officeProcessWorkspaceScopeService.access(processoId, OfficeActionType.JUNTAR_DOCUMENTO, httpServletRequest);
        OfficeDocumentBatchGovernanceService.DocumentBatchSnapshot snapshot = officeDocumentBatchGovernanceService.snapshot(batchId);
        ArrayList<String> blockers = new ArrayList<>(safeList(access.blockers()));
        ArrayList<String> warnings = new ArrayList<>(safeList(access.warnings()));
        if (!Objects.equals(snapshot.processoId(), processoId)) {
            blockers.add("BATCH_PROCESS_MISMATCH");
        }
        Long actorId = currentUserService.currentUserIdOrZero();
        if (snapshot.createdByUserId() == null) {
            blockers.add("BATCH_WITHOUT_OWNER");
        } else if (actorId > 0 && !Objects.equals(snapshot.createdByUserId(), actorId)) {
            blockers.add("BATCH_NOT_OWNED_BY_ACTOR");
        }
        if (snapshot.reservedCount() > 0) {
            warnings.add("BATCH_HAS_RESERVED_ITEMS");
        }
        if (snapshot.failedCount() > 0) {
            warnings.add("BATCH_HAS_FAILED_ITEMS");
        }
        boolean allowed = access.allowed() && blockers.isEmpty();
        return new PjbFrontendOfficeGovernedUploadBatchView(
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
                allowed,
                List.copyOf(blockers),
                List.copyOf(warnings)
        );
    }

    @Transactional
    public PjbFrontendOfficeGovernedUploadItemReservationView reserveItem(Long processoId,
                                                                          UUID batchId,
                                                                          FrontendOfficeGovernedUploadReserveItemRequest request,
                                                                          HttpServletRequest httpServletRequest) {
        PjbFrontendOfficeGovernedUploadBatchView batch = batch(processoId, batchId, httpServletRequest);
        if (!batch.allowed()) {
            throw new IllegalStateException("Reserva de item fora do escopo governado: " + String.join(", ", batch.blockers()));
        }
        UploadItemReserveResponse response = uploadBatchSurfaceFacadeService.reserveItem(batchId, new UploadItemReserveRequest(
                request.nomeOriginal(),
                request.contentType(),
                request.tamanhoBytes(),
                request.hashSha384(),
                request.edgeAttestationJson()
        ));
        PjbFrontendOfficeGovernedUploadBatchView refreshed = batch(processoId, batchId, httpServletRequest);
        return new PjbFrontendOfficeGovernedUploadItemReservationView(
                processoId,
                batchId,
                response.itemId(),
                response.uploadUrl(),
                response.status(),
                refreshed.batchFingerprint(),
                refreshed.allowed(),
                refreshed.blockers(),
                refreshed.warnings()
        );
    }

    @Transactional
    public PjbFrontendOfficeGovernedUploadIngressView directUpload(Long processoId,
                                                                   UUID batchId,
                                                                   UUID itemId,
                                                                   String token,
                                                                   HttpServletRequest httpServletRequest) throws Exception {
        PjbFrontendOfficeGovernedUploadBatchView batch = batch(processoId, batchId, httpServletRequest);
        if (!batch.allowed()) {
            throw new IllegalStateException("Upload direto fora do escopo governado: " + String.join(", ", batch.blockers()));
        }
        UploadIngressResponse response = uploadBatchSurfaceFacadeService.directUpload(batchId, itemId, token, httpServletRequest);
        PjbFrontendOfficeGovernedUploadBatchView refreshed = batch(processoId, batchId, httpServletRequest);
        return new PjbFrontendOfficeGovernedUploadIngressView(
                processoId,
                batchId,
                itemId,
                response.status(),
                response.sha256(),
                response.sha384(),
                response.storageUri(),
                refreshed.batchFingerprint(),
                refreshed.allowed(),
                refreshed.blockers(),
                refreshed.warnings()
        );
    }

    @Transactional
    public PjbFrontendOfficeGovernedUploadFinalizeView finalizeBatch(Long processoId,
                                                                     UUID batchId,
                                                                     FrontendOfficeGovernedUploadFinalizeRequest request,
                                                                     HttpServletRequest httpServletRequest) {
        PjbFrontendOfficeGovernedUploadBatchView batch = batch(processoId, batchId, httpServletRequest);
        if (!batch.allowed()) {
            throw new IllegalStateException("Finalizacao do lote fora do escopo governado: " + String.join(", ", batch.blockers()));
        }
        if (request != null && request.expectedFingerprint() != null && !request.expectedFingerprint().isBlank()
                && !Objects.equals(batch.batchFingerprint(), request.expectedFingerprint().trim())) {
            throw new IllegalStateException("Lote de upload alterado desde a revisao do frontend.");
        }
        JobCreateResponse response = uploadBatchSurfaceFacadeService.finalizeBatch(
                batchId,
                request == null ? null : request.idempotencyKey(),
                request == null ? null : request.clientRequestId()
        );
        PjbFrontendOfficeGovernedUploadBatchView refreshed = batch(processoId, batchId, httpServletRequest);
        return new PjbFrontendOfficeGovernedUploadFinalizeView(
                processoId,
                batchId,
                response.jobId(),
                response.status(),
                response.replay(),
                response.inProgress(),
                refreshed.batchFingerprint(),
                refreshed.allowed(),
                refreshed.blockers(),
                refreshed.warnings()
        );
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
