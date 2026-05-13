package com.tcc.pjb.backend.controller.processual.comunicacao.institutional.access;

import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalHttpRoutes.InstitutionalApiRoutes;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalBindingApprovalResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalContextActivationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalIdentityGuardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalRepresentativeVerificationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalStepUpPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalTextClosureAuditResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(InstitutionalApiRoutes.CANONICAL_BASE)
public class NationalCommunicationInstitutionalAccessClosureController {

    private final NationalCommunicationInstitutionalSurfaceFacadeService facadeService;

    public NationalCommunicationInstitutionalAccessClosureController(NationalCommunicationInstitutionalSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping(InstitutionalApiRoutes.PATH_REPRESENTANTE_VERIFICACAO)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalRepresentativeVerificationResponse> verificarRepresentante(@PathVariable String requestId) {
        return ResponseEntity.of(facadeService.verificarRepresentante(requestId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_VINCULOS_APROVACAO)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalBindingApprovalResponse> aprovacaoVinculo(@RequestParam(required = false) String affiliationId,
                                                                                                       @RequestParam(required = false) String nominationId) {
        return ResponseEntity.ok(facadeService.aprovacaoVinculo(affiliationId, nominationId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_IDENTIDADE_GUARDA)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalIdentityGuardResponse> guardaIdentidade() {
        return ResponseEntity.ok(facadeService.guardaIdentidade());
    }

    @GetMapping(InstitutionalApiRoutes.PATH_POLITICA_STEP_UP)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalStepUpPolicyResponse> politicaStepUp(@RequestParam(required = false) String affiliationId,
                                                                                                  @RequestParam(required = false) String nominationId,
                                                                                                  @RequestParam(required = false) String sensitiveAct) {
        return ResponseEntity.ok(facadeService.politicaStepUp(affiliationId, nominationId, sensitiveAct));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_CONTEXTOS_ATIVACAO)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalContextActivationResponse> ativacaoContexto(@RequestParam(required = false) String affiliationId,
                                                                                                         @RequestParam(required = false) String nominationId,
                                                                                                         @RequestParam(required = false) String unidadeCodigo,
                                                                                                         @RequestParam(required = false) String caixaCodigo,
                                                                                                         @RequestParam(required = false) String sensitiveAct) {
        return ResponseEntity.ok(facadeService.ativacaoContexto(affiliationId, nominationId, unidadeCodigo, caixaCodigo, sensitiveAct));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_FECHAMENTO_TEXTO)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalTextClosureAuditResponse> fechamentoTexto() {
        return ResponseEntity.ok(facadeService.fechamentoTexto());
    }
}
