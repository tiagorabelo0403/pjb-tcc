package com.tcc.pjb.backend.controller.security.operational;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialDirectorProvisionRequest;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialSnapshotResponse;
import com.tcc.pjb.backend.service.security.operational.OperationalFunctionCredentialService;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(OperationalApiRoutes.INSTITUTIONAL_CREDENTIAL_GOVERNANCE_BASE)
@PreAuthorize("hasAnyRole('ADMINISTRADOR','SERVIDOR','SERVIDOR_FORUM')")
public class InstitutionalCredentialGovernanceController {

    private final OperationalFunctionCredentialService credentialService;

    public InstitutionalCredentialGovernanceController(OperationalFunctionCredentialService credentialService) {
        this.credentialService = Objects.requireNonNull(credentialService);
    }

    @PostMapping(OperationalApiRoutes.PATH_INSTITUTIONAL_CREDENTIAL_GOVERNANCE)
    public ResponseEntity<OperationalCredentialSnapshotResponse> provision(@RequestBody OperationalCredentialDirectorProvisionRequest request) {
        return ResponseEntity.ok(credentialService.directorProvision(request));
    }

    @GetMapping(OperationalApiRoutes.PATH_INSTITUTIONAL_CREDENTIAL_GOVERNANCE_TARGET)
    public ResponseEntity<OperationalCredentialSnapshotResponse> targetSnapshot(@PathVariable Long targetUserId,
                                                                                @RequestParam("lane") String lane) {
        return ResponseEntity.ok(credentialService.snapshotForTarget(targetUserId, lane));
    }
}
