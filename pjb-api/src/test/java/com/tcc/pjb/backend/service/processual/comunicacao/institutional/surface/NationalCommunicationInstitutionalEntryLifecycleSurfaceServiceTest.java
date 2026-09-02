package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalEntryGuardApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalOperationalClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalOperationalLifecycleApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalStructuralDiagnosticApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalEntryGuardSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalFourLevelAccessSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalOperationalLifecycle;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalStructuralDiagnosticReport;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalFourLevelAccessResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryGuardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalLifecycleResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalStructuralDiagnosticResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NationalCommunicationInstitutionalEntryLifecycleSurfaceServiceTest {

    private final InstitutionalEntryContextApplicationService entryContextApplicationService = mock(InstitutionalEntryContextApplicationService.class);
    private final InstitutionalOperationalLifecycleApplicationService lifecycleApplicationService = mock(InstitutionalOperationalLifecycleApplicationService.class);
    private final InstitutionalEntryGuardApplicationService entryGuardApplicationService = mock(InstitutionalEntryGuardApplicationService.class);
    private final InstitutionalOperationalClosureApplicationService operationalClosureApplicationService = mock(InstitutionalOperationalClosureApplicationService.class);
    private final InstitutionalStructuralDiagnosticApplicationService structuralDiagnosticApplicationService = mock(InstitutionalStructuralDiagnosticApplicationService.class);
    private final NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport = mock(NationalCommunicationInstitutionalSurfaceAssemblerSupport.class);
    private final NationalCommunicationInstitutionalEntryLifecycleSurfaceService service = new NationalCommunicationInstitutionalEntryLifecycleSurfaceService(
            entryContextApplicationService, lifecycleApplicationService, entryGuardApplicationService,
            operationalClosureApplicationService, structuralDiagnosticApplicationService, surfaceAssemblerSupport);

    @Test
    void detalharAfiliacaoLifecycleDelegaEMapeiaQuandoPresente() {
        var domain = mock(InstitutionalOperationalLifecycle.class);
        var response = mock(NationalCommunicationInstitutionalOperationalLifecycleResponse.class);
        when(lifecycleApplicationService.detalharAfiliacao("aff-1")).thenReturn(Optional.of(domain));
        when(surfaceAssemblerSupport.toLifecycle(domain)).thenReturn(response);

        assertThat(service.detalharAfiliacaoLifecycle("aff-1")).contains(response);
    }

    @Test
    void detalharAfiliacaoLifecycleRetornaVazioQuandoAusente() {
        when(lifecycleApplicationService.detalharAfiliacao("aff-2")).thenReturn(Optional.empty());

        assertThat(service.detalharAfiliacaoLifecycle("aff-2")).isEmpty();
    }

    @Test
    void detalharSolicitacaoLifecycleDelegaEMapeiaQuandoPresente() {
        var domain = mock(InstitutionalOperationalLifecycle.class);
        var response = mock(NationalCommunicationInstitutionalOperationalLifecycleResponse.class);
        when(lifecycleApplicationService.detalharSolicitacao("req-1")).thenReturn(Optional.of(domain));
        when(surfaceAssemblerSupport.toLifecycle(domain)).thenReturn(response);

        assertThat(service.detalharSolicitacaoLifecycle("req-1")).contains(response);
    }

    @Test
    void guardiaoEntradaDelegaEMapeia() {
        var domain = mock(InstitutionalEntryGuardSummary.class);
        var response = mock(NationalCommunicationInstitutionalEntryGuardResponse.class);
        when(entryGuardApplicationService.avaliarEntradaAtual()).thenReturn(domain);
        when(surfaceAssemblerSupport.toGuard(domain)).thenReturn(response);

        assertThat(service.guardiaoEntrada()).isSameAs(response);
    }

    @Test
    void quatroNiveisDelegaEMapeia() {
        var domain = mock(InstitutionalFourLevelAccessSummary.class);
        var response = mock(NationalCommunicationInstitutionalFourLevelAccessResponse.class);
        when(operationalClosureApplicationService.resolverQuatroNiveisAtual("aff-3")).thenReturn(domain);
        when(surfaceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.quatroNiveis("aff-3")).isSameAs(response);
    }

    @Test
    void diagnosticoEstruturalDelegaEMapeia() {
        var domain = mock(InstitutionalStructuralDiagnosticReport.class);
        var response = mock(NationalCommunicationInstitutionalStructuralDiagnosticResponse.class);
        when(structuralDiagnosticApplicationService.diagnosticar("aff-4")).thenReturn(domain);
        when(surfaceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.diagnosticoEstrutural("aff-4")).isSameAs(response);
    }
}
