package com.tcc.pjb.backend.ai.juridica.api;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaLegalAiSpineService;
import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiSpineProfileResponse;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import jakarta.validation.Valid;
import java.util.Locale;
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
@RequestMapping(path = "/api/ai/legal/spine/profile", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@PreAuthorize("isAuthenticated()")
public class JuridicaLegalAiSpineController {

    private final JuridicaLegalAiSpineService juridicaLegalAiSpineService;

    public JuridicaLegalAiSpineController(JuridicaLegalAiSpineService juridicaLegalAiSpineService) {
        this.juridicaLegalAiSpineService = juridicaLegalAiSpineService;
    }

    @GetMapping
    public ResponseEntity<LegalAiSpineProfileResponse> current(@RequestParam(name = "capability", required = false) String capability,
                                                               @RequestParam(name = "version", required = false) String version) {
        ApiVersion effectiveVersion = ApiVersion.tryParse(version).orElse(ApiVersion.latest());
        String effectiveCapability = capability == null ? "LEGAL_GENERAL_ASSIST_" + effectiveVersion.name() : capability.toUpperCase(Locale.ROOT);
        return ResponseEntity.ok(juridicaLegalAiSpineService.resolveForSurface(effectiveCapability, effectiveVersion));
    }

    @PostMapping(path = "/resolve", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LegalAiSpineProfileResponse> resolve(@Valid @RequestBody IARequest request) {
        ApiVersion version = ApiVersion.inferFromToken(request.getAcao()).orElse(ApiVersion.latest());
        return ResponseEntity.ok(juridicaLegalAiSpineService.resolveForIa(request, version, request.getAcao()));
    }
}
