package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.application.InstitutionalProceduralCoherenceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.application.InstitutionalProcessWorkspaceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.topology.application.InstitutionalRecipientTopologyApplicationService;
import com.tcc.pjb.backend.model.dto.processual.NationalCommunicationInstitutionalTopologyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.InstitutionalProceduralCoherenceReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProcessDiagnosticReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProceduralActEvaluationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProceduralCoherenceAggregateResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalProcessWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalProcessWorkspaceSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de NationalCommunicationInstitutionalSurfaceFacadeService: topologia de
 * destinatários, workspace processual e coerência procedimental -- 3 colaboradores
 * independentes, cada um usado só pelos próprios métodos, sem interseção com os outros grupos.
 */
@Service
public class NationalCommunicationInstitutionalProcessWorkspaceSurfaceService {

    private final InstitutionalRecipientTopologyApplicationService topologyApplicationService;
    private final InstitutionalProcessWorkspaceApplicationService processWorkspaceApplicationService;
    private final InstitutionalProceduralCoherenceApplicationService proceduralCoherenceApplicationService;
    private final NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport;

    public NationalCommunicationInstitutionalProcessWorkspaceSurfaceService(
            InstitutionalRecipientTopologyApplicationService topologyApplicationService,
            InstitutionalProcessWorkspaceApplicationService processWorkspaceApplicationService,
            InstitutionalProceduralCoherenceApplicationService proceduralCoherenceApplicationService,
            NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport) {
        this.topologyApplicationService = topologyApplicationService;
        this.processWorkspaceApplicationService = processWorkspaceApplicationService;
        this.proceduralCoherenceApplicationService = proceduralCoherenceApplicationService;
        this.surfaceAssemblerSupport = surfaceAssemblerSupport;
    }

    public List<NationalCommunicationInstitutionalTopologyResponse> topologiaDestinatarios() {
        return topologyApplicationService.list().stream().map(surfaceAssemblerSupport::toTopology).toList();
    }

    public List<NationalCommunicationInstitutionalProcessWorkspaceSummaryResponse> listarWorkspaces(Long processoId, String rito, String fase, String status, String ramo) {
        return processWorkspaceApplicationService.listarPerfis(processoId, rito, fase, status, ramo).stream().map(surfaceAssemblerSupport::toSummary).toList();
    }

    public NationalCommunicationInstitutionalProcessWorkspaceResponse detalharWorkspace(String profileCode, Long processoId, String rito, String fase, String status, String ramo) {
        return surfaceAssemblerSupport.toResponse(processWorkspaceApplicationService.detalharPerfil(profileCode, processoId, rito, fase, status, ramo));
    }

    public NationalCommunicationInstitutionalProcessDiagnosticReportResponse diagnosticarWorkspace(Long processoId, String rito, String fase, String status, String ramo) {
        return surfaceAssemblerSupport.toDiagnostic(processWorkspaceApplicationService.diagnosticar(processoId, rito, fase, status, ramo));
    }

    public InstitutionalProceduralCoherenceReportResponse diagnosticarCoerencia(Long processoId, String rito, String fase, String status, String ramo) {
        return surfaceAssemblerSupport.toDiagnostic(proceduralCoherenceApplicationService.diagnosticar(processoId, rito, fase, status, ramo));
    }

    public NationalCommunicationInstitutionalProceduralCoherenceAggregateResponse detalharCoerencia(String profileCode, Long processoId, String rito, String fase, String status, String ramo) {
        return surfaceAssemblerSupport.toAggregate(proceduralCoherenceApplicationService.detalhar(profileCode, processoId, rito, fase, status, ramo));
    }

    public NationalCommunicationInstitutionalProceduralActEvaluationResponse avaliarAtoCoerencia(String profileCode, String actionCode, Long processoId, String rito, String fase, String status, String ramo) {
        return surfaceAssemblerSupport.toActEvaluation(proceduralCoherenceApplicationService.avaliarAto(profileCode, actionCode, processoId, rito, fase, status, ramo));
    }
}
