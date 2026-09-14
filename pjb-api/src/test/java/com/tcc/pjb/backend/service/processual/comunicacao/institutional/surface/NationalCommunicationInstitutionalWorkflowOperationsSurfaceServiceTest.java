package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalInboxBatchApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalNoReadCertificationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalOperationalCoverageApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalPanelApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalSlaPredictiveApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalTriageSuggestionApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalBulkActionSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalSlaPredictiveDashboard;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalTriageSuggestionDashboard;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalActionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalSlaPredictiveDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalBulkRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalBulkResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalNoReadRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalTriageSuggestionDashboardResponse;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import java.util.List;
import org.junit.jupiter.api.Test;

class NationalCommunicationInstitutionalWorkflowOperationsSurfaceServiceTest {

    private final InstitutionalSlaPredictiveApplicationService predictiveService = mock(InstitutionalSlaPredictiveApplicationService.class);
    private final InstitutionalInboxBatchApplicationService batchService = mock(InstitutionalInboxBatchApplicationService.class);
    private final InstitutionalTriageSuggestionApplicationService triageSuggestionService = mock(InstitutionalTriageSuggestionApplicationService.class);
    private final InstitutionalOperationalCoverageApplicationService coverageService = mock(InstitutionalOperationalCoverageApplicationService.class);
    private final InstitutionalPanelApplicationService panelService = mock(InstitutionalPanelApplicationService.class);
    private final InstitutionalNoReadCertificationApplicationService noReadService = mock(InstitutionalNoReadCertificationApplicationService.class);
    private final NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport = mock(NationalCommunicationInstitutionalSurfaceAssemblerSupport.class);
    private final NationalCommunicationInstitutionalWorkflowOperationsSurfaceService service = new NationalCommunicationInstitutionalWorkflowOperationsSurfaceService(
            predictiveService, batchService, triageSuggestionService,
            coverageService, panelService, noReadService, surfaceAssemblerSupport);

    @Test
    void slaPreditivoDelegaEMapeia() {
        var dashboard = mock(InstitutionalSlaPredictiveDashboard.class);
        var response = mock(NationalCommunicationInstitutionalSlaPredictiveDashboardResponse.class);
        when(predictiveService.dashboard("CE", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO)).thenReturn(dashboard);
        when(surfaceAssemblerSupport.toSlaDashboard(dashboard)).thenReturn(response);

        assertThat(service.slaPreditivo("CE", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO)).isSameAs(response);
    }

    @Test
    void receberLoteDelegaComExpedicoesEDetalheDoRequest() {
        var request = new NationalCommunicationInstitutionalBulkRequest(List.of("uuid-1", "uuid-2"), "detalhe-x");
        var summary = mock(InstitutionalBulkActionSummary.class);
        var response = mock(NationalCommunicationInstitutionalBulkResponse.class);
        when(batchService.receberLote(request.expedicoesUuids(), request.detalhe())).thenReturn(summary);
        when(surfaceAssemblerSupport.toBulkResponse(summary)).thenReturn(response);

        assertThat(service.receberLote(request)).isSameAs(response);
    }

    @Test
    void certificarCienciaLoteDelegaComExpedicoesEDetalheDoRequest() {
        var request = new NationalCommunicationInstitutionalBulkRequest(List.of("uuid-3"), "detalhe-y");
        var summary = mock(InstitutionalBulkActionSummary.class);
        var response = mock(NationalCommunicationInstitutionalBulkResponse.class);
        when(batchService.certificarCienciaLote(request.expedicoesUuids(), request.detalhe())).thenReturn(summary);
        when(surfaceAssemblerSupport.toBulkResponse(summary)).thenReturn(response);

        assertThat(service.certificarCienciaLote(request)).isSameAs(response);
    }

    @Test
    void triagemSugeridaDelegaEMapeia() {
        var dashboard = mock(InstitutionalTriageSuggestionDashboard.class);
        var response = mock(NationalCommunicationInstitutionalTriageSuggestionDashboardResponse.class);
        when(triageSuggestionService.suggest("exp-1")).thenReturn(dashboard);
        when(surfaceAssemblerSupport.toTriageDashboard(dashboard)).thenReturn(response);

        assertThat(service.triagemSugerida("exp-1")).isSameAs(response);
    }

    @Test
    void certificarNaoLeituraDelegaComExpedicaoEDetalheDoRequest() {
        var request = new NationalCommunicationInstitutionalNoReadRequest("exp-9", "motivo-x");
        var item = mock(InstitutionalInboxItem.class);
        var response = mock(NationalCommunicationInstitutionalActionResponse.class);
        when(noReadService.certificarNaoLeitura(request.expedicaoUuid(), request.detalhe())).thenReturn(item);
        when(surfaceAssemblerSupport.toActionResponse(item)).thenReturn(response);

        assertThat(service.certificarNaoLeitura(request)).isSameAs(response);
    }
}
