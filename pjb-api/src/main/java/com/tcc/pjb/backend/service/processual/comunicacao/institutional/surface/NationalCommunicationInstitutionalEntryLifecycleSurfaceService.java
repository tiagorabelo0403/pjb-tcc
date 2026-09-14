package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalEntryGuardApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalOperationalClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalOperationalLifecycleApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalStructuralDiagnosticApplicationService;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalFourLevelAccessResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryContextResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryGuardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalCaseResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalLifecycleResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalStructuralDiagnosticResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de NationalCommunicationInstitutionalSurfaceFacadeService: contexto de entrada,
 * cadastros e lifecycle operacional, guardião de entrada, 4 níveis de acesso e diagnóstico
 * estrutural -- 4 colaboradores do pacote registry.application usados juntos pelos mesmos
 * chamadores (afiliação/solicitação), mais o contexto de entrada compartilhado com
 * entradaInteligente() no facade principal.
 */
@Service
public class NationalCommunicationInstitutionalEntryLifecycleSurfaceService {

    private final InstitutionalEntryContextApplicationService entryContextApplicationService;
    private final InstitutionalOperationalLifecycleApplicationService lifecycleApplicationService;
    private final InstitutionalEntryGuardApplicationService entryGuardApplicationService;
    private final InstitutionalOperationalClosureApplicationService operationalClosureApplicationService;
    private final InstitutionalStructuralDiagnosticApplicationService structuralDiagnosticApplicationService;
    private final NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport;

    public NationalCommunicationInstitutionalEntryLifecycleSurfaceService(
            InstitutionalEntryContextApplicationService entryContextApplicationService,
            InstitutionalOperationalLifecycleApplicationService lifecycleApplicationService,
            InstitutionalEntryGuardApplicationService entryGuardApplicationService,
            InstitutionalOperationalClosureApplicationService operationalClosureApplicationService,
            InstitutionalStructuralDiagnosticApplicationService structuralDiagnosticApplicationService,
            NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport) {
        this.entryContextApplicationService = entryContextApplicationService;
        this.lifecycleApplicationService = lifecycleApplicationService;
        this.entryGuardApplicationService = entryGuardApplicationService;
        this.operationalClosureApplicationService = operationalClosureApplicationService;
        this.structuralDiagnosticApplicationService = structuralDiagnosticApplicationService;
        this.surfaceAssemblerSupport = surfaceAssemblerSupport;
    }

    public List<NationalCommunicationInstitutionalEntryContextResponse> contextosEntrada() {
        return entryContextApplicationService.resolverContextosAtuais().stream().map(surfaceAssemblerSupport::toContext).toList();
    }

    public List<NationalCommunicationInstitutionalOperationalLifecycleResponse> cadastrosOperacionais() {
        return lifecycleApplicationService.listar().stream().map(surfaceAssemblerSupport::toLifecycle).toList();
    }

    public Optional<NationalCommunicationInstitutionalOperationalLifecycleResponse> detalharAfiliacaoLifecycle(String affiliationId) {
        return lifecycleApplicationService.detalharAfiliacao(affiliationId).map(surfaceAssemblerSupport::toLifecycle);
    }

    public Optional<NationalCommunicationInstitutionalOperationalLifecycleResponse> detalharSolicitacaoLifecycle(String requestId) {
        return lifecycleApplicationService.detalharSolicitacao(requestId).map(surfaceAssemblerSupport::toLifecycle);
    }

    public NationalCommunicationInstitutionalEntryGuardResponse guardiaoEntrada() {
        return surfaceAssemblerSupport.toGuard(entryGuardApplicationService.avaliarEntradaAtual());
    }

    public NationalCommunicationInstitutionalFourLevelAccessResponse quatroNiveis(String affiliationId) {
        return surfaceAssemblerSupport.toResponse(operationalClosureApplicationService.resolverQuatroNiveisAtual(affiliationId));
    }

    public List<NationalCommunicationInstitutionalOperationalCaseResponse> casosOperacionais(String affiliationId) {
        return operationalClosureApplicationService.listarCasosOperacionais(affiliationId).stream().map(surfaceAssemblerSupport::toResponse).toList();
    }

    public NationalCommunicationInstitutionalStructuralDiagnosticResponse diagnosticoEstrutural(String affiliationId) {
        return surfaceAssemblerSupport.toResponse(structuralDiagnosticApplicationService.diagnosticar(affiliationId));
    }
}
