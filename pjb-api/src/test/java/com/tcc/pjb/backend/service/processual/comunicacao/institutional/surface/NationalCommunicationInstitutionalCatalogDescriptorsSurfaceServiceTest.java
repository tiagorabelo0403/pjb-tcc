package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.application.InstitutionalCanonicalCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.domain.InstitutionalCanonicalCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.application.InstitutionalIntegrationContractCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalIntegrationContractDescriptor;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelBlueprintSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalNoticeChannelApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalNoticeChannelDescriptor;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalCanonicalCatalogEntryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelBlueprintResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationContractDescriptorResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalNoticeChannelResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class NationalCommunicationInstitutionalCatalogDescriptorsSurfaceServiceTest {

    private final InstitutionalCanonicalCatalogApplicationService canonicalCatalogService = mock(InstitutionalCanonicalCatalogApplicationService.class);
    private final InstitutionalIntegrationContractCatalogApplicationService contractCatalogService = mock(InstitutionalIntegrationContractCatalogApplicationService.class);
    private final InstitutionalNoticeChannelApplicationService noticeChannelService = mock(InstitutionalNoticeChannelApplicationService.class);
    private final InstitutionalPanelBlueprintApplicationService panelBlueprintApplicationService = mock(InstitutionalPanelBlueprintApplicationService.class);
    private final NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport = mock(NationalCommunicationInstitutionalSurfaceAssemblerSupport.class);
    private final NationalCommunicationInstitutionalCatalogDescriptorsSurfaceService service = new NationalCommunicationInstitutionalCatalogDescriptorsSurfaceService(
            canonicalCatalogService, contractCatalogService, noticeChannelService, panelBlueprintApplicationService, surfaceAssemblerSupport);

    @Test
    void catalogoCanonicoListaEMapeiaCadaEntrada() {
        var domain = mock(InstitutionalCanonicalCatalogEntry.class);
        var response = mock(NationalCommunicationInstitutionalCanonicalCatalogEntryResponse.class);
        when(canonicalCatalogService.list()).thenReturn(List.of(domain));
        when(surfaceAssemblerSupport.toCanonicalCatalogEntry(domain)).thenReturn(response);

        assertThat(service.catalogoCanonico()).containsExactly(response);
    }

    @Test
    void contratoIntegracaoListaEMapeiaCadaDescritor() {
        var domain = mock(InstitutionalIntegrationContractDescriptor.class);
        var response = mock(NationalCommunicationInstitutionalIntegrationContractDescriptorResponse.class);
        when(contractCatalogService.list()).thenReturn(List.of(domain));
        when(surfaceAssemblerSupport.toIntegrationContract(domain)).thenReturn(response);

        assertThat(service.contratoIntegracao()).containsExactly(response);
    }

    @Test
    void avisosExternosListaEMapeiaCadaCanal() {
        var domain = mock(InstitutionalNoticeChannelDescriptor.class);
        var response = mock(NationalCommunicationInstitutionalNoticeChannelResponse.class);
        when(noticeChannelService.list()).thenReturn(List.of(domain));
        when(surfaceAssemblerSupport.toNoticeChannel(domain)).thenReturn(response);

        assertThat(service.avisosExternos()).containsExactly(response);
    }

    @Test
    void painelBlueprintsListaEMapeiaComScopeEPanel() {
        var domain = mock(InstitutionalPanelBlueprintSpec.class);
        var response = mock(NationalCommunicationInstitutionalPanelBlueprintResponse.class);
        when(panelBlueprintApplicationService.listar("BR", "PJB")).thenReturn(List.of(domain));
        when(surfaceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.painelBlueprints("BR", "PJB")).containsExactly(response);
    }
}
