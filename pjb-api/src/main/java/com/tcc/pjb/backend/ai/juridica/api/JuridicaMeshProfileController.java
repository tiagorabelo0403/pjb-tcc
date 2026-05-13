package com.tcc.pjb.backend.ai.juridica.api;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.juridica.mesh.JuridicaUnifiedMeshProfileService;
import com.tcc.pjb.backend.model.dto.ai.legal.mesh.LegalAiMeshProfileResponse;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/ai/legal/mesh/profile", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@PreAuthorize("isAuthenticated()")
public class JuridicaMeshProfileController {

    private final JuridicaUnifiedMeshProfileService juridicaUnifiedMeshProfileService;

    public JuridicaMeshProfileController(JuridicaUnifiedMeshProfileService juridicaUnifiedMeshProfileService) {
        this.juridicaUnifiedMeshProfileService = juridicaUnifiedMeshProfileService;
    }

    @GetMapping
    public ResponseEntity<LegalAiMeshProfileResponse> current(@RequestParam(name = "capability", required = false) String capability,
                                                              @RequestParam(name = "version", required = false) String version) {
        ApiVersion effectiveVersion = ApiVersion.tryParse(version).orElse(ApiVersion.latest());
        return ResponseEntity.ok(juridicaUnifiedMeshProfileService.resolveForSurface(capability == null ? "LEGAL_GENERAL_ASSIST_" + effectiveVersion.name() : capability.toUpperCase(Locale.ROOT), effectiveVersion));
    }

    @PostMapping(path = "/resolve", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LegalAiMeshProfileResponse> resolve(@Valid @RequestBody IARequest request) {
        ApiVersion version = ApiVersion.inferFromToken(request.getAcao()).orElse(ApiVersion.latest());
        return ResponseEntity.ok(juridicaUnifiedMeshProfileService.resolveForIa(request, version, request.getAcao(), Map.of(), Map.of(), Map.of("effectiveMode", "READ_ONLY")));
    }
}
