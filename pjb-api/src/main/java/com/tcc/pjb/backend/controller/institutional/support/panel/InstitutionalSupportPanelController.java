package com.tcc.pjb.backend.controller.institutional.support.panel;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.institutional.support.panel.InstitutionalSupportPanelSnapshotResponse;
import com.tcc.pjb.backend.model.dto.institutional.support.operations.InstitutionalSupportCompetenceSnapshotResponse;
import com.tcc.pjb.backend.model.dto.institutional.support.operations.InstitutionalSupportCoverageSnapshotResponse;
import com.tcc.pjb.backend.model.dto.institutional.support.operations.InstitutionalSupportPrepautaSnapshotResponse;
import com.tcc.pjb.backend.model.dto.security.OperationalStepUpChallengeResponse;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialPasswordSetRequest;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialSnapshotResponse;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialUnlockRequest;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialUnlockResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.institutional.support.lane.InstitutionalSupportLaneResolver;
import com.tcc.pjb.backend.service.institutional.support.panel.InstitutionalSupportPanelService;
import com.tcc.pjb.backend.service.security.operational.OperationalFunctionCredentialService;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(OperationalApiRoutes.INSTITUTIONAL_SUPPORT_BASE)
@PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR','ROLE_SERVIDOR_FORUM','ROLE_ADMINISTRADOR','ROLE_MEMBRO_MINISTERIO_PUBLICO','ROLE_PROMOTOR_ELEITORAL','ROLE_PROMOTOR_TRABALHISTA','ROLE_PROCURADOR_GERAL_REPUBLICA','ROLE_DEFENSOR_PUBLICO','ROLE_DEFENSOR_PUBLICO_FEDERAL','ROLE_PROCURADOR','ROLE_PROCURADORIA_MUNICIPAL','ROLE_PROCURADORIA_ESTADUAL','ROLE_PROCURADORIA_FEDERAL')")
public class InstitutionalSupportPanelController {

    private final InstitutionalSupportPanelService panelService;
    private final InstitutionalSupportLaneResolver laneResolver;
    private final OperationalFunctionCredentialService credentialService;
    private final CapabilityRateLimiter rateLimiter;

    public InstitutionalSupportPanelController(InstitutionalSupportPanelService panelService,
                                               InstitutionalSupportLaneResolver laneResolver,
                                               OperationalFunctionCredentialService credentialService,
                                               CapabilityRateLimiter rateLimiter) {
        this.panelService = Objects.requireNonNull(panelService);
        this.laneResolver = Objects.requireNonNull(laneResolver);
        this.credentialService = Objects.requireNonNull(credentialService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @GetMapping(OperationalApiRoutes.PATH_INSTITUTIONAL_SUPPORT_BRANCH_SNAPSHOT)
    public ResponseEntity<InstitutionalSupportPanelSnapshotResponse> snapshot(@PathVariable String branchCode,
                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "institutional_support_snapshot", ApiVersion.V1);
        return ResponseEntity.ok(panelService.snapshot(branchCode));
    }

    @GetMapping(OperationalApiRoutes.PATH_INSTITUTIONAL_SUPPORT_BRANCH_AGENDA)
    public ResponseEntity<InstitutionalSupportPanelSnapshotResponse> agenda(@PathVariable String branchCode,
                                                                            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "institutional_support_agenda", ApiVersion.V1);
        return ResponseEntity.ok(panelService.agenda(branchCode));
    }


    @GetMapping(OperationalApiRoutes.PATH_INSTITUTIONAL_SUPPORT_BRANCH_COMPETENCE_MATRIX)
    public ResponseEntity<InstitutionalSupportCompetenceSnapshotResponse> competenceMatrix(@PathVariable String branchCode,
                                                                                            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "institutional_support_competence_matrix", ApiVersion.V1);
        return ResponseEntity.ok(panelService.competenceMatrix(branchCode));
    }

    @GetMapping(OperationalApiRoutes.PATH_INSTITUTIONAL_SUPPORT_BRANCH_COVERAGE)
    public ResponseEntity<InstitutionalSupportCoverageSnapshotResponse> coverage(@PathVariable String branchCode,
                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "institutional_support_coverage", ApiVersion.V1);
        return ResponseEntity.ok(panelService.coverage(branchCode));
    }

    @GetMapping(OperationalApiRoutes.PATH_INSTITUTIONAL_SUPPORT_BRANCH_PROCESS_PREPAUTA)
    public ResponseEntity<InstitutionalSupportPrepautaSnapshotResponse> prePauta(@PathVariable String branchCode,
                                                                                 @PathVariable Long processoId,
                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "institutional_support_pre_pauta", ApiVersion.V1);
        return ResponseEntity.ok(panelService.prePauta(branchCode, processoId));
    }

    @GetMapping(OperationalApiRoutes.PATH_INSTITUTIONAL_SUPPORT_BRANCH_CREDENTIAL_SECURITY)
    public ResponseEntity<OperationalCredentialSnapshotResponse> credentialSnapshot(@PathVariable String branchCode,
                                                                                    Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "institutional_support_credential_snapshot", ApiVersion.V1);
        requireBranch(branchCode);
        return ResponseEntity.ok(rebind(branchCode, credentialService.snapshotForCurrentUser("INSTITUTIONAL_SUPPORT")));
    }

    @PostMapping(OperationalApiRoutes.PATH_INSTITUTIONAL_SUPPORT_BRANCH_CREDENTIAL_CHALLENGE)
    public ResponseEntity<OperationalStepUpChallengeResponse> credentialChallenge(@PathVariable String branchCode,
                                                                                  @PathVariable String functionCode,
                                                                                  Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "institutional_support_credential_challenge", ApiVersion.V1);
        requireBranch(branchCode);
        return ResponseEntity.ok(credentialService.issueCurrentUserPasswordChallenge(functionCode));
    }

    @PostMapping(OperationalApiRoutes.PATH_INSTITUTIONAL_SUPPORT_BRANCH_CREDENTIAL_PASSWORD)
    public ResponseEntity<OperationalCredentialSnapshotResponse> setCredentialPassword(@PathVariable String branchCode,
                                                                                       @PathVariable String functionCode,
                                                                                       @RequestBody OperationalCredentialPasswordSetRequest request,
                                                                                       Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "institutional_support_credential_password", ApiVersion.V1);
        requireBranch(branchCode);
        return ResponseEntity.ok(rebind(branchCode, credentialService.setCurrentUserPassword(functionCode, request)));
    }

    @PostMapping(OperationalApiRoutes.PATH_INSTITUTIONAL_SUPPORT_BRANCH_CREDENTIAL_UNLOCK)
    public ResponseEntity<OperationalCredentialUnlockResponse> unlockCredential(@PathVariable String branchCode,
                                                                                @PathVariable String functionCode,
                                                                                @RequestBody OperationalCredentialUnlockRequest request,
                                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "institutional_support_credential_unlock", ApiVersion.V1);
        requireBranch(branchCode);
        return ResponseEntity.ok(credentialService.unlockCurrentUserFunction(functionCode, request));
    }


    private void requireBranch(String branchCode) {
        var lane = laneResolver.requireCurrentUser();
        if (branchCode != null && !branchCode.isBlank() && !lane.branchCode().equalsIgnoreCase(branchCode)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "branchCode fora do escopo institucional do usuário");
        }
    }

    private OperationalCredentialSnapshotResponse rebind(String branchCode, OperationalCredentialSnapshotResponse snapshot) {
        java.util.List<OperationalCredentialSnapshotResponse.Entry> entries = snapshot.entries().stream()
                .map(entry -> new OperationalCredentialSnapshotResponse.Entry(
                        entry.functionCode(),
                        entry.label(),
                        entry.status(),
                        entry.provisionedByInstitution(),
                        entry.active(),
                        entry.resetRequired(),
                        entry.locked(),
                        entry.justicaAxis(),
                        entry.tribunalCodigo(),
                        entry.forumCode(),
                        entry.unitCode(),
                        entry.varaLabel(),
                        entry.uf(),
                        entry.comarca(),
                        entry.activatedAt(),
                        entry.lastVerifiedAt(),
                        entry.lastResetAt(),
                        entry.policy(),
                        routeMap(branchCode, entry.functionCode())
                ))
                .toList();
        java.util.Map<String, Object> routes = entries.isEmpty()
                ? compactMap(java.util.Map.of(
                        "credentialBasePath", OperationalApiRoutes.institutionalSupportCredentialSecurity(branchCode),
                        "branchBound", Boolean.TRUE
                ))
                : routeMap(branchCode, entries.get(0).functionCode());
        return new OperationalCredentialSnapshotResponse(snapshot.laneCode(), entries, snapshot.directorGovernance(), routes);
    }

    private java.util.Map<String, Object> routeMap(String branchCode, String functionCode) {
        java.util.LinkedHashMap<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("challengePath", OperationalApiRoutes.institutionalSupportCredentialChallenge(branchCode, functionCode));
        out.put("setPasswordPath", OperationalApiRoutes.institutionalSupportCredentialPassword(branchCode, functionCode));
        out.put("unlockPath", OperationalApiRoutes.institutionalSupportCredentialUnlock(branchCode, functionCode));
        out.put("credentialBasePath", OperationalApiRoutes.institutionalSupportCredentialSecurity(branchCode));
        out.put("branchBound", Boolean.TRUE);
        return compactMap(out);
    }

    private java.util.Map<String, Object> compactMap(java.util.Map<String, ?> raw) {
        java.util.LinkedHashMap<String, Object> out = new java.util.LinkedHashMap<>();
        if (raw != null) {
            raw.forEach((key, value) -> {
                if (key != null && value != null) {
                    out.put(key, value);
                }
            });
        }
        return java.util.Map.copyOf(out);
    }
}
