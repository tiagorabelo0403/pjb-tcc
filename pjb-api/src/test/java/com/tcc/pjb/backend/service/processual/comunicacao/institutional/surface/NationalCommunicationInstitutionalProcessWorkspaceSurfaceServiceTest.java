package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.application.InstitutionalProceduralCoherenceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralActEvaluation;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralCoherenceAggregate;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralCoherenceDiagnosticReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.application.InstitutionalProcessWorkspaceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessDiagnosticReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.core.comunicacao.institucional.topology.application.InstitutionalRecipientTopologyApplicationService;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.InstitutionalProceduralCoherenceReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProceduralActEvaluationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProceduralCoherenceAggregateResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProcessDiagnosticReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalProcessWorkspaceResponse;
import org.junit.jupiter.api.Test;

class NationalCommunicationInstitutionalProcessWorkspaceSurfaceServiceTest {

    private final InstitutionalRecipientTopologyApplicationService topologyApplicationService = mock(InstitutionalRecipientTopologyApplicationService.class);
    private final InstitutionalProcessWorkspaceApplicationService processWorkspaceApplicationService = mock(InstitutionalProcessWorkspaceApplicationService.class);
    private final InstitutionalProceduralCoherenceApplicationService proceduralCoherenceApplicationService = mock(InstitutionalProceduralCoherenceApplicationService.class);
    private final NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport = mock(NationalCommunicationInstitutionalSurfaceAssemblerSupport.class);
    private final NationalCommunicationInstitutionalProcessWorkspaceSurfaceService service = new NationalCommunicationInstitutionalProcessWorkspaceSurfaceService(
            topologyApplicationService, processWorkspaceApplicationService, proceduralCoherenceApplicationService, surfaceAssemblerSupport);

    @Test
    void detalharWorkspaceDelegaComOsMesmos5FiltrosEMapeia() {
        var domain = mock(InstitutionalProcessWorkspace.class);
        var response = mock(NationalCommunicationInstitutionalProcessWorkspaceResponse.class);
        when(processWorkspaceApplicationService.detalharPerfil("perfil-1", 10L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).thenReturn(domain);
        when(surfaceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.detalharWorkspace("perfil-1", 10L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).isSameAs(response);
    }

    @Test
    void diagnosticarWorkspaceDelegaEMapeia() {
        var domain = mock(InstitutionalProcessDiagnosticReport.class);
        var response = mock(NationalCommunicationInstitutionalProcessDiagnosticReportResponse.class);
        when(processWorkspaceApplicationService.diagnosticar(10L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).thenReturn(domain);
        when(surfaceAssemblerSupport.toDiagnostic(domain)).thenReturn(response);

        assertThat(service.diagnosticarWorkspace(10L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).isSameAs(response);
    }

    @Test
    void diagnosticarCoerenciaDelegaEMapeia() {
        var domain = mock(InstitutionalProceduralCoherenceDiagnosticReport.class);
        var response = mock(InstitutionalProceduralCoherenceReportResponse.class);
        when(proceduralCoherenceApplicationService.diagnosticar(10L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).thenReturn(domain);
        when(surfaceAssemblerSupport.toDiagnostic(domain)).thenReturn(response);

        assertThat(service.diagnosticarCoerencia(10L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).isSameAs(response);
    }

    @Test
    void detalharCoerenciaDelegaEMapeia() {
        var domain = mock(InstitutionalProceduralCoherenceAggregate.class);
        var response = mock(NationalCommunicationInstitutionalProceduralCoherenceAggregateResponse.class);
        when(proceduralCoherenceApplicationService.detalhar("perfil-2", 10L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).thenReturn(domain);
        when(surfaceAssemblerSupport.toAggregate(domain)).thenReturn(response);

        assertThat(service.detalharCoerencia("perfil-2", 10L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).isSameAs(response);
    }

    @Test
    void avaliarAtoCoerenciaDelegaComOsMesmos7ArgumentosEMapeia() {
        var domain = mock(InstitutionalProceduralActEvaluation.class);
        var response = mock(NationalCommunicationInstitutionalProceduralActEvaluationResponse.class);
        when(proceduralCoherenceApplicationService.avaliarAto("perfil-3", "PETICIONAR", 10L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).thenReturn(domain);
        when(surfaceAssemblerSupport.toActEvaluation(domain)).thenReturn(response);

        assertThat(service.avaliarAtoCoerencia("perfil-3", "PETICIONAR", 10L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).isSameAs(response);
    }
}
