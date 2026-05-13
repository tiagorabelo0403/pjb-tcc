package com.tcc.pjb.backend.controller;

import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.magistratura.delegation.DelegationCredential;
import com.tcc.pjb.backend.core.security.magistratura.delegation.DelegationScope;
import com.tcc.pjb.backend.core.security.magistratura.delegation.DelegationTokenPayload;
import com.tcc.pjb.backend.core.security.magistratura.delegation.DelegationTokenService;
import com.tcc.pjb.backend.core.security.magistratura.delegation.DelegationValidationService;
import com.tcc.pjb.backend.core.security.magistratura.delegation.JudgeDelegationApiRoutes;
import com.tcc.pjb.backend.core.security.magistratura.delegation.JudgeDelegationService;
import com.tcc.pjb.backend.model.dto.judge.delegation.JudgeDelegationFlowApproveResponse;
import com.tcc.pjb.backend.model.dto.judge.delegation.JudgeDelegationFlowDecisionRequest;
import com.tcc.pjb.backend.model.dto.judge.delegation.JudgeDelegationFlowRequest;
import com.tcc.pjb.backend.model.dto.judge.delegation.JudgeDelegationFlowView;
import com.tcc.pjb.backend.model.dto.judge.delegation.JudgeDelegationCurrentUserResponse;
import com.tcc.pjb.backend.model.dto.judge.delegation.JudgeDelegationIssueRequest;
import com.tcc.pjb.backend.model.dto.judge.delegation.JudgeDelegationIssueResponse;
import com.tcc.pjb.backend.model.dto.judge.delegation.JudgeDelegationVerifyRequest;
import com.tcc.pjb.backend.model.dto.judge.delegation.JudgeDelegationVerifyResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.magistratura.delegationflow.JudgeDelegationFlowService;

@RestController
@RequestMapping(JudgeDelegationApiRoutes.CANONICAL_BASE)
public class JudgeDelegationController {

    private static final String MAGISTRACY_ROLES = "hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','JUIZ_ELEITORAL','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO')";
    private static final String DELEGATION_PARTICIPANT_ROLES = "hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','JUIZ_ELEITORAL','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO')";

    private final JudgeDelegationService judgeDelegationService;
    private final DelegationTokenService tokenService;
    private final DelegationValidationService validationService;
    private final CurrentUserService currentUserService;
    private final JudgeDelegationFlowService flowService;

    public JudgeDelegationController(JudgeDelegationService judgeDelegationService,
                                     DelegationTokenService tokenService,
                                     DelegationValidationService validationService,
                                     CurrentUserService currentUserService,
                                     JudgeDelegationFlowService flowService) {
        this.judgeDelegationService = judgeDelegationService;
        this.tokenService = tokenService;
        this.validationService = validationService;
        this.currentUserService = currentUserService;
        this.flowService = flowService;
    }

    @PostMapping(JudgeDelegationApiRoutes.PATH_ISSUE)
    @PreAuthorize(MAGISTRACY_ROLES)
    public ResponseEntity<JudgeDelegationIssueResponse> issue(@RequestBody JudgeDelegationIssueRequest req) {
        Long assessorId = req == null ? null : req.assessorId();
        int minutes = req == null || req.duracaoMinutos() == null ? 60 : req.duracaoMinutos();
        DelegationScope scope = DelegationScope.parse(req == null ? null : req.scope());
        String token = judgeDelegationService.emitirDelegacaoParaAssessor(
                assessorId,
                minutes,
                scope,
                req == null ? null : req.deviceBindingHash());
        DelegationTokenPayload payload = tokenService.verifyAndDecode(token);
        return ResponseEntity.ok(new JudgeDelegationIssueResponse(
                token,
                payload.jti(),
                payload.magistrateId(),
                payload.delegateId(),
                payload.scope().toUpperCase(),
                Instant.ofEpochSecond(payload.exp())));
    }

    @PostMapping(JudgeDelegationApiRoutes.PATH_REQUESTS)
    @PreAuthorize(DELEGATION_PARTICIPANT_ROLES)
    public ResponseEntity<JudgeDelegationFlowView> requestDelegation(@RequestBody JudgeDelegationFlowRequest request) {
        return ResponseEntity.ok(flowService.solicitar(request));
    }

    @PostMapping(JudgeDelegationApiRoutes.PATH_REQUEST_APPROVE)
    @PreAuthorize(MAGISTRACY_ROLES)
    public ResponseEntity<JudgeDelegationFlowApproveResponse> approveRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(flowService.aprovar(requestId));
    }

    @PostMapping(JudgeDelegationApiRoutes.PATH_REQUEST_REJECT)
    @PreAuthorize(MAGISTRACY_ROLES)
    public ResponseEntity<JudgeDelegationFlowView> rejectRequest(@PathVariable Long requestId,
                                                                 @RequestBody(required = false) JudgeDelegationFlowDecisionRequest request) {
        return ResponseEntity.ok(flowService.rejeitar(requestId, request == null ? null : request.motivo()));
    }

    @PostMapping(JudgeDelegationApiRoutes.PATH_REQUEST_REVOKE)
    @PreAuthorize(MAGISTRACY_ROLES)
    public ResponseEntity<JudgeDelegationFlowView> revokeRequest(@PathVariable Long requestId,
                                                                 @RequestBody(required = false) JudgeDelegationFlowDecisionRequest request) {
        return ResponseEntity.ok(flowService.revogar(requestId, request == null ? null : request.motivo()));
    }

    @GetMapping(JudgeDelegationApiRoutes.PATH_REQUESTS_PENDING)
    @PreAuthorize(MAGISTRACY_ROLES)
    public ResponseEntity<List<JudgeDelegationFlowView>> pendingForMagistrate() {
        return ResponseEntity.ok(flowService.pendentesParaMagistrado());
    }

    @GetMapping(JudgeDelegationApiRoutes.PATH_REQUESTS_MINE)
    @PreAuthorize(DELEGATION_PARTICIPANT_ROLES)
    public ResponseEntity<List<JudgeDelegationFlowView>> myRequests() {
        return ResponseEntity.ok(flowService.minhasSolicitacoes());
    }

    @GetMapping(JudgeDelegationApiRoutes.PATH_ACTIVE)
    @PreAuthorize(MAGISTRACY_ROLES)
    public ResponseEntity<List<JudgeDelegationFlowView>> active() {
        return ResponseEntity.ok(flowService.ativasDaMagistratura());
    }

    @PostMapping(JudgeDelegationApiRoutes.PATH_VERIFY)
    @PreAuthorize(DELEGATION_PARTICIPANT_ROLES)
    public ResponseEntity<JudgeDelegationVerifyResponse> verify(@RequestBody JudgeDelegationVerifyRequest req) {
        try {
            if (req == null || req.token() == null || req.token().isBlank()) {
                return ResponseEntity.ok(new JudgeDelegationVerifyResponse(false, null, null, null, null, null));
            }
            Usuario current = currentUserService.getRequired();
            DelegationTokenPayload payload = tokenService.verifyAndDecode(req.token());
            DelegationCredential credential = validationService.validateForDelegate(payload, current, req.deviceBindingHash());
            return ResponseEntity.ok(new JudgeDelegationVerifyResponse(
                    true,
                    credential.jti(),
                    credential.magistrateId(),
                    credential.delegateId(),
                    credential.scope().name(),
                    credential.validUntil()));
        } catch (Exception e) {
            return ResponseEntity.ok(new JudgeDelegationVerifyResponse(false, null, null, null, null, null));
        }
    }

    @GetMapping(JudgeDelegationApiRoutes.PATH_ME)
    @PreAuthorize(DELEGATION_PARTICIPANT_ROLES)
    public ResponseEntity<JudgeDelegationCurrentUserResponse> me() {
        Usuario usuario = currentUserService.getRequired();
        return ResponseEntity.ok(new JudgeDelegationCurrentUserResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getTipoUsuario() == null ? null : usuario.getTipoUsuario().name(),
                usuario.getUf(),
                usuario.getComarca(),
                usuario.isMagistrado()
        ));
    }
}
