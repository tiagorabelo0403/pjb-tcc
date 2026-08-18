package com.tcc.pjb.backend.controller.magistratura;

import java.time.Instant;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualDescriptor;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualSecurityPolicyService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.stepup.DecisionStepUpTokenPayload;
import com.tcc.pjb.backend.core.security.stepup.DecisionStepUpTokenService;
import com.tcc.pjb.backend.model.dto.julgamento.safety.DecisionStepUpIssueRequest;
import com.tcc.pjb.backend.model.dto.julgamento.safety.DecisionStepUpIssueResponse;
import com.tcc.pjb.backend.model.entity.Usuario;

@RestController
@RequestMapping(path = "/api/v1/magistratura/face", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@PreAuthorize("isAuthenticated()")
public class FaceStepUpController {

    private final CurrentUserService currentUserService;
    private final DecisionStepUpTokenService decisionStepUpTokenService;
    private final AuditLedgerService auditLedgerService;
    private final AtoProcessualSecurityPolicyService atoProcessualSecurityPolicyService;

    public FaceStepUpController(CurrentUserService currentUserService,
                                DecisionStepUpTokenService decisionStepUpTokenService,
                                AuditLedgerService auditLedgerService,
                                AtoProcessualSecurityPolicyService atoProcessualSecurityPolicyService) {
        this.currentUserService = currentUserService;
        this.decisionStepUpTokenService = decisionStepUpTokenService;
        this.auditLedgerService = auditLedgerService;
        this.atoProcessualSecurityPolicyService = atoProcessualSecurityPolicyService;
    }

    @PostMapping(value = "/issue-decisao", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DecisionStepUpIssueResponse issueDecision(@Valid @RequestBody DecisionStepUpIssueRequest request) {
        Usuario u = currentUserService.getRequired();
        AtoProcessualDescriptor descriptor = atoProcessualSecurityPolicyService.descriptorForActType(request.actType());
        String canonicalActType = atoProcessualSecurityPolicyService.canonicalActType(request.actType());
        long now = Instant.now().getEpochSecond();
        long ttl = 120L;
        DecisionStepUpTokenPayload payload = new DecisionStepUpTokenPayload(
                UUID.randomUUID().toString(),
                u.getId(),
                now,
                now + ttl,
                "FACE_DECISAO",
                canonicalActType,
                request.processoId(),
                request.focusSessionId(),
                request.windowBinding(),
                request.tabBinding(),
                request.routeBinding(),
                request.requestHash()
        );
        String token = decisionStepUpTokenService.sign(payload);
        auditLedgerService.appendSafely(
                "DECISION_STEPUP_ISSUED",
                "STEPUP",
                payload.jti(),
                "user=" + u.getId() + " processo=" + request.processoId() + " act=" + canonicalActType + " securityAction=" + descriptor.securityProfile().securityAction() + " exp=" + payload.exp()
        );
        return new DecisionStepUpIssueResponse(
                token,
                payload.exp(),
                ttl,
                payload.focusSessionId(),
                payload.processoId(),
                payload.actType(),
                descriptor.codigo(),
                descriptor.securityProfile().securityAction()
        );
    }

    @PostMapping("/issue")
    public ResponseEntity<Void> issue() {
        Usuario u = currentUserService.getRequired();
        auditLedgerService.appendSafely(
                "FACE_STEPUP_ISSUE_REJECTED",
                "STEPUP",
                UUID.randomUUID().toString(),
                "user=" + u.getId() + " mecanismo aposentado - substituido por passkey platform+TPM+UV"
        );
        return ResponseEntity.status(HttpStatus.GONE).build();
    }
}
