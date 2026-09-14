package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.application.InstitutionalCanonicalCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.application.InstitutionalIntegrationContractCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalNoticeChannelApplicationService;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalCanonicalCatalogEntryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelBlueprintResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationContractDescriptorResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalNoticeChannelResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6, sub-passo do WorkflowOperations original) para separar as consultas
 * de catálogo/descritor estáticos (leitura pura de metadados governança) das operações
 * de runtime sobre expedições. Cada método aqui é uma leitura idempotente sem estado.
 */
@Service
public class NationalCommunicationInstitutionalCatalogDescriptorsSurfaceService {

    private final InstitutionalCanonicalCatalogApplicationService canonicalCatalogService;
    private final InstitutionalIntegrationContractCatalogApplicationService contractCatalogService;
    private final InstitutionalNoticeChannelApplicationService noticeChannelService;
    private final InstitutionalPanelBlueprintApplicationService panelBlueprintApplicationService;
    private final NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport;

    public NationalCommunicationInstitutionalCatalogDescriptorsSurfaceService(
            InstitutionalCanonicalCatalogApplicationService canonicalCatalogService,
            InstitutionalIntegrationContractCatalogApplicationService contractCatalogService,
            InstitutionalNoticeChannelApplicationService noticeChannelService,
            InstitutionalPanelBlueprintApplicationService panelBlueprintApplicationService,
            NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport) {
        this.canonicalCatalogService = canonicalCatalogService;
        this.contractCatalogService = contractCatalogService;
        this.noticeChannelService = noticeChannelService;
        this.panelBlueprintApplicationService = panelBlueprintApplicationService;
        this.surfaceAssemblerSupport = surfaceAssemblerSupport;
    }

    public List<NationalCommunicationInstitutionalCanonicalCatalogEntryResponse> catalogoCanonico() {
        return canonicalCatalogService.list().stream().map(surfaceAssemblerSupport::toCanonicalCatalogEntry).toList();
    }

    public List<NationalCommunicationInstitutionalIntegrationContractDescriptorResponse> contratoIntegracao() {
        return contractCatalogService.list().stream().map(surfaceAssemblerSupport::toIntegrationContract).toList();
    }

    public List<NationalCommunicationInstitutionalNoticeChannelResponse> avisosExternos() {
        return noticeChannelService.list().stream().map(surfaceAssemblerSupport::toNoticeChannel).toList();
    }

    public List<NationalCommunicationInstitutionalPanelBlueprintResponse> painelBlueprints(String scope, String panel) {
        return panelBlueprintApplicationService.listar(scope, panel).stream().map(surfaceAssemblerSupport::toResponse).toList();
    }
}
