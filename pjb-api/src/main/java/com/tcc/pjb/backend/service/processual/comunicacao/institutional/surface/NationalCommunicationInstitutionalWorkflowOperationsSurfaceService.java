package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalInboxBatchApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalNoReadCertificationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalOperationalCoverageApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalPanelApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalSlaPredictiveApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalTriageSuggestionApplicationService;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalDelegationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalActionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageApplyRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalSlaPredictiveDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalBulkRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalBulkResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalNoReadRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalInboxItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelSummaryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalTriageSuggestionDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalUnitQueueResponse;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de NationalCommunicationInstitutionalSurfaceFacadeService: catálogo canônico,
 * SLA preditivo, lotes/triagem de expedição, contratos de integração, coberturas operacionais,
 * painéis de órgão/unidade, avisos externos e não-leitura -- 10 colaboradores independentes do
 * mesmo domínio operacional (workflow.application), sem lógica entrelaçada entre si.
 */
@Service
public class NationalCommunicationInstitutionalWorkflowOperationsSurfaceService {

    private final InstitutionalSlaPredictiveApplicationService predictiveService;
    private final InstitutionalInboxBatchApplicationService batchService;
    private final InstitutionalTriageSuggestionApplicationService triageSuggestionService;
    private final InstitutionalOperationalCoverageApplicationService coverageService;
    private final InstitutionalPanelApplicationService panelService;
    private final InstitutionalNoReadCertificationApplicationService noReadService;
    private final NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport;

    public NationalCommunicationInstitutionalWorkflowOperationsSurfaceService(
            InstitutionalSlaPredictiveApplicationService predictiveService,
            InstitutionalInboxBatchApplicationService batchService,
            InstitutionalTriageSuggestionApplicationService triageSuggestionService,
            InstitutionalOperationalCoverageApplicationService coverageService,
            InstitutionalPanelApplicationService panelService,
            InstitutionalNoReadCertificationApplicationService noReadService,
            NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport) {
        this.predictiveService = predictiveService;
        this.batchService = batchService;
        this.triageSuggestionService = triageSuggestionService;
        this.coverageService = coverageService;
        this.panelService = panelService;
        this.noReadService = noReadService;
        this.surfaceAssemblerSupport = surfaceAssemblerSupport;
    }

    public NationalCommunicationInstitutionalSlaPredictiveDashboardResponse slaPreditivo(String uf, DestinatarioInstitucionalKind destinatarioKind) {
        return surfaceAssemblerSupport.toSlaDashboard(predictiveService.dashboard(uf, destinatarioKind));
    }

    public NationalCommunicationInstitutionalBulkResponse receberLote(NationalCommunicationInstitutionalBulkRequest request) {
        return surfaceAssemblerSupport.toBulkResponse(batchService.receberLote(request.expedicoesUuids(), request.detalhe()));
    }

    public NationalCommunicationInstitutionalBulkResponse certificarCienciaLote(NationalCommunicationInstitutionalBulkRequest request) {
        return surfaceAssemblerSupport.toBulkResponse(batchService.certificarCienciaLote(request.expedicoesUuids(), request.detalhe()));
    }

    public NationalCommunicationInstitutionalTriageSuggestionDashboardResponse triagemSugerida(String expedicaoUuid) {
        return surfaceAssemblerSupport.toTriageDashboard(triageSuggestionService.suggest(expedicaoUuid));
    }

    public List<NationalCommunicationInstitutionalCoverageResponse> coberturasOperacionais(String unidadeCodigo) {
        return coverageService.listar(unidadeCodigo).stream().map(surfaceAssemblerSupport::toCoverage).toList();
    }

    public List<NationalCommunicationInstitutionalDelegationResponse> aplicarCoberturasAtivas(NationalCommunicationInstitutionalCoverageApplyRequest request) {
        return coverageService.aplicarAtivas(request.expedicaoUuid(), request.motivoComplementar()).stream().map(surfaceAssemblerSupport::toDelegation).toList();
    }

    public List<NationalCommunicationInstitutionalPanelSummaryResponse> painelOrgao(String unidadeCodigo) {
        return panelService.painelOrgao(unidadeCodigo).stream().map(surfaceAssemblerSupport::toResponse).toList();
    }

    public List<NationalCommunicationInstitutionalUnitQueueResponse> filasUnidade(String unidadeCodigo) {
        return panelService.filasUnidade(unidadeCodigo).stream().map(surfaceAssemblerSupport::toResponse).toList();
    }

    public List<NationalCommunicationInstitutionalInboxItemResponse> pendentesNaoLeitura(String unidadeCodigo) {
        return noReadService.pendentesDecurso(unidadeCodigo).stream().map(surfaceAssemblerSupport::toInbox).toList();
    }

    public NationalCommunicationInstitutionalActionResponse certificarNaoLeitura(NationalCommunicationInstitutionalNoReadRequest request) {
        return surfaceAssemblerSupport.toActionResponse(noReadService.certificarNaoLeitura(request.expedicaoUuid(), request.detalhe()));
    }
}
