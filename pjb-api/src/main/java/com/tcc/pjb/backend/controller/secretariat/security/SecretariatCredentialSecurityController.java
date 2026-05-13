package com.tcc.pjb.backend.controller.secretariat.security;


import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.security.OperationalStepUpChallengeResponse;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialPasswordSetRequest;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialSnapshotResponse;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialUnlockRequest;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialUnlockResponse;
import com.tcc.pjb.backend.service.security.operational.OperationalFunctionCredentialService;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(OperationalApiRoutes.SECRETARIAT_CREDENTIAL_SECURITY_BASE)
@PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_ADMINISTRADOR')")
public class SecretariatCredentialSecurityController {

    private final OperationalFunctionCredentialService credentialService;

    public SecretariatCredentialSecurityController(OperationalFunctionCredentialService credentialService) {
        this.credentialService = Objects.requireNonNull(credentialService);
    }

    @GetMapping
    public ResponseEntity<OperationalCredentialSnapshotResponse> snapshot() {
        return ResponseEntity.ok(credentialService.snapshotForCurrentUser("SECRETARIAT"));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_CREDENTIAL_CHALLENGE)
    public ResponseEntity<OperationalStepUpChallengeResponse> issueChallenge(@PathVariable String functionCode) {
        return ResponseEntity.ok(credentialService.issueCurrentUserPasswordChallenge(functionCode));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_CREDENTIAL_PASSWORD)
    public ResponseEntity<OperationalCredentialSnapshotResponse> setPassword(@PathVariable String functionCode,
                                                                             @RequestBody OperationalCredentialPasswordSetRequest request) {
        return ResponseEntity.ok(credentialService.setCurrentUserPassword(functionCode, request));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_CREDENTIAL_UNLOCK)
    public ResponseEntity<OperationalCredentialUnlockResponse> unlock(@PathVariable String functionCode,
                                                                      @RequestBody OperationalCredentialUnlockRequest request) {
        return ResponseEntity.ok(credentialService.unlockCurrentUserFunction(functionCode, request));
    }
}
