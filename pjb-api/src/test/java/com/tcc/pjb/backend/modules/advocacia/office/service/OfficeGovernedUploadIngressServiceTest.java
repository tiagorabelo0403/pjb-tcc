package com.tcc.pjb.backend.modules.advocacia.office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.jobs.JobCreateResponse;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedUploadBatchCreateRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedUploadFinalizeRequest;
import com.tcc.pjb.backend.model.dto.upload.UploadBatchCreateResponse;
import com.tcc.pjb.backend.service.upload.surface.UploadBatchSurfaceFacadeService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class OfficeGovernedUploadIngressServiceTest {

    @Test
    void createBatch_deveRespeitarEscopoDoWorkspace() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService = mock(OfficeProcessWorkspaceScopeService.class);
        OfficeDocumentBatchGovernanceService officeDocumentBatchGovernanceService = mock(OfficeDocumentBatchGovernanceService.class);
        UploadBatchSurfaceFacadeService uploadBatchSurfaceFacadeService = mock(UploadBatchSurfaceFacadeService.class);
        OfficeGovernedUploadIngressService service = new OfficeGovernedUploadIngressService(
                currentUserService,
                officeProcessWorkspaceScopeService,
                officeDocumentBatchGovernanceService,
                uploadBatchSurfaceFacadeService
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        UUID batchId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        when(officeProcessWorkspaceScopeService.access(1001L, com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType.JUNTAR_DOCUMENTO, request))
                .thenReturn(new PjbFrontendOfficeProcessAccessView(1001L, "0001", 44L, "HYBRID", "JUNTAR_DOCUMENTO", true, true, false, 77L, "Dr. Senior", List.of(), List.of()));
        when(uploadBatchSurfaceFacadeService.createBatch(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new UploadBatchCreateResponse(batchId, "INITIATED"));
        when(currentUserService.currentUserIdOrZero()).thenReturn(10L);
        when(officeDocumentBatchGovernanceService.snapshot(batchId))
                .thenReturn(new OfficeDocumentBatchGovernanceService.DocumentBatchSnapshot(batchId, 1001L, 10L, "INITIATED", 3, 0, 0, 0, 0, 0, 0L, "fp-1"));

        var view = service.createBatch(1001L, new FrontendOfficeGovernedUploadBatchCreateRequest(3), request);

        assertThat(view.batchId()).isEqualTo(batchId);
        assertThat(view.allowed()).isTrue();
        assertThat(view.batchFingerprint()).isEqualTo("fp-1");
    }

    @Test
    void finalizeBatch_deveBloquearFingerprintDivergente() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService = mock(OfficeProcessWorkspaceScopeService.class);
        OfficeDocumentBatchGovernanceService officeDocumentBatchGovernanceService = mock(OfficeDocumentBatchGovernanceService.class);
        UploadBatchSurfaceFacadeService uploadBatchSurfaceFacadeService = mock(UploadBatchSurfaceFacadeService.class);
        OfficeGovernedUploadIngressService service = new OfficeGovernedUploadIngressService(
                currentUserService,
                officeProcessWorkspaceScopeService,
                officeDocumentBatchGovernanceService,
                uploadBatchSurfaceFacadeService
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        UUID batchId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        when(officeProcessWorkspaceScopeService.access(1001L, com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType.JUNTAR_DOCUMENTO, request))
                .thenReturn(new PjbFrontendOfficeProcessAccessView(1001L, "0001", 44L, "HYBRID", "JUNTAR_DOCUMENTO", true, false, false, 10L, "Tiago Silva", List.of(), List.of()));
        when(currentUserService.currentUserIdOrZero()).thenReturn(10L);
        when(officeDocumentBatchGovernanceService.snapshot(batchId))
                .thenReturn(new OfficeDocumentBatchGovernanceService.DocumentBatchSnapshot(batchId, 1001L, 10L, "INITIATED", 3, 1, 1, 0, 0, 0, 2048L, "fp-real"));
        when(uploadBatchSurfaceFacadeService.finalizeBatch(batchId, "idem-1", "client-1"))
                .thenReturn(new JobCreateResponse(UUID.randomUUID(), "PENDING", false, false));

        assertThatThrownBy(() -> service.finalizeBatch(1001L, batchId, new FrontendOfficeGovernedUploadFinalizeRequest("fp-outro", "idem-1", "client-1"), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("alterado desde a revisao do frontend");
    }
}
